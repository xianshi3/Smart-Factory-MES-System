.PHONY: all docker backend ai gateway frontend simulator clean stop status

all: docker backend ai gateway frontend simulator

# ----- Infrastructure -----
docker:
	docker compose up -d

docker-down:
	docker compose down

# ----- Java Backend -----
build:
	mvn clean package -DskipTests

auth:
	mvn spring-boot:run -pl mes-auth

gateway:
	mvn spring-boot:run -pl mes-gateway

workorder:
	mvn spring-boot:run -pl mes-workorder

process:
	mvn spring-boot:run -pl mes-process

quality:
	mvn spring-boot:run -pl mes-quality

dashboard:
	mvn spring-boot:run -pl mes-dashboard

backend: build
	$(MAKE) auth & $(MAKE) gateway & $(MAKE) workorder & $(MAKE) process & $(MAKE) quality & $(MAKE) dashboard

# ----- AI Service -----
ai:
	cd mes-ai-service && python -m src.main

# ----- .NET Gateway -----
dotnet-gateway:
	cd mes-device-gateway/src/MesDeviceGateway && dotnet run

# ----- Frontend -----
frontend:
	cd mes-frontend && npm install && npm run dev

# ----- Simulator -----
simulator:
	cd mes-device-simulator-wpf && dotnet run

# ----- Utils -----
clean:
	rm -rf mes-ai-service/.venv mes-ai-service/src/**/__pycache__
	rm -rf mes-device-gateway/src/MesDeviceGateway/bin mes-device-gateway/src/MesDeviceGateway/obj
	rm -rf .opencode

stop:
	@powershell -NoProfile -Command "$$ports = 8081,8082,8083,8084,8085,9090,3000,8087,5000; foreach ($$p in $$ports) { Get-NetTCPConnection -LocalPort $$p -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $$_.OwningProcess -Force -ErrorAction SilentlyContinue; Write-Host \"[$$p] stopped\" } }"

status:
	@echo "=== Service Status ==="
	@for port in 8081 8082 8083 8084 8085 9090 3000 5173 8087 5000; do \
		netstat -ano | findstr ":$$port " | findstr "LISTENING" >nul && echo "  [$$port] RUNNING" || echo "  [$$port] STOPPED"; \
	done
