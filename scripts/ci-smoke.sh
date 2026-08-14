#!/usr/bin/env bash
# CI 冒烟测试：MySQL/Redis 由 GitHub Actions services 提供。
# 真实启动 auth/workorder/process/quality/gateway 五个服务，
# 逐个等待 /actuator/health 就绪，再经网关验证"登录 -> 带 token 访问 -> 无 token 被 401 拦截"完整链路。
# 注：mes-dashboard 依赖 InfluxDB/Kafka，未纳入本次冒烟。
set -euo pipefail

SERVICES=("mes-auth:8081" "mes-workorder:8082" "mes-process:8083" "mes-quality:8084" "mes-gateway:9090")
PIDS=()

cleanup() {
  for p in "${PIDS[@]:-}"; do kill "$p" 2>/dev/null || true; done
}
trap cleanup EXIT

wait_for_health() {
  local name=$1 port=$2 i code
  for i in $(seq 1 60); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" 2>/dev/null || true)
    if [ -n "$code" ] && [ "$code" != "000" ]; then
      echo "OK: $name 已就绪 (port $port, http $code)"
      return 0
    fi
    sleep 2
  done
  echo "FAIL: $name 启动超时，最近日志:"
  tail -n 40 "/tmp/$name.log" 2>/dev/null || true
  return 1
}

echo "==> 初始化数据库 schema"
mysql -h 127.0.0.1 -P 3306 -uroot -proot --default-character-set=utf8mb4 mes_db < sql/init.sql

for svc in "${SERVICES[@]}"; do
  name=${svc%%:*}
  port=${svc##*:}
  jar=$(ls "$name"/target/*.jar 2>/dev/null | grep -v original | head -1)
  if [ -z "$jar" ]; then
    echo "FAIL: 找不到 $name 的可执行 jar"
    exit 1
  fi
  echo "==> 启动 $name ($jar)"
  nohup java -jar "$jar" > "/tmp/$name.log" 2>&1 &
  PIDS+=("$!")
  wait_for_health "$name" "$port"
done

echo "==> 校验认证/权限链路（经网关全链路）"
LOGIN=$(curl -s -X POST http://localhost:9090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])' 2>/dev/null || true)
if [ -z "$TOKEN" ]; then
  echo "FAIL: 登录失败: $LOGIN"
  exit 1
fi
echo "OK: 登录成功（admin/admin123）"

CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9090/api/workorder/planning/board \
  -H "Authorization: Bearer $TOKEN")
if [ "$CODE" != "200" ]; then
  echo "FAIL: 带 token 访问工作台应返回 200，实际 $CODE"
  exit 1
fi
echo "OK: 带 token 访问工作台: 200"

CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9090/api/workorder/planning/board)
if [ "$CODE" != "401" ]; then
  echo "FAIL: 无 token 访问应返回 401，实际 $CODE"
  exit 1
fi
echo "OK: 无 token 访问被拦截: 401"

echo "SMOKE PASS"