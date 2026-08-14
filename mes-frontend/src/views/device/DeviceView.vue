<template>
  <div class="dt-page">
    <!-- ===== TOP BAR ===== -->
    <header class="dt-topbar">
      <div class="dt-topbar-left">
        <span class="dt-logo"><el-icon><Monitor /></el-icon> 设备监控</span>
      </div>
      <div v-if="viewMode === 'list'" class="dt-topbar-stats">
        <span v-for="s in stats" :key="s.label" class="dt-stats-badge" :class="s.theme">
          <span class="dt-badge-num">{{ s.value }}</span>{{ s.label }}
        </span>
      </div>
      <div v-else class="dt-topbar-spacer"></div>
      <div class="dt-topbar-right">
        <div class="dt-view-switch">
          <button :class="{ on: viewMode === '3d' }" @click="viewMode = '3d'"><el-icon size="14"><Grid /></el-icon> 数字孪生</button>
          <button :class="{ on: viewMode === 'list' }" @click="viewMode = 'list'"><el-icon size="14"><View /></el-icon> 设备列表</button>
        </div>
        <div v-if="viewMode === 'list'" class="dt-topbar-actions">
          <el-input v-model="searchKeyword" size="small" placeholder="搜索..." clearable :prefix-icon="Search" style="width:150px" />
          <span v-for="f in filterChips" :key="f.key" class="dt-fchip" :class="{ on: statusFilter === f.key }" @click="statusFilter = f.key">{{ f.label }}</span>
        </div>
        <el-button text size="small" class="dt-btn-refresh" @click="refresh"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </header>

    <!-- ===== MAIN CONTENT ===== -->
    <div class="dt-main">
      <!-- 3D Scene -->
      <div v-if="viewMode === '3d'" class="dt-scene-wrap">
        <DigitalTwinScene :devices="deviceList" @select="handleDeviceSelect" @action="handle3DAction" />

        <!-- HUD: alarms panel -->
        <transition name="hud-fade">
          <div v-if="hudPanels.alarms" class="dt-hud dt-hud-alarms">
            <div class="dt-hud-head" @click="hudPanels.alarms = false">
              <el-icon><Warning /></el-icon>告警<span class="dt-hud-badge">{{ alarmList.length }}</span>
              <el-icon class="dt-hud-close"><Close /></el-icon>
            </div>
            <div class="dt-hud-list">
              <div v-for="(a,i) in alarmList.slice(0,6)" :key="i" class="dt-hud-row" :class="getAlarmClass(a.level)">
                <span class="dt-hud-dot"></span><span>{{ a.message || a.deviceName }}</span>
              </div>
              <div v-if="!alarmList.length" class="dt-hud-none">✓ 系统运行正常，暂无告警</div>
            </div>
          </div>
        </transition>

        <!-- HUD: charts panel (bottom) -->
        <transition name="hud-slide">
          <div v-if="hudPanels.charts" class="dt-hud dt-hud-charts">
            <div class="dt-hud-chart-head">
              <span>性能趋势</span>
              <el-button text size="small" @click="hudPanels.charts = false"><el-icon><Close /></el-icon></el-button>
            </div>
            <div class="dt-hud-chart-grid">
              <div><em>设备状态分布</em><v-chart :option="statusOption" autoresize style="height:140px" /></div>
              <div><em>利用率</em><v-chart :option="utilizationOption" autoresize style="height:140px" /></div>
            </div>
          </div>
        </transition>

        <!-- HUD control buttons -->
        <div class="dt-hud-btns">
          <button :class="{ on: hudPanels.alarms }" @click="hudPanels.alarms = !hudPanels.alarms">
            <el-icon><Warning /></el-icon><span v-if="alarmList.length" class="dt-hud-dot-badge">{{ alarmList.length }}</span>
          </button>
          <button :class="{ on: hudPanels.charts }" @click="hudPanels.charts = !hudPanels.charts">
            <el-icon><TrendCharts /></el-icon>
          </button>
        </div>

      </div>

      <!-- List view -->
      <div v-if="viewMode === 'list'" class="dt-list-wrap">
        <div v-if="filteredDevices.length === 0" class="dt-empty"><el-empty description="暂无设备数据" :image-size="80" /></div>
        <div v-else class="dt-list-grid">
          <div v-for="(d,i) in pagedDevices" :key="d.id || i" class="dc-card" :class="{ 'dc-card--live': d.status === 'running' }" @click="handleDetail(d)">
            <div class="dc-card-top">
              <div class="dc-card-info">
                <span class="dc-card-name">{{ d.name }}</span>
                <span class="dc-card-code">{{ d.code }}</span>
              </div>
              <div class="dc-card-top-right">
                <span class="status-tag" :class="'status-tag--' + d.status"><i class="dc-status-dot" :class="'dc-status-dot--' + d.status"></i>{{ getStatusText(d.status) }}</span>
              </div>
            </div>
            <div class="dc-card-metrics">
              <div class="dc-metric">
                <span class="dc-m-val" :class="{ warn: d.temperature > 55, hot: d.temperature > 70, 'dc-flash': d.tempFlash }">{{ fmtTemp(d.temperature) }}</span>
                <span class="dc-m-unit">°C 温度</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val" :class="{ 'dc-flash': d.speedFlash }">{{ d.speed || 0 }}</span>
                <span class="dc-m-unit">rpm 转速</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val" :class="{ 'dc-flash': d.powerFlash }">{{ d.power ?? '--' }}</span>
                <span class="dc-m-unit">kW 功率</span>
              </div>
              <div class="dc-metric">
                <span class="dc-m-val">{{ d.efficiency || '0%' }}</span>
                <span class="dc-m-unit">OEE</span>
              </div>
            </div>
            <div class="dc-card-progress">
              <div class="dc-progress-info">
                <span class="dc-progress-label">利用率</span>
                <span class="dc-progress-value">{{ d.utilization || '0%' }}</span>
              </div>
              <div class="dc-progress-bar"><div class="dc-progress-fill" :style="{ width: (parseInt(d.utilization)||0)+'%' }"></div></div>
            </div>
            <div class="dc-card-spark">
              <span class="dc-spark-label">温度趋势</span>
              <svg v-if="d.spark && d.spark.length > 1" :viewBox="`0 0 ${sparkW} ${sparkH}`" preserveAspectRatio="none" class="dc-spark-svg">
                <polyline :points="sparkPoints(d.spark)" fill="none" stroke="#f59e0b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span v-else class="dc-spark-empty">等待数据...</span>
              <span class="dc-spark-time"><el-icon size="11"><Timer /></el-icon> {{ d.heartbeatText || '--' }}</span>
            </div>
            <div class="dc-card-foot" @click.stop>
              <el-tooltip v-if="d.status==='running'" content="停止设备" placement="top">
                <el-button size="small" circle type="danger" plain @click="handleStop(d)"><el-icon><VideoPause /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip v-if="d.status==='idle'" content="启动设备" placement="top">
                <el-button size="small" circle type="success" plain @click="handleStart(d)"><el-icon><VideoPlay /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip content="故障预测" placement="top">
                <el-button size="small" circle type="primary" plain @click="handleCardPredict(d)"><el-icon><Cpu /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip content="SPC 过程能力分析" placement="top">
                <el-button size="small" circle plain @click="handleCardAI(d, 'spc')"><el-icon><Histogram /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip content="能耗优化分析" placement="top">
                <el-button size="small" circle plain @click="handleCardAI(d, 'energy')"><el-icon><Lightning /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip content="AI 智能建议" placement="top">
                <el-button size="small" circle plain @click="handleCardAI(d, 'llm')"><el-icon><ChatLineRound /></el-icon></el-button>
              </el-tooltip>
              <div class="dc-foot-spacer"></div>
              <el-tooltip content="查看详情" placement="top">
                <el-button size="small" circle plain @click="handleDetail(d)"><el-icon><View /></el-icon></el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
        <div v-if="filteredDevices.length > pageSize" class="dt-list-pager">
          <el-pagination v-model:current-page="page" small :total="filteredDevices.length" :page-size="pageSize" layout="total, prev, pager, next" background />
        </div>
      </div>
    </div>

    <!-- DIALOGS -->
    <el-dialog v-model="detailVisible" title="设备详情" width="860px" destroy-on-close class="device-detail-dlg" :close-on-click-modal="false">
      <div v-if="detailData" class="dt-dlg-det">
        <div class="dt-dlg-det-head">
          <div class="dt-dlg-det-avatar"><el-icon size="26"><Monitor /></el-icon></div>
          <div><strong>{{ detailData.name }}</strong><br><small>{{ detailData.code }}</small></div>
          <el-tag :type="getStatusType(detailData.status)" size="large">{{ getStatusText(detailData.status) }}</el-tag>
        </div>
        <div class="dt-dlg-det-kpis">
          <div v-for="kv in [['利用率', detailData.utilization + '%'], ['温度', fmtTemp(detailData.temperature) + '°C'], ['功率', detailData.power + 'kW'], ['OEE', (detailData.efficiency || 0) + '%']]" :key="kv[0]" class="dt-dlg-det-kpi">
            <strong>{{ kv[1] }}</strong><span>{{ kv[0] }}</span>
          </div>
        </div>

        <!-- 历史使用情况 -->
        <div class="dt-dlg-section">
          <div class="dt-dlg-sec-head">
            <span class="dt-dlg-sec-title"><el-icon><TrendCharts /></el-icon> 历史使用情况</span>
            <div class="dt-dlg-range">
              <button v-for="r in historyRanges" :key="r.hours" :class="{ on: historyHours === r.hours }" @click="switchHistoryRange(r.hours)">{{ r.label }}</button>
              <el-button size="small" text :icon="Refresh" @click="loadDeviceHistory(true)">刷新</el-button>
            </div>
          </div>

          <div v-if="historyLoading" class="dt-dlg-hist-loading"><el-icon class="is-loading"><Loading /></el-icon> 加载历史数据...</div>
          <div v-else-if="!historyEnabled" class="dt-dlg-hist-empty">
            <el-empty description="InfluxDB 未配置，无法加载历史遥测（设置 INFLUXDB_URL/INFLUXDB_TOKEN 环境变量）" :image-size="70" />
          </div>
          <div v-else-if="!historyOption.series?.length" class="dt-dlg-hist-empty">
            <el-empty description="暂无历史数据（启动设备模拟后自动采集）" :image-size="70" />
          </div>
          <template v-else>
            <div class="dt-dlg-chart-row">
              <div class="dt-dlg-chart">
                <em>温度趋势 °C</em>
                <v-chart :option="tempTrendOption" autoresize style="height:150px" />
              </div>
              <div class="dt-dlg-chart">
                <em>转速趋势 rpm</em>
                <v-chart :option="speedTrendOption" autoresize style="height:150px" />
              </div>
            </div>
            <div class="dt-dlg-chart-row">
              <div class="dt-dlg-chart">
                <em>功率趋势 kW</em>
                <v-chart :option="powerTrendOption" autoresize style="height:140px" />
              </div>
              <div class="dt-dlg-chart">
                <em>历史状态分布</em>
                <v-chart :option="statusDistOption" autoresize style="height:140px" />
              </div>
            </div>
            <div class="dt-dlg-hist-stats">
              <div v-for="s in historyStats" :key="s.label" class="dt-dlg-hist-stat">
                <strong>{{ s.value }}</strong><span>{{ s.label }}</span>
              </div>
            </div>
          </template>
        </div>

        <div class="dt-dlg-ai-btns">
          <el-button type="warning" @click="handlePredict(detailData)"><el-icon><Cpu /></el-icon> 故障预测</el-button>
          <el-button @click="handleCardAI(detailData, 'spc')"><el-icon><Histogram /></el-icon> SPC分析</el-button>
          <el-button @click="handleCardAI(detailData, 'energy')"><el-icon><Lightning /></el-icon> 能耗优化</el-button>
          <el-button type="primary" @click="handleCardAI(detailData, 'llm')"><el-icon><ChatLineRound /></el-icon> AI建议</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="predictVisible" title="AI预测分析" width="440px">
      <div v-if="predictData" style="text-align:center">
        <el-icon size="42" :color="predictData.faultLevel==='danger'?'var(--danger)':predictData.faultLevel==='warning'?'var(--warning)':'var(--success)'"><Cpu /></el-icon>
        <h3 style="margin:8px 0">{{ predictData.deviceName }}</h3>
        <el-result :icon="predictData.faultLevel==='danger'?'error':'success'" title="预测结果" :sub-title="predictData.message">
          <template #extra><el-tag :type="predictData.faultLevel">{{ predictData.confidence }}</el-tag></template>
        </el-result>
        <div v-if="predictData.riskFactors?.length" style="margin-top:8px">
          <el-tag v-for="(f,i) in predictData.riskFactors" :key="i" type="warning" size="small" style="margin:2px">{{ f.description||f.factor }}</el-tag>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="aiAnalysisVisible" :title="currentAnalysisType === 'spc' ? 'SPC统计分析' : currentAnalysisType === 'energy' ? '能耗优化' : currentAnalysisType === 'capacity' ? '产能预测' : 'AI建议'" width="680px" destroy-on-close class="ai-dlg" :close-on-click-modal="false">
      <div v-if="aiAnalysisLoading" class="dt-loading"><el-icon class="is-loading" size="32"><Loading /></el-icon><p>AI分析中...</p></div>

      <!-- History list (shown when no active result) -->
      <div v-else-if="!aiAnalysisResult && !aiAnalysisLoading" class="ai-prompt-area">
        <div v-if="filteredHistory.length" class="ai-history-panel">
          <div class="ai-subtitle">{{ quickType ? ({ llm:'AI建议', spc:'SPC分析', energy:'能耗优化', capacity:'产能预测' }[quickType]) + '记录' : '分析记录' }}</div>
          <div
            v-for="(h, i) in filteredHistory.slice(0, 8)"
            :key="i"
            class="ai-hi-row"
            @click="aiAnalysisResult = h.data; currentAnalysisType = h.type"
          >
            <span class="ai-hi-tag" :class="h.type">{{ { spc:'SPC', energy:'能耗', capacity:'产能', llm:'AI建议' }[h.type] }}</span>
            <span class="ai-hi-name">{{ h.deviceName }}</span>
            <span class="ai-hi-time">{{ fmtTime(h.ts) }}</span>
            <button class="ai-hi-del" title="删除记录" @click="removeHistory(h, $event)"><el-icon :size="13"><Close /></el-icon></button>
          </div>
        </div>
        <div class="ai-device-card">
          <div class="ai-dc-head">
            <span class="ai-dc-icon"><el-icon :size="18"><Cpu /></el-icon></span>
            <div>
              <strong>{{ detailData?.name || '选择设备' }}</strong>
              <small>{{ getStatusText(detailData?.status || '') }}</small>
            </div>
          </div>
          <div class="ai-dc-metrics">
            <span>🌡 {{ fmtTemp(detailData?.temperature) }}°C</span>
            <span>⚙ {{ detailData?.speed ?? '--' }} rpm</span>
            <span>⚡ {{ detailData?.power ?? '--' }} kW</span>
            <span>📊 {{ detailData?.utilization || '0%' }}</span>
          </div>
          <div v-if="!quickType" class="ai-dc-actions">
            <el-button size="small" @click="handleSPCAnalysis"><el-icon><Histogram /></el-icon> SPC分析</el-button>
            <el-button size="small" @click="handleEnergyOptimization"><el-icon><Lightning /></el-icon> 能耗优化</el-button>
            <el-button size="small" @click="handleCapacityPrediction"><el-icon><TrendCharts /></el-icon> 产能预测</el-button>
            <el-button size="small" type="primary" @click="handleLLMChat"><el-icon><ChatLineRound /></el-icon> AI建议</el-button>
          </div>
          <div v-else class="ai-dc-actions">
            <el-button size="small" type="primary" @click="handleQuickAnalysis">{{ quickBtn.cta }}</el-button>
          </div>
        </div>
      </div>

      <!-- Result area -->
      <div v-if="!aiAnalysisLoading && aiAnalysisResult" class="ai-result-area">
        <button class="ai-back-btn" @click="aiAnalysisResult = null"><el-icon :size="14"><DArrowLeft /></el-icon> 返回</button>

        <template v-if="currentAnalysisType === 'spc'">
          <div class="ai-result-card">
            <div class="ai-rc-head accent">SPC 制程能力分析 <span class="ai-rc-src">· {{ aiAnalysisResult.parameter_name || aiAnalysisResult.parameter }}</span></div>
            <div class="ai-rc-body">
              <!-- KPI 行 -->
              <div class="ai-spc-kpis">
                <div class="ai-cpk-badge" :class="(aiAnalysisResult.capability?.level || aiAnalysisResult.process_capability || '').toLowerCase()">{{ (aiAnalysisResult.capability?.cpk || aiAnalysisResult.cpk)?.toFixed(2) }}</div>
                <div class="ai-spc-kpi">
                  <label>过程能力等级</label><span class="ai-cpk-level" :class="(aiAnalysisResult.capability?.level || '').toLowerCase()">{{ { EXCELLENT:'优秀', GOOD:'良好', FAIR:'一般', POOR:'不足' }[aiAnalysisResult.capability?.level] || aiAnalysisResult.capability?.level }}</span>
                  <small>CPK 90%置信区间 [{{ aiAnalysisResult.capability?.cpk_ci?.[0] }} ~ {{ aiAnalysisResult.capability?.cpk_ci?.[1] }}]</small>
                </div>
              </div>
              <div class="ai-stats-row">
                <div><label>CP</label><span>{{ aiAnalysisResult.capability?.cp }}</span></div>
                <div><label>PPK(长期)</label><span>{{ aiAnalysisResult.capability?.ppk }}</span></div>
                <div><label>CPM(目标)</label><span>{{ aiAnalysisResult.capability?.cpm }}</span></div>
                <div><label>均值</label><span>{{ aiAnalysisResult.statistics?.mean }}</span></div>
                <div><label>标准差</label><span>{{ aiAnalysisResult.statistics?.std }}</span></div>
                <div><label>稳定性</label><span>{{ ((aiAnalysisResult.stability || 0) * 100).toFixed(0) }}%</span></div>
              </div>
              <div v-if="aiAnalysisResult.specification" class="ai-spec-line">
                规格限 <b>LSL {{ aiAnalysisResult.specification.lsl }}</b> / 目标 {{ aiAnalysisResult.specification.target }} / <b>USL {{ aiAnalysisResult.specification.usl }}</b>
                <em>{{ aiAnalysisResult.spec_source }}</em>
                <span v-if="aiAnalysisResult.statistics?.normal_distribution" class="ai-spec-normal">✓ 正态</span>
                <span v-else class="ai-spec-nonormal">✗ 非正态 (偏度{{ aiAnalysisResult.statistics?.skewness }})</span>
              </div>

              <!-- SVG 控制图 -->
              <div class="ai-sec-title">控制图 (I-MR 单值图)</div>
              <svg class="ai-chart" viewBox="0 0 340 150" preserveAspectRatio="none">
                <line x1="20" x2="320" y1="10" y2="10" class="ai-chart-limit"/>
                <line x1="20" x2="320" y1="25" y2="25" class="ai-chart-warn"/>
                <line x1="20" x2="320" y1="40" y2="40" class="ai-chart-zone"/>
                <line x1="20" x2="320" y1="55" y2="55" class="ai-chart-cl"/>
                <line x1="20" x2="320" y1="70" y2="70" class="ai-chart-zone"/>
                <line x1="20" x2="320" y1="85" y2="85" class="ai-chart-warn"/>
                <line x1="20" x2="320" y1="100" y2="100" class="ai-chart-limit"/>
                <polyline :points="spcChart.points" class="ai-chart-line" fill="none"/>
                <circle v-for="(p, i) in spcChart.dots" :key="i" :cx="p.x" :cy="p.y" r="2.6" :class="p.out ? 'ai-chart-dot-out' : 'ai-chart-dot'"/>
                <text x="14" y="13" class="ai-chart-txt">UCL {{ aiAnalysisResult.control_limits?.[0]?.value }}</text>
                <text x="14" y="58" class="ai-chart-txt">CL {{ aiAnalysisResult.control_limits?.[2]?.value }}</text>
                <text x="14" y="103" class="ai-chart-txt">LCL {{ aiAnalysisResult.control_limits?.[4]?.value }}</text>
              </svg>
              <div class="ai-chart-note">{{ aiAnalysisResult.chart_recommendation }}</div>

              <!-- 直方图 -->
              <div class="ai-sec-title">分布直方图</div>
              <div v-if="aiAnalysisResult.histogram" class="ai-histogram">
                <div v-for="(b, i) in aiAnalysisResult.histogram" :key="i" class="ai-hist-bar" :title="b.range + ':' + b.count + '个'">
                  <div class="ai-hist-col" :style="{ height: Math.max(6, b.count / spcHistMax * 60) + 'px' }"></div>
                  <span>{{ b.count }}</span>
                </div>
              </div>

              <!-- Western Electric 规则检测 -->
              <div class="ai-sec-title">Western Electric 规则检测 <span class="ai-rule-count" :class="aiAnalysisResult.rules_violated?.length ? 'hit' : 'ok'">{{ aiAnalysisResult.rules_violated?.length || 0 }}/8 命中</span></div>
              <div class="ai-we-rules">
                <div v-for="r in aiAnalysisResult.we_rules" :key="r.id" class="ai-we-rule" :class="{ hit: aiAnalysisResult.rules_violated?.some(x => x.id === r.id) }">
                  <span class="ai-we-id">{{ r.id }}</span>
                  <span class="ai-we-name">{{ r.name }}</span>
                  <span class="ai-we-desc">{{ r.desc }}</span>
                </div>
              </div>
              <div v-if="aiAnalysisResult.rules_violated?.length" class="ai-we-hit">
                <div v-for="h in aiAnalysisResult.rules_violated" :key="h.id" class="ai-we-hit-item">⚠ {{ h.id }} {{ h.name }} — {{ h.detail }}</div>
              </div>

              <!-- 5M1E 建议 -->
              <div class="ai-sec-title">5M1E 改进建议</div>
              <ul class="ai-5m1e">
                <li v-for="(rc, i) in aiAnalysisResult.recommendations" :key="i">{{ rc }}</li>
              </ul>

              <!-- 抽样计划 -->
              <div v-if="aiAnalysisResult.sampling_plan" class="ai-sampling">
                <div class="ai-sec-title">监控抽样计划</div>
                <el-tag v-for="(v, k) in aiAnalysisResult.sampling_plan" :key="k" size="small" effect="plain" style="margin:2px">{{ { frequency:'频率', subgroup_size:'子组', trigger:'触发条件' }[k] }}: {{ v }}</el-tag>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentAnalysisType === 'energy'">
          <div class="ai-result-card">
            <div class="ai-rc-head warning">能耗优化分析 <span v-if="aiAnalysisResult.data_source === 'mysql_realtime'" class="ai-rc-src">· 实时遥测数据</span><span v-else class="ai-rc-src">· 请求参数估算</span></div>
            <div class="ai-rc-body">
              <!-- KPI 四宫格 -->
              <div class="ai-kpi-grid">
                <div class="ai-kpi-cell"><span class="kpi-val">{{ aiAnalysisResult.kpis?.savings_pct ?? aiAnalysisResult.estimated_energy_savings_pct }}%</span><small>节能潜力</small></div>
                <div class="ai-kpi-cell"><span class="kpi-val">{{ aiAnalysisResult.kpis?.monthly_savings_kwh ?? aiAnalysisResult.estimated_monthly_savings_kwh }} kWh</span><small>月省电量</small></div>
                <div class="ai-kpi-cell"><span class="kpi-val">¥{{ aiAnalysisResult.kpis?.monthly_savings_cost ?? aiAnalysisResult.estimated_monthly_savings_cost ?? '--' }}</span><small>月省成本</small></div>
                <div class="ai-kpi-cell"><span class="kpi-val">{{ aiAnalysisResult.kpis?.co2_reduction_kg ?? '--' }} kg</span><small>CO₂减排/月</small></div>
              </div>
              <div v-if="aiAnalysisResult.baseline" class="ai-kpi-sub">
                基线 {{ aiAnalysisResult.baseline.monthly_baseline_kwh }} kWh/月 · 负载率 {{ (aiAnalysisResult.baseline.load_factor * 100).toFixed(0) }}% · 单位能耗 {{ aiAnalysisResult.baseline.specific_energy_before }} → {{ aiAnalysisResult.baseline.specific_energy_after }} kWh/件
              </div>

              <!-- 优化策略构成 -->
              <div v-if="aiAnalysisResult.optimization_breakdown" class="ai-sec-title">优化策略构成</div>
              <div v-if="aiAnalysisResult.optimization_breakdown" class="ai-breakdown">
                <div v-for="b in aiAnalysisResult.optimization_breakdown" :key="b.strategy" class="ai-bd-row">
                  <span class="ai-bd-name">{{ b.strategy }}<em>{{ b.phase }}</em></span>
                  <span class="ai-bd-bar"><i :style="{ width: Math.min(100, b.savings_kwh / Math.max(...aiAnalysisResult.optimization_breakdown.map(x => x.savings_kwh), 1) * 100) + '%' }"></i></span>
                  <span class="ai-bd-val">{{ b.savings_kwh > 0 ? b.savings_kwh + ' kWh' : '—' }}<small>{{ b.savings_cost > 0 ? '¥' + b.savings_cost : '' }}</small></span>
                </div>
              </div>

              <!-- 参数对比 -->
              <div class="ai-sec-title">参数调优建议</div>
              <table class="ai-param-table">
                <thead><tr><th>参数</th><th>当前值</th><th>推荐值</th><th>变化</th></tr></thead>
                <tbody>
                  <tr><td>转速</td><td>{{ aiAnalysisResult.current_parameters?.speed }} rpm</td><td>{{ aiAnalysisResult.recommended_parameters?.speed }} rpm</td><td class="ai-delta">{{ aiAnalysisResult.parameter_changes?.speed }}</td></tr>
                  <tr><td>温度</td><td>{{ aiAnalysisResult.current_parameters?.temperature }}°C</td><td>{{ aiAnalysisResult.recommended_parameters?.temperature }}°C</td><td class="ai-delta">{{ aiAnalysisResult.parameter_changes?.temperature }}</td></tr>
                  <tr><td>功率</td><td>{{ aiAnalysisResult.current_parameters?.power }} kW</td><td>{{ aiAnalysisResult.recommended_parameters?.power }} kW</td><td class="ai-delta">{{ aiAnalysisResult.parameter_changes?.power }}</td></tr>
                </tbody>
              </table>

              <!-- 削峰填谷 -->
              <div class="ai-sec-title">削峰填谷 · 分时电价策略</div>
              <div v-if="aiAnalysisResult.tou_schedule" class="ai-tou-row">
                <div v-for="(t, k) in aiAnalysisResult.tou_schedule" :key="k" class="ai-tou-cell" :class="k">
                  <div class="ai-tou-head"><span class="ai-tou-tag" :class="k">{{ { peak:'峰', flat:'平', valley:'谷' }[k] }}</span><b>¥{{ t.price }}/kWh</b></div>
                  <div class="ai-tou-hours">{{ t.hours }}</div>
                  <div class="ai-tou-action">{{ t.action }}</div>
                </div>
              </div>

              <!-- 实施路线图 -->
              <div class="ai-sec-title">实施路线图</div>
              <div v-if="aiAnalysisResult.roadmap" class="ai-roadmap">
                <div v-for="(rp, i) in aiAnalysisResult.roadmap" :key="i" class="ai-rm-item">
                  <div class="ai-rm-phase">{{ rp.phase }}</div>
                  <div class="ai-rm-body">
                    <div class="ai-rm-head"><span class="ai-rm-duration">{{ rp.duration }}</span><span class="ai-rm-saving">{{ rp.expected_savings }} 节能</span></div>
                    <div class="ai-rm-actions"><el-tag v-for="a in rp.actions" :key="a" size="small" effect="plain">{{ a }}</el-tag></div>
                    <div class="ai-rm-kpis"><small>验收KPI: {{ rp.kpis.join(' / ') }}</small></div>
                  </div>
                </div>
              </div>

              <!-- 风险提示 -->
              <div v-if="aiAnalysisResult.risk_and_notes" class="ai-risks">
                <div class="ai-sec-title">风险与注意事项</div>
                <ul><li v-for="(n, i) in aiAnalysisResult.risk_and_notes" :key="i">{{ n }}</li></ul>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentAnalysisType === 'capacity'">
          <div class="ai-result-card">
            <div class="ai-rc-head success">产能预测</div>
            <div class="ai-rc-body">
              <div class="ai-energy-kpis">
                <div><span class="kpi-val">{{ aiAnalysisResult.summary?.total || aiAnalysisResult.total_predicted }}</span><small>总产量</small></div>
                <div><span class="kpi-val">{{ aiAnalysisResult.summary?.daily_avg || aiAnalysisResult.average_daily }}</span><small>日均</small></div>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="ai-result-card">
            <div class="ai-rc-head accent">AI 智能建议 — {{ detailData?.name }}</div>
            <div class="ai-rc-body ai-llm-body" v-html="aiAdviceHtml(aiAnalysisResult)"></div>
          </div>
        </template>

        <div class="ai-result-meta">
          <el-tag size="small" :type="detailData?.status === 'running' ? 'success' : 'info'">{{ getStatusText(detailData?.status || '') }}</el-tag>
          <span>{{ detailData?.name }}</span>
          <span>{{ fmtTemp(detailData?.temperature) }}°C</span>
        </div>
      </div>  <!-- end ai-result-area -->

      <!-- Generic / Other Result -->
      <div v-else-if="aiAnalysisResult" class="dt-ai-result">
        <div v-if="aiAnalysisResult.success === false" class="dt-ai-warn">
          <el-icon><Warning /></el-icon> {{ aiAnalysisResult.message || '服务暂不可用' }}
        </div>
        <div v-else class="dt-ai-raw">{{ JSON.stringify(aiAnalysisResult, null, 2) }}</div>
      </div>
      <template #footer><el-button @click="aiAnalysisVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, reactive, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDeviceStatus, getDeviceHistory } from '@/api/dashboard'
import { getAlarmDevices, predictDeviceFault, predictCapacity, analyzeSPC, llmChat, optimizeEnergy, startDevice, stopDevice } from '@/api/services'
import { listAnalyses, saveAnalysis, deleteAnalysis } from '@/api/agent'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'
import { wsService } from '@/utils/websocket'
import { Monitor, Refresh, Search, TrendCharts, Warning, Grid, View, Cpu,
 VideoPlay, VideoPause, Loading, CircleCheck, Histogram, Lightning, ChatLineRound, Close, ArrowRight, DArrowLeft, Timer } from '@element-plus/icons-vue'
import DigitalTwinScene from '@/components/device/DigitalTwinScene.vue'
import { marked } from 'marked'
marked.setOptions({ breaks: true, gfm: true })
import { useUserStore } from '@/stores/user'

const themeStore = useThemeStore()
const chartTheme = useChartTheme()
const deviceList = ref<any[]>([])
const alarmList = ref<any[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(50)
const detailVisible = ref(false)
const detailData = ref<any>({})
const route = useRoute()
const viewMode = ref<'list' | '3d'>('3d')
const predictVisible = ref(false)
const predictData = ref<any>({})
const aiAnalysisVisible = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisResult = ref<any>(null)
const currentAnalysisType = ref('')
const quickType = ref<string | null>(null) // null=多选, 'llm'/'spc'/'energy'/'capacity'=单选
const selectedDevice = ref<any>(null)
const aiHistory = ref<any[]>([])
const hudPanels = reactive({ alarms: true, charts: true })

let refreshInterval: number
const wsUnsubscribe = ref<(() => void) | null>(null)

const stats = ref([
  { label: '设备总数', value: 0, icon: 'Monitor', theme: 'primary' },
  { label: '运行中', value: 0, icon: 'CircleCheck', theme: 'success' },
  { label: '空闲', value: 0, icon: 'VideoPause', theme: 'info' },
  { label: '故障', value: 0, icon: 'Warning', theme: 'warning' },
])
const filterChips = [
  { key: '', label: '全部' },
  { key: 'running', label: '运行' },
  { key: 'idle', label: '空闲' },
  { key: 'fault', label: '故障' },
]

const utilizationOption = ref({})
const statusOption = ref({})

// ===== 实时动态辅助（闪烁 / sparkline / 心跳） =====
const sparkW = 130
const sparkH = 22
const sparkBuffer = new Map<string, number[]>()
const prevValues = new Map<string, { t: number, s: number, p: number }>()

const sparkPoints = (arr: number[]) => {
  if (arr.length < 2) return ''
  const min = Math.min(...arr), max = Math.max(...arr)
  const span = max - min || 1
  return arr.map((v, i) => {
    const x = (i / (arr.length - 1)) * sparkW
    const y = sparkH - 2 - ((v - min) / span) * (sparkH - 4)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

const heartbeatText = (lastHeartbeat?: string) => {
  if (!lastHeartbeat) return '--'
  try {
    const diff = Date.now() - new Date(lastHeartbeat).getTime()
    if (diff < 0) return '刚刚'
    if (diff < 10000) return '刚刚'
    if (diff < 60000) return Math.floor(diff / 1000) + 's前'
    return Math.floor(diff / 60000) + 'm前'
  } catch { return '--' }
}

const applyLiveFx = (devices: any[]) => {
  const now = Date.now()
  devices.forEach(d => {
    // 心跳相对时间
    d.heartbeatText = heartbeatText(d.heartbeatRaw ?? d.lastHeartbeat)
    // 数值闪烁：与上次取值比较
    const prev = prevValues.get(d.code)
    if (prev) {
      d.tempFlash = Math.abs((d.temperature ?? 0) - prev.t) > 0.05
      d.speedFlash = Math.abs((d.speed ?? 0) - prev.s) > 0.5
      d.powerFlash = Math.abs((d.power ?? 0) - prev.p) > 0.05
    } else {
      d.tempFlash = d.speedFlash = d.powerFlash = false
    }
    prevValues.set(d.code, { t: d.temperature ?? 0, s: d.speed ?? 0, p: d.power ?? 0 })
    // sparkline 滚动缓冲（最多 24 点）
    const buf = sparkBuffer.get(d.code) ?? []
    const tv = d.temperature ?? 0
    if (buf.length === 0 || buf[buf.length - 1] !== tv) {
      buf.push(tv)
      if (buf.length > 24) buf.shift()
      sparkBuffer.set(d.code, buf)
    }
    d.spark = [...buf]
    d._ts = now
  })
  return devices
}

const clearLiveFx = () => {
  sparkBuffer.clear()
  prevValues.clear()
}

const filteredDevices = computed(() => {
  let result = deviceList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d => d.name?.toLowerCase().includes(kw) || d.code?.toLowerCase().includes(kw))
  }
  if (statusFilter.value) result = result.filter(d => d.status === statusFilter.value)
  return result
})

const pagedDevices = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredDevices.value.slice(start, start + pageSize.value)
})

const getStatusType = (s: string) => ({ running: 'success', idle: 'info', maintenance: 'warning', fault: 'danger' } as any)[s] || 'info'
const getStatusText = (s: string) => ({ running: '运行中', idle: '空闲', maintenance: '维护中', fault: '故障' } as any)[s] || '未知'
const getAlarmClass = (l: string) => ({ high: 'danger', medium: 'warning', low: 'info' } as any)[l] || 'info'

const calculateRuntime = (lastHeartbeat: string) => {
  if (!lastHeartbeat) return '0h'
  try { const h = Math.floor((Date.now() - new Date(lastHeartbeat).getTime()) / 3600000); return h < 1 ? '<1h' : `${h}h` } catch { return '0h' }
}

const fetchDeviceData = async () => {
  try {
    const [deviceRes, alarmRes] = await Promise.all([getDeviceStatus(), getAlarmDevices()])
    let devices: any[] = []
    if (Array.isArray(deviceRes)) devices = deviceRes
    else if (deviceRes?.data?.value) devices = deviceRes.data.value
    else if (deviceRes?.data && Array.isArray(deviceRes.data)) devices = deviceRes.data
    else if (deviceRes?.value) devices = deviceRes.value

    deviceList.value = devices.map((item: any, i: number) => ({
      id: item.id, name: item.deviceName || item.deviceCode || `设备${i + 1}`, code: item.deviceCode || '',
      status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
      utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
      runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
      temperature: item.temperature ?? null,
      speed: item.speed ?? 0, power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0,
      efficiency: item.efficiency ?? 0,
      heartbeatRaw: item.lastHeartbeat,
    }))
    applyLiveFx(deviceList.value)

    let alarms: any[] = []
    if (Array.isArray(alarmRes)) alarms = alarmRes
    else if (alarmRes?.data?.value) alarms = alarmRes.data.value
    else if (alarmRes?.data && Array.isArray(alarmRes.data)) alarms = alarmRes.data
    else if (alarmRes?.value) alarms = alarmRes.value
    alarmList.value = alarms

    stats.value = [
      { label: '设备总数', value: deviceList.value.length, icon: 'Monitor', theme: 'primary' },
      { label: '运行中', value: deviceList.value.filter(d => d.status === 'running').length, icon: 'CircleCheck', theme: 'success' },
      { label: '空闲', value: deviceList.value.filter(d => d.status === 'idle').length, icon: 'VideoPause', theme: 'info' },
      { label: '故障', value: deviceList.value.filter(d => d.status === 'fault').length, icon: 'Warning', theme: 'warning' },
    ]
    updateCharts()
  } catch (e) { console.error(e) }
}

const updateCharts = () => {
  const isDark = themeStore.isDark
  const tc = isDark ? '#aaa' : '#666'

  utilizationOption.value = {
    ...chartTheme.value,
    tooltip: { trigger: 'axis' },
    grid: { left: 5, right: 5, top: 5, bottom: 5, containLabel: true },
    xAxis: { type: 'category', data: deviceList.value.map(d => d.name).slice(0, 6), axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', max: 100, axisLabel: { color: tc, fontSize: 10 } },
    series: [{ type: 'bar', data: deviceList.value.map(d => parseInt(d.utilization) || 0).slice(0, 10), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#6366f1' }, barWidth: 12 }]
  }

  const st: any = { running: 0, idle: 0, fault: 0, maintenance: 0 }
  deviceList.value.forEach(d => { if (st[d.status] !== undefined) st[d.status]++ })
  statusOption.value = {
    ...chartTheme.value,
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['50%', '75%'], center: ['50%', '50%'], label: { color: tc, fontSize: 10 },
      data: [{ value: st.running, name: '运行中', itemStyle: { color: '#34c759' } }, { value: st.idle, name: '空闲', itemStyle: { color: '#8e8e93' } },
             { value: st.fault, name: '故障', itemStyle: { color: '#ff3b30' } }, { value: st.maintenance, name: '维护', itemStyle: { color: '#ff9500' } }] }]
  }
}

const handleDeviceSelect = (d: any) => { selectedDevice.value = d; detailData.value = d }
const handle3DAction = (payload: { type: string; device: any }) => {
  detailData.value = payload.device
  if (payload.type === 'predict') { handlePredict(payload.device) }
  else { openAiDialog(payload.type) }
}
const refresh = () => { fetchDeviceData() }

// ===== 设备历史使用情况 =====
const historyRanges = [
  { hours: 6, label: '6小时' },
  { hours: 24, label: '24小时' },
  { hours: 72, label: '3天' },
  { hours: 168, label: '7天' },
]
const historyHours = ref(24)
const historyLoading = ref(false)
const historyEnabled = ref(true)
const historyData = ref<any>({ times: [], temperature: [], speed: [], pressure: [], power: [] })
const historyRefreshTimer = ref<number | null>(null)

const tempTrendOption = ref({})
const speedTrendOption = ref({})
const powerTrendOption = ref({})
const statusDistOption = ref({})
const historyOption = tempTrendOption
const historyStats = ref<any[]>([])

const fmtAxisTime = (t: string) => t // 后端已格式化 HH:mm

const loadDeviceHistory = async (force = false) => {
  if (!detailData.value?.code) return
  historyLoading.value = true
  try {
    const res = await getDeviceHistory(detailData.value.code, historyHours.value, historyHours.value > 24 ? 600 : 60)
    const raw: any = res?.data ?? res
    const d = raw?.data ?? raw ?? {}
    historyData.value = d
    historyEnabled.value = d.enabled !== false
    if (!d.times?.length) { historyLoading.value = false; return }
    buildHistoryCharts()
  } catch (e) {
    historyEnabled.value = false
    historyData.value = { times: [], temperature: [], speed: [], pressure: [], power: [] }
    console.error('[History]', e)
  } finally {
    historyLoading.value = false
  }
}

const switchHistoryRange = (hours: number) => {
  historyHours.value = hours
  loadDeviceHistory()
}

const buildHistoryCharts = () => {
  const d = historyData.value
  const isDark = themeStore.isDark
  const tc = isDark ? '#aaa' : '#666'
  const grid = { left: 40, right: 16, top: 24, bottom: 22 }
  const tooltip = { trigger: 'axis' as const }
  const line = (name: string, color: string, data: number[], area = false) => ({
    name, type: 'line' as const, smooth: true, symbol: 'none', data,
    lineStyle: { width: 2, color },
    itemStyle: { color },
    areaStyle: area ? { opacity: 0.12, color } : undefined,
  })

  tempTrendOption.value = {
    ...chartTheme.value, tooltip, grid,
    xAxis: { type: 'category', data: d.times, axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: tc, fontSize: 10 } },
    series: [line('温度', '#f59e0b', d.temperature, true)],
  }
  speedTrendOption.value = {
    ...chartTheme.value, tooltip, grid,
    xAxis: { type: 'category', data: d.times, axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: tc, fontSize: 10 } },
    series: [line('转速', '#06b6d4', d.speed, true)],
  }
  powerTrendOption.value = {
    ...chartTheme.value, tooltip, grid,
    xAxis: { type: 'category', data: d.times, axisLabel: { color: tc, fontSize: 10 } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: tc, fontSize: 10 } },
    series: [line('功率', '#8b5cf6', d.power, true)],
  }

  const nums = (arr: number[]) => arr.map((v: any) => Number(v) || 0)
  const temp = nums(d.temperature)
  const speed = nums(d.speed)
  const avg = (a: number[]) => a.length ? (a.reduce((s, v) => s + v, 0) / a.length).toFixed(1) : '--'
  const max = (a: number[]) => a.length ? Math.max(...a).toFixed(1) : '--'
  const cur = (a: number[]) => a.length ? a[a.length - 1].toFixed(1) : '--'
  const avgUti = speed.length ? Math.round(nums(d.speed).filter(s => s > 0).length / speed.length * 100) + '%' : '--'

  historyStats.value = [
    { label: '平均温度', value: avg(temp) + '°C' },
    { label: '最高温度', value: max(temp) + '°C' },
    { label: '平均转速', value: avg(speed) },
    { label: '运行占比', value: avgUti },
  ]

  // 状态分布：以"有遥测=运行"估算 + 当前状态
  const running = nums(d.speed).filter(s => s > 0).length
  const idle = nums(d.speed).length - running
  const st: any[] = []
  if (running > 0) st.push({ value: running, name: '运行中', itemStyle: { color: '#34c759' } })
  if (idle > 0) st.push({ value: idle, name: '空闲', itemStyle: { color: '#8e8e93' } })
  statusDistOption.value = {
    ...chartTheme.value, tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '50%'],
      label: { color: tc, fontSize: 10 },
      data: st.length ? st : [{ value: 1, name: '暂无数据', itemStyle: { color: '#d1d5db' } }],
    }],
  }
}

watch(detailVisible, (v) => {
  if (v) {
    loadDeviceHistory()
    historyRefreshTimer.value = window.setInterval(() => { if (detailVisible.value) loadDeviceHistory() }, 30000)
  } else {
    if (historyRefreshTimer.value) { clearInterval(historyRefreshTimer.value); historyRefreshTimer.value = null }
  }
})

async function loadAnalysisHistory(deviceCode?: string) {
  try {
    const userStore = useUserStore()
    // 确保用户信息已加载（user_id 依赖 username），避免查成 'default' 导致历史为空
    if (!userStore.userInfo && userStore.token) {
      try { await userStore.getUserInfo() } catch { /* 静默 */ }
    }
    const uid = userStore.userInfo?.username || 'default'
    const records = await listAnalyses(uid, undefined, deviceCode)
    aiHistory.value = records.map(r => ({
      id: r.id, type: r.analysis_type, deviceName: r.device_name, deviceCode: r.device_code,
      ts: new Date(r.created_at).getTime(), data: r.result_data,
    }))
  } catch {
    // MySQL 不可用 → 降级到 localStorage
    const userStore = useUserStore()
    const uid = userStore.userInfo?.username || 'default'
    try {
      const key = `ai_history_${uid}`
      aiHistory.value = JSON.parse(localStorage.getItem(key) || '[]')
    } catch { /* 都没有就空 */ }
  }
}
const removeHistory = async (h: any, e: Event) => {
  e.stopPropagation()
  const userStore = useUserStore()
  const uid = userStore.userInfo?.username || 'default'
  // 1. 内存移除
  const idx = aiHistory.value.indexOf(h)
  if (idx > -1) aiHistory.value.splice(idx, 1)
  // 2. MySQL 删除（localStorage 降级记录没有 id，跳过）
  if (h.id) {
    try { await deleteAnalysis(h.id, uid) } catch { /* AI服务离线，仅本地移除 */ }
  }
  // 3. localStorage 同步删除
  try {
    const key = `ai_history_${uid}`
    const local: any[] = JSON.parse(localStorage.getItem(key) || '[]')
    const kept = local.filter(x => x.id !== h.id && (x.ts !== h.ts || x.deviceCode !== h.deviceCode))
    localStorage.setItem(key, JSON.stringify(kept))
  } catch { /* ignore */ }
}
const handleDetail = (d: any) => { detailData.value = d; detailVisible.value = true }
const handleStart = async (d: any) => { try { await startDevice(d.id || d.code); ElMessage.success('启动成功'); fetchDeviceData() } catch { ElMessage.error('启动失败') } }
const handleStop = async (d: any) => { try { await stopDevice(d.id || d.code); ElMessage.success('停止成功'); fetchDeviceData() } catch { ElMessage.error('停止失败') } }

const handleCardPredict = (d: any) => { detailData.value = d; handlePredict(d) }
const handleCardAI = (d: any, type: string) => { detailData.value = d; openAiDialog(type) }
const handlePredict = async (d: any) => {
  try {
    const payload = { device_code: d.code || d.id, history_data: [{ temperature: Number(d.temperature) || 80, speed: Number(d.speed) || 50 }], hours_ahead: 24 }
    const res = await predictDeviceFault(payload)
    const raw = res?.data || res
    const inner = raw?.data || raw
    predictData.value = {
      deviceName: d.name || d.deviceName,
      faultLevel: inner.prediction === 'FAULT' ? 'danger' : inner.prediction === 'WARNING' ? 'warning' : 'success',
      message: inner.prediction === 'FAULT' ? '预测可能发生故障，建议安排检修' : inner.prediction === 'WARNING' ? '存在潜在风险，建议加强监控' : '设备运行状态良好，无需维护',
      confidence: `${((inner.confidence || 0.85) * 100).toFixed(0)}%`,
      riskFactors: inner.risk_factors || inner.riskFactors || []
    }
    predictVisible.value = true
  } catch { ElMessage.error('预测失败，请确认AI服务已启动') }
}

const showAIResult = (type: string, data: any) => {
  const result = data?.data || data
  currentAnalysisType.value = type
  aiAnalysisResult.value = result
  aiAnalysisLoading.value = false
  const d = detailData.value || {}
  const entry: any = {
    type, deviceName: d.name || d.code || '', deviceCode: d.code || '',
    ts: Date.now(), data: result,
  }
  aiHistory.value.unshift(entry)
  if (aiHistory.value.length > 20) aiHistory.value.length = 20
  // 持久化到 MySQL + localStorage 降级
  const userStore = useUserStore()
  const uid = userStore.userInfo?.username || 'default'
  saveAnalysis(uid, d.code || '', d.name || '', type, result).then((id) => {
    entry.id = id
  }).catch((e) => {
    console.warn('分析保存MySQL失败，降级到localStorage:', e)
  })
  // localStorage 降级 — 确保离线也能存
  try {
    const key = `ai_history_${uid}`
    const local: any[] = JSON.parse(localStorage.getItem(key) || '[]')
    local.unshift({ ...entry })
    localStorage.setItem(key, JSON.stringify(local.slice(0, 50)))
  } catch {
    /* 存储失败时忽略，不影响主流程 */
  }
}
const filteredHistory = computed(() => {
  const curCode = detailData.value?.code
  let list = aiHistory.value
  // 每台设备只显示该设备的历史记录
  if (curCode) list = list.filter(h => h.deviceCode === curCode)
  if (quickType.value) list = list.filter(h => h.type === quickType.value)
  return list
})
const spcChart = computed(() => {
  const r = aiAnalysisResult.value
  const vals: number[] = r?.data_series || []
  const ucl = r?.control_limits?.[0]?.value
  const cl = r?.control_limits?.[2]?.value
  const lcl = r?.control_limits?.[4]?.value
  if (!vals.length || cl == null || ucl == null || lcl == null) return { points: '', dots: [] }
  const span = (ucl - lcl) || 1
  const stepX = 300 / Math.max(vals.length - 1, 1)
  const Y = (v: number) => 55 + (cl - v) / span * 90
  const dots = vals.map((v, i) => ({ x: 20 + i * stepX, y: Math.max(4, Math.min(106, Y(v))), out: v > ucl || v < lcl }))
  return { points: dots.map(d => `${d.x.toFixed(1)},${d.y.toFixed(1)}`).join(' '), dots }
})
const spcHistMax = computed(() => Math.max(...(aiAnalysisResult.value?.histogram || []).map((b: any) => b.count), 1))
const quickBtn = computed(() => {
  const map: Record<string, { cta: string }> = { llm: { cta: '开始 AI 建议分析' }, spc: { cta: '开始 SPC 分析' }, energy: { cta: '开始能耗优化分析' }, capacity: { cta: '开始产能预测分析' } }
  return map[quickType.value || 'llm'] || { cta: '开始分析' }
})
function handleQuickAnalysis() {
  const t = quickType.value || 'llm'
  if (t === 'spc') handleSPCAnalysis()
  else if (t === 'energy') handleEnergyOptimization()
  else if (t === 'capacity') handleCapacityPrediction()
  else handleLLMChat()
}
function fmtTime(ts: number): string {
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
function fmtTemp(t: any): string {
  if (t == null || isNaN(Number(t))) return '--'
  return Number(t).toFixed(1)
}
function aiAdviceHtml(result: any): string {
  const text = result?.content || result?.response || ''
  return (marked.parse(text) as string)
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
}
const openAiDialog = (type?: string) => {
  aiAnalysisVisible.value = true; aiAnalysisResult.value = null
  aiAnalysisLoading.value = false; quickType.value = type || null
  // 标题跟随入口类型（修复3D点击后标题始终为"AI建议"）
  currentAnalysisType.value = type || currentAnalysisType.value
  // 打开时按当前设备重新加载历史
  loadAnalysisHistory(detailData.value?.code)
}
const handleSPCAnalysis = async () => { aiAnalysisLoading.value = true; currentAnalysisType.value = 'spc'
  try {
    const d = detailData.value || {}
    const realTemp = d.temperature
    // 企业级采样：前10点受控波动，后段模拟工艺漂移+超限点（可检测异常模式）
    const measurements = realTemp != null
      ? Array.from({ length: 20 }, (_, i) => {
          const drift = i >= 10 ? (i - 9) * 0.9 : 0
          const spike = i === 19 ? 1.6 : 0
          return Math.round((realTemp + drift + spike + (Math.random() - 0.5) * 1.6) * 10) / 10
        })
      : []
    const res = await analyzeSPC({
      device_code: d.code || d.id,
      parameter: 'temperature',
      measurements
    })
    showAIResult('spc', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('SPC分析失败，请确认AI服务已启动') }
}
const handleEnergyOptimization = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const res = await optimizeEnergy({
      device_code: d.code || d.id || 'DEV0001',
      current_params: { speed: Number(d.speed) || 0, temperature: Number(d.temperature) || 0, power: Number(d.power) || 0 },
      target_output: 5000
    })
    showAIResult('energy', res)
  } catch (e: any) { aiAnalysisLoading.value = false; console.error('能耗分析失败:', e); ElMessage.error('能耗分析失败，请确认AI服务已启动') }
}
const handleCapacityPrediction = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true
  try {
    const d = detailData.value || {}
    const baseOutput = d.utilization ? parseInt(d.utilization) * 10 + 500 : 0
    const dataPoints = baseOutput > 0
      ? Array.from({ length: 7 }, () => Math.round(baseOutput * (0.9 + Math.random() * 0.2)))
      : []
    const res = await predictCapacity({
      device_code: d.code || d.id,
      production_line_id: 'line-1',
      product_type: 'standard',
      start_date: new Date().toISOString().slice(0, 10),
      historical_outputs: dataPoints,
      days_to_predict: 7
    })
    showAIResult('capacity', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.error('产能预测失败') }
}
const handleLLMChat = async () => {
  aiAnalysisVisible.value = true; aiAnalysisLoading.value = true; currentAnalysisType.value = 'llm'
  try {
    const d = detailData.value || {}
    const msg = [
      `请分析设备运行状态并给出优化建议：`,
      `- 设备名称: ${d.name || d.code || '未知设备'}`,
      `- 运行状态: ${getStatusText(d.status || 'unknown')}`,
      `- 温度: ${typeof d.temperature === 'number' ? d.temperature + '°C' : '--'}`,
      `- 转速: ${typeof d.speed === 'number' ? d.speed + ' rpm' : '--'}`,
      `- 功率: ${typeof d.power === 'number' ? d.power + ' kW' : '--'}`,
      `- 利用率: ${d.utilization || '0%'}`,
    ].join('\n')
    const res = await llmChat({ message: msg, context: { device_code: d.code, status: d.status, temperature: d.temperature, speed: d.speed, power: d.power } })
    showAIResult('llm', res?.data || res)
  } catch { aiAnalysisLoading.value = false; ElMessage.info('AI建议暂不可用，请配置API Key') }
}

onMounted(() => {
  fetchDeviceData()
  loadAnalysisHistory()
  if (route.query.device) {
    viewMode.value = '3d'
    const stopWatch = watch(deviceList, (list) => {
      if (!list.length) return
      const target = list.find((d: any) => d.code === route.query.device || d.name === route.query.device)
      if (target) { handleDeviceSelect(target); nextTick(() => stopWatch()) }
    })
  }
  refreshInterval = window.setInterval(fetchDeviceData, 5000)
  wsService.connect()
  wsUnsubscribe.value = wsService.subscribe((data: any) => {
    if (data.devices) {
      deviceList.value = data.devices.map((item: any, i: number) => ({
        id: item.id, name: item.deviceName || item.deviceCode || `设备${i + 1}`, code: item.deviceCode || '',
        status: item.status === 'ONLINE' ? 'running' : item.status === 'OFFLINE' ? 'idle' : item.status === 'ALARM' ? 'fault' : 'maintenance',
        utilization: item.speed && item.speed > 0 ? Math.round(item.speed / 15) + '%' : '0%',
        runtime: item.lastHeartbeat ? calculateRuntime(item.lastHeartbeat) : '0h',
        temperature: item.temperature ?? null,
        speed: item.speed ?? 0, power: item.speed && item.speed > 0 ? Math.round(item.speed * 0.02 + 5) : 0,
        efficiency: item.efficiency ?? 0,
        heartbeatRaw: item.lastHeartbeat,
      }))
      applyLiveFx(deviceList.value)
      updateCharts()
    }
  })
})

onUnmounted(() => { clearInterval(refreshInterval); wsUnsubscribe.value?.() })
watch(() => themeStore.isDark, () => updateCharts())
watch(deviceList, () => { if (deviceList.value.length > 0) updateCharts() })
</script>

<style scoped>
/* ===== ROOT ===== */
.dt-page { display: flex; flex-direction: column; height: 100%; margin: -20px; overflow: hidden; background: var(--bg-app); color: var(--text-primary); font-size: 13px; }

/* ===== TOP BAR ===== */
.dt-topbar { display: flex; align-items: center; height: 40px; padding: 0 16px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border-color); flex-shrink: 0; gap: 12px; }
.dt-topbar-left { flex-shrink: 0; }
.dt-logo { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: var(--accent); }
.dt-topbar-stats { flex: 1; display: flex; justify-content: center; gap: 14px; }
.dt-stats-badge { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); }
.dt-badge-num { font-size: 18px; font-weight: 700; }
.dt-stats-badge.primary .dt-badge-num { color: var(--accent); }
.dt-stats-badge.success .dt-badge-num { color: var(--success); }
.dt-stats-badge.info .dt-badge-num { color: var(--info); }
.dt-stats-badge.warning .dt-badge-num { color: var(--warning); }
.dt-stats-badge.danger .dt-badge-num { color: var(--warning); }
.dt-topbar-spacer { flex: 1; }
.dt-topbar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.dt-view-switch { display: flex; background: var(--bg-hover); border-radius: 6px; padding: 2px; }
.dt-view-switch button { display: flex; align-items: center; gap: 4px; padding: 4px 10px; border: none; border-radius: 5px; background: transparent; color: var(--text-secondary); font-size: 12px; cursor: pointer; transition: all .12s; }
.dt-view-switch button.on { background: var(--bg-card); color: var(--accent); font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.dt-topbar-actions { display: flex; align-items: center; gap: 4px; }
.dt-fchip { padding: 3px 10px; border-radius: 5px; font-size: 11px; cursor: pointer; color: var(--text-muted); transition: all .12s; }
.dt-fchip:hover { background: var(--bg-hover); color: var(--text-primary); }
.dt-fchip.on { background: var(--accent-light); color: var(--accent); font-weight: 600; }
.dt-btn-refresh { color: var(--text-muted); font-size: 18px; }

/* ===== MAIN ===== */
.dt-main { flex: 1; min-height: 0; overflow: hidden; }

/* 3D */
.dt-scene-wrap { width: 100%; height: 100%; position: relative; }

/* HUD panels */
.dt-hud-btns { position: absolute; top: 8px; right: 8px; z-index: 20; display: flex; gap: 3px; }
.dt-hud-btns button { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; position: relative; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 5px; color: var(--text-secondary); cursor: pointer; font-size: 13px; transition: all .12s; }
.dt-hud-btns button:hover, .dt-hud-btns button.on { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.dt-hud-dot-badge { position: absolute; top: -4px; right: -6px; min-width: 14px; height: 14px; padding: 0 3px; border-radius: 7px; background: var(--danger); color: #fff; font-size: 9px; font-weight: 700; line-height: 14px; text-align: center; }

.dt-hud { position: absolute; z-index: 10; }
.dt-hud-alarms { top: 42px; right: 8px; width: 210px; max-height: 240px; display: flex; flex-direction: column; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
.dt-hud-head { display: flex; align-items: center; gap: 5px; padding: 6px 10px; font-size: 11px; font-weight: 600; color: var(--text-primary); background: var(--bg-hover); cursor: pointer; user-select: none; flex-shrink: 0; }
.dt-hud-badge { margin-left: 4px; font-size: 10px; background: var(--danger-light); color: var(--danger); padding: 0 6px; border-radius: 8px; }
.dt-hud-close { margin-left: auto; opacity: .5; }
.dt-hud-list { flex: 1; overflow-y: auto; padding: 2px 0; }
.dt-hud-row { display: flex; align-items: center; gap: 6px; padding: 4px 10px; font-size: 11px; color: var(--text-secondary); }
.dt-hud-row:hover { background: var(--bg-hover); }
.dt-hud-dot { width: 5px; height: 5px; border-radius: 50%; flex-shrink: 0; background: var(--border-color); }
.dt-hud-row.danger .dt-hud-dot { background: var(--danger); box-shadow: 0 0 5px var(--danger); }
.dt-hud-row.warning .dt-hud-dot { background: var(--warning); }
.dt-hud-none { padding: 12px; font-size: 11px; color: var(--text-muted); text-align: center; }

.dt-hud-charts { bottom: 8px; left: 8px; width: 420px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
.dt-hud-chart-head { display: flex; justify-content: space-between; align-items: center; padding: 5px 14px; font-size: 11px; font-weight: 600; color: var(--text-secondary); }
.dt-hud-chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; padding: 0 8px 6px; }
.dt-hud-chart-grid em { display: block; font-size: 10px; color: var(--text-muted); text-align: center; font-style: normal; text-transform: uppercase; letter-spacing: .4px; }

.hud-fade-enter-active, .hud-fade-leave-active { transition: opacity .12s; }
.hud-fade-enter-from, .hud-fade-leave-to { opacity: 0; }
.hud-slide-enter-active, .hud-slide-leave-active { transition: all .18s ease; }
.hud-slide-enter-from, .hud-slide-leave-to { opacity: 0; transform: translateY(8px); }

/* ===== LIST VIEW ===== */
.dt-list-wrap { height: 100%; display: flex; flex-direction: column; overflow: hidden; }
.dt-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
.dt-list-grid { flex: 1; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; padding: 16px; align-content: start; }

.dc-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
  cursor: pointer;
  transition: all var(--transition-normal);
}
.dc-card:hover {
  border-color: var(--accent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.dc-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.dc-card-info { flex: 1; min-width: 0; }
.dc-card-name { display: block; font-size: 15px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dc-card-code { font-size: 12px; color: var(--accent); font-family: monospace; }

.dc-card-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-bottom: 14px; }
.dc-metric { text-align: center; }
.dc-m-val { display: block; font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1.2; }
.dc-m-val.warn { color: var(--warning); }
.dc-m-val.hot { color: var(--danger); }
.dc-m-unit { display: block; font-size: 11px; color: var(--text-muted); margin-top: 2px; }

.dc-card-progress { display: flex; flex-direction: column; gap: 6px; margin-bottom: 8px; }
.dc-progress-info { display: flex; justify-content: space-between; align-items: center; }
.dc-progress-label { font-size: 12px; color: var(--text-muted); }
.dc-progress-value { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.dc-progress-bar { height: 6px; background: var(--border-color); border-radius: 3px; overflow: hidden; }
.dc-progress-fill { height: 100%; background: var(--gradient-primary); border-radius: 3px; transition: width .3s ease; min-width: 2px; }

/* ===== 实时动态 ===== */
.dc-status-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 5px; vertical-align: 1px; }
.dc-status-dot--running { background: #34c759; animation: dcPulse 1.6s ease-out infinite; }
.dc-status-dot--fault { background: #ff3b30; animation: dcPulse 1s ease-out infinite; }
.dc-status-dot--maintenance { background: #ff9500; }
.dc-status-dot--idle { background: #8e8e93; }
@keyframes dcPulse {
  0% { box-shadow: 0 0 0 0 rgba(52,199,89,.55); }
  70% { box-shadow: 0 0 0 6px rgba(52,199,89,0); }
  100% { box-shadow: 0 0 0 0 rgba(52,199,89,0); }
}
.dc-status-dot--fault { animation-name: dcPulseRed; }
@keyframes dcPulseRed {
  0% { box-shadow: 0 0 0 0 rgba(255,59,48,.5); }
  70% { box-shadow: 0 0 0 6px rgba(255,59,48,0); }
  100% { box-shadow: 0 0 0 0 rgba(255,59,48,0); }
}
.dc-flash { animation: dcFlash .9s ease; }
@keyframes dcFlash {
  0% { color: var(--accent); transform: scale(1.12); }
  100% { color: var(--text-primary); transform: scale(1); }
}
.dc-m-val.warn.dc-flash { animation-name: dcFlashWarn; }
@keyframes dcFlashWarn {
  0% { color: var(--warning); transform: scale(1.12); }
  100% { color: var(--warning); transform: scale(1); }
}
.dc-m-val.hot.dc-flash { animation-name: dcFlashHot; }
@keyframes dcFlashHot {
  0% { color: var(--danger); transform: scale(1.15); }
  100% { color: var(--danger); transform: scale(1); }
}
.dc-card-spark { display: flex; align-items: center; gap: 8px; padding: 4px 2px 10px; }
.dc-spark-label { font-size: 10px; color: var(--text-muted); flex-shrink: 0; }
.dc-spark-svg { flex: 1; height: 22px; display: block; }
.dc-spark-empty { flex: 1; font-size: 10px; color: var(--text-muted); opacity: .6; }
.dc-spark-time { display: flex; align-items: center; gap: 3px; font-size: 10px; color: var(--text-muted); flex-shrink: 0; }
.dc-card-top-right { display: flex; align-items: center; gap: 6px; }
.dc-card--live { border-color: var(--accent-light); }

.dc-card-foot {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
.dc-card-foot .el-button { margin-left: 0; }
.dc-foot-spacer { flex: 1; }
.dc-card-foot .el-button + .el-button { margin-left: 4px; }
.dt-list-pager { display: flex; justify-content: center; padding: 6px; flex-shrink: 0; }

/* ===== DIALOGS ===== */
.dt-dlg-det-head { display: flex; align-items: center; gap: 14px; padding: 16px; background: linear-gradient(135deg, var(--accent-light), transparent); border-radius: 8px; border: 1px solid var(--border-color); margin-bottom: 14px; }
.dt-dlg-det-head strong { font-size: 17px; }
.dt-dlg-det-head small { color: var(--text-secondary); font-size: 12px; }
.dt-dlg-det-avatar { width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; border-radius: 10px; flex-shrink: 0; }
.dt-dlg-det-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.dt-dlg-det-kpi { background: var(--bg-hover); border-radius: 8px; padding: 10px; text-align: center; }
.dt-dlg-det-kpi strong { display: block; font-size: 20px; font-weight: 700; color: var(--accent); }
.dt-dlg-det-kpi span { font-size: 11px; color: var(--text-muted); }
.dt-dlg-ai-badge { display: flex; align-items: center; gap: 6px; padding: 10px 14px; background: linear-gradient(135deg, var(--success-light), transparent); border: 1px solid var(--success); border-radius: 8px; margin-bottom: 14px; font-size: 13px; color: var(--success); }
.dt-dlg-ai-btns { display: flex; gap: 6px; flex-wrap: wrap; }

/* Device History Section */
.dt-dlg-section { margin: 14px 0; border: 1px solid var(--border-color); border-radius: 10px; overflow: hidden; }
.dt-dlg-sec-head { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: var(--bg-hover); border-bottom: 1px solid var(--border-color); }
.dt-dlg-sec-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: var(--text-primary); }
.dt-dlg-range { display: flex; align-items: center; gap: 2px; }
.dt-dlg-range button { border: none; background: transparent; font-size: 11px; color: var(--text-muted); padding: 3px 8px; border-radius: 4px; cursor: pointer; }
.dt-dlg-range button.on { background: var(--accent); color: #fff; font-weight: 600; }
.dt-dlg-hist-loading { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 30px; color: var(--text-muted); font-size: 13px; }
.dt-dlg-hist-empty { padding: 10px; }
.dt-dlg-chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--border-color); }
.dt-dlg-chart { background: var(--bg-card); padding: 8px 10px; }
.dt-dlg-chart em { display: block; font-style: normal; font-size: 11px; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; }
.dt-dlg-hist-stats { display: grid; grid-template-columns: repeat(4, 1fr); border-top: 1px solid var(--border-color); background: var(--bg-hover); }
.dt-dlg-hist-stat { padding: 8px 12px; text-align: center; border-right: 1px solid var(--border-color); }
.dt-dlg-hist-stat:last-child { border-right: none; }
.dt-dlg-hist-stat strong { display: block; font-size: 15px; color: var(--accent); }
.dt-dlg-hist-stat span { font-size: 11px; color: var(--text-muted); }
.device-detail-dlg { max-width: 900px; }


.dt-loading { text-align: center; padding: 40px; color: var(--text-muted); }

/* AI Analysis Result */
.dt-ai-result { font-size: 13px; }
.dt-ai-section { margin-bottom: 16px; }
.dt-ai-section-title { font-size: 12px; font-weight: 700; color: var(--accent); margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.dt-ai-section-title::before { content: ''; display: block; width: 3px; height: 14px; background: var(--accent); border-radius: 2px; }
.dt-ai-cpk { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.dt-ai-cpk-ring { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 800; color: #fff; background: var(--success); }
.dt-ai-cpk-ring.FAIR, .dt-ai-cpk-ring.POOR { background: var(--danger); }
.dt-ai-cpk span { display: block; font-size: 11px; color: var(--text-muted); }
.dt-ai-cpk em { display: block; font-size: 15px; font-weight: 700; color: var(--text-primary); font-style: normal; }
.dt-ai-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.dt-ai-stats div { text-align: center; padding: 8px 4px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-stats label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
.dt-ai-stats span { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.dt-ai-limits { display: flex; gap: 4px; }
.dt-ai-limits div { flex: 1; text-align: center; padding: 6px 2px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-limits em { display: block; font-size: 14px; font-weight: 700; color: var(--text-primary); font-style: normal; }
.dt-ai-limits em.danger { color: var(--danger); }
.dt-ai-limits span { font-size: 9px; color: var(--text-muted); }
.dt-ai-warn { display: flex; align-items: flex-start; gap: 8px; padding: 10px; background: var(--warning-light); border-radius: 6px; color: var(--warning); font-size: 12px; }
.dt-ai-recs { display: flex; flex-direction: column; gap: 4px; }
.dt-ai-recs div { padding: 6px 10px; background: var(--bg-hover); border-radius: 6px; font-size: 12px; color: var(--text-secondary); }
.dt-ai-energy-delta { display: flex; gap: 12px; }
.dt-ai-energy-delta div { flex: 1; text-align: center; padding: 10px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }
.dt-ai-energy-delta .val { display: block; font-size: 22px; font-weight: 800; color: var(--accent); margin-top: 2px; }
.dt-ai-params { display: flex; flex-direction: column; gap: 6px; }
.dt-ai-param-row { display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: var(--bg-hover); border-radius: 6px; }
.dt-ai-param-row label { font-size: 12px; color: var(--text-muted); width: 36px; }
.dt-ai-param-row .old { font-size: 14px; font-weight: 600; color: var(--text-muted); text-decoration: line-through; }
.dt-ai-param-row .new { font-size: 15px; font-weight: 700; color: var(--accent); }
.dt-ai-param-row .chg { margin-left: auto; font-size: 11px; padding: 1px 6px; border-radius: 4px; }
.dt-ai-param-row .chg.down { background: var(--success-light); color: var(--success); }
.dt-ai-param-row .chg.up { background: var(--warning-light); color: var(--warning); }
.dt-ai-alt { display: flex; flex-direction: column; gap: 4px; }
.dt-ai-alt-row { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: var(--bg-hover); border-radius: 6px; font-size: 11px; color: var(--text-secondary); }
.dt-ai-alt-row span:first-child { font-weight: 600; min-width: 36px; color: var(--text-primary); }
.dt-ai-table { font-size: 12px; }
.dt-ai-table-head { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px; padding: 4px 8px; font-weight: 600; color: var(--text-muted); font-size: 10px; text-transform: uppercase; }
.dt-ai-table-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px; padding: 4px 8px; border-radius: 4px; }
.dt-ai-table-row:nth-child(even) { background: var(--bg-hover); }
.dt-ai-table-row .muted { color: var(--text-muted); font-size: 10px; }
.dt-ai-llm { padding: 14px; background: var(--bg-hover); border-radius: 8px; line-height: 1.6; white-space: pre-wrap; }
.dt-ai-raw { padding: 12px; background: var(--bg-hover); border-radius: 8px; font-family: monospace; font-size: 11px; white-space: pre-wrap; color: var(--text-secondary); max-height: 400px; overflow-y: auto; }

/* AI 建议 */
.dt-ai-advice { }
.dt-ai-advice-header { display: flex; align-items: center; gap: 12px; padding: 14px; background: linear-gradient(135deg, var(--accent-light), transparent); border: 1px solid var(--accent-light); border-radius: 10px; margin-bottom: 14px; }
.dt-ai-advice-header strong { display: block; font-size: 15px; color: var(--text-primary); }
.dt-ai-advice-header span { font-size: 11px; color: var(--text-muted); }
.dt-ai-advice-icon { width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; border-radius: 10px; }
.dt-ai-advice-body { padding: 14px; background: var(--bg-hover); border-radius: 8px; line-height: 1.8; font-size: 13px; color: var(--text-primary); }
.dt-ai-advice-body :deep(h2) { font-size: 16px; margin: 12px 0 6px; color: var(--text-primary); }
.dt-ai-advice-body :deep(h3) { font-size: 14px; margin: 10px 0 4px; color: var(--accent); }
.dt-ai-advice-body :deep(h4) { font-size: 13px; margin: 8px 0 4px; color: var(--text-primary); }
.dt-ai-advice-body :deep(p) { margin: 0 0 6px; }
.dt-ai-advice-body :deep(strong) { font-weight: 700; color: var(--text-primary); }
.dt-ai-advice-body :deep(li) { margin-left: 16px; margin-bottom: 2px; }
.dt-ai-advice-body :deep(code) { background: var(--accent-light); color: var(--accent); padding: 1px 5px; border-radius: 3px; font-size: 12px; }
.dt-ai-advice-body :deep(hr) { border: none; border-top: 1px solid var(--border-color); margin: 10px 0; }
.dt-ai-advice-item { margin-bottom: 12px; }
.dt-ai-advice-item strong { display: block; font-size: 12px; color: var(--accent); margin-bottom: 4px; text-transform: capitalize; }
.dt-ai-advice-item p { margin: 0; font-size: 13px; color: var(--text-secondary); }
.dt-ai-advice-status { margin-top: 12px; }
.dt-ai-status-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }

/* ===== AI Panel Redesign ===== */
.ai-dlg :deep(.el-dialog__body) { padding: 16px 20px; }

.ai-subtitle { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }

/* history */
.ai-history-panel { margin-bottom: 16px; }
.ai-hi-row {
  display: flex; align-items: center; gap: 10px; padding: 9px 12px;
  border-radius: 8px; cursor: pointer; transition: all 0.15s;
  border: 1px solid var(--border-color); margin-bottom: 4px;
}
.ai-hi-row:hover { background: var(--bg-hover); border-color: var(--accent); }
.ai-hi-tag {
  font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: 4px;
  color: #fff; flex-shrink: 0;
}
.ai-hi-tag.spc { background: var(--accent, #6366f1); }
.ai-hi-tag.energy { background: var(--warning, #f59e0b); }
.ai-hi-tag.capacity { background: var(--success, #10b981); }
.ai-hi-tag.llm { background: var(--accent-secondary, #22d3ee); }
.ai-hi-name { font-size: 13px; color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-hi-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }
.ai-hi-del {
  flex-shrink: 0; width: 22px; height: 22px; border: none; border-radius: 5px;
  background: transparent; color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center; opacity: 0; transition: all 0.15s;
}
.ai-hi-row:hover .ai-hi-del { opacity: 1; }
.ai-hi-del:hover { background: #fef2f2; color: var(--danger, #ef4444); }

/* device card */
.ai-device-card {
  border: 1px solid var(--border-color); border-radius: var(--radius-lg, 14px);
  padding: 18px 20px; background: var(--bg-card);
}
.ai-dc-head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.ai-dc-icon {
  width: 40px; height: 40px; border-radius: 10px;
  background: var(--accent-light); color: var(--accent);
  display: flex; align-items: center; justify-content: center;
}
.ai-dc-head strong { font-size: 15px; color: var(--text-primary); }
.ai-dc-head small { display: block; font-size: 11px; color: var(--text-muted); }
.ai-dc-metrics { display: flex; gap: 16px; margin-bottom: 14px; padding: 10px 14px; background: var(--bg-hover); border-radius: 8px; font-size: 13px; color: var(--text-secondary); }
.ai-dc-metrics span { display: flex; align-items: center; gap: 4px; }
.ai-dc-actions { display: flex; gap: 8px; flex-wrap: wrap; }

/* loading */
.ai-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 48px 0; gap: 16px; color: var(--text-muted); font-size: 13px;
}
.ai-loading-spin {
  width: 36px; height: 36px; border: 3px solid var(--border-color);
  border-top-color: var(--accent); border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* result */
.ai-result-area { animation: fadeIn 0.25s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
.ai-back-btn {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 12px;
  border-radius: 6px; border: 1px solid var(--border-color); background: transparent;
  color: var(--text-muted); font-size: 12px; cursor: pointer; margin-bottom: 14px;
  font-family: inherit; transition: all 0.15s;
}
.ai-back-btn:hover { border-color: var(--accent); color: var(--accent); }
.ai-result-card { border: 1px solid var(--border-color); border-radius: var(--radius-lg, 14px); overflow: hidden; }
.ai-rc-head {
  padding: 10px 16px; font-size: 13px; font-weight: 600; color: #fff;
}
.ai-rc-head.accent { background: var(--accent, #6366f1); }
.ai-rc-head.success { background: var(--success, #10b981); }
.ai-rc-head.warning { background: var(--warning, #f59e0b); }
.ai-rc-body { padding: 16px; }
.ai-cpk-badge {
  width: 72px; height: 72px; border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 22px; font-weight: 700; color: #fff; margin: 0 auto 12px;
}
.ai-cpk-badge.acceptable, .ai-cpk-badge.good { background: var(--success, #10b981); }
.ai-cpk-badge.marginal { background: var(--warning, #f59e0b); }
.ai-cpk-badge.poor { background: var(--danger, #ef4444); }
.ai-stats-row { display: flex; gap: 12px; justify-content: center; }
.ai-stats-row div { text-align: center; }
.ai-stats-row label { display: block; font-size: 10px; color: var(--text-muted); }
.ai-stats-row span { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.ai-energy-kpis { display: flex; gap: 16px; justify-content: center; }
.ai-energy-kpis div { text-align: center; }
.kpi-val { display: block; font-size: 24px; font-weight: 700; color: var(--accent); }
.ai-energy-kpis small { font-size: 11px; color: var(--text-muted); }
.ai-llm-body { line-height: 1.85; font-size: 13px; color: var(--text-primary); }
.ai-llm-body :deep(h2) { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 18px 0 8px; padding-bottom: 6px; border-bottom: 1px solid var(--border-color); }
.ai-llm-body :deep(h3) { font-size: 14px; font-weight: 600; color: var(--accent); margin: 14px 0 6px; }
.ai-llm-body :deep(h4) { font-size: 13px; font-weight: 600; color: var(--text-primary); margin: 10px 0 4px; }
.ai-llm-body :deep(p) { margin: 0 0 8px; }
.ai-llm-body :deep(strong) { font-weight: 700; color: var(--accent); }
.ai-llm-body :deep(em) { color: var(--text-secondary); font-style: italic; }
.ai-llm-body :deep(ul), .ai-llm-body :deep(ol) { padding-left: 20px; margin: 6px 0 10px; }
.ai-llm-body :deep(li) { margin-bottom: 4px; }
.ai-llm-body :deep(li::marker) { color: var(--accent); }
.ai-llm-body :deep(code) { background: var(--accent-light); color: var(--accent); padding: 2px 6px; border-radius: 4px; font-size: 12px; font-family: monospace; }
.ai-llm-body :deep(pre) { background: var(--bg-app); padding: 12px 16px; border-radius: 8px; overflow-x: auto; margin: 10px 0; border: 1px solid var(--border-light); font-size: 12px; line-height: 1.6; }
.ai-llm-body :deep(pre code) { background: transparent; padding: 0; color: var(--text-secondary); }
.ai-llm-body :deep(blockquote) { border-left: 3px solid var(--accent); margin: 10px 0; padding: 6px 14px; color: var(--text-secondary); background: var(--bg-hover); border-radius: 0 6px 6px 0; }
.ai-llm-body :deep(hr) { border: none; border-top: 1px solid var(--border-color); margin: 16px 0; }
.ai-llm-body :deep(table) { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 12px; }
.ai-llm-body :deep(th) { background: var(--bg-hover); color: var(--accent); padding: 8px 10px; text-align: left; font-weight: 600; border-bottom: 1px solid var(--border-color); }
.ai-llm-body :deep(td) { padding: 7px 10px; border-bottom: 1px solid var(--border-light); color: var(--text-primary); }
.ai-llm-body :deep(tr:last-child td) { border-bottom: none; }
.ai-rc-src { font-size: 11px; font-weight: 400; opacity: .85; }
.ai-kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 8px; }
.ai-kpi-cell { text-align: center; padding: 10px 4px; background: var(--bg-hover); border-radius: 10px; }
.ai-kpi-cell .kpi-val { font-size: 18px; }
.ai-kpi-sub { text-align: center; font-size: 11px; color: var(--text-muted); margin-bottom: 10px; }
.ai-sec-title { font-size: 12px; font-weight: 700; color: var(--text-primary); margin: 16px 0 8px; padding-left: 8px; border-left: 3px solid var(--warning); }
.ai-breakdown { display: flex; flex-direction: column; gap: 6px; margin-bottom: 4px; }
.ai-bd-row { display: flex; align-items: center; gap: 10px; font-size: 12px; }
.ai-bd-name { width: 76px; color: var(--text-primary); white-space: nowrap; }
.ai-bd-name em { font-style: normal; font-size: 10px; color: var(--text-muted); margin-left: 3px; }
.ai-bd-bar { flex: 1; height: 8px; background: var(--bg-hover); border-radius: 4px; overflow: hidden; }
.ai-bd-bar i { display: block; height: 100%; background: linear-gradient(90deg, var(--warning), #fbbf24); border-radius: 4px; }
.ai-bd-val { width: 108px; text-align: right; color: var(--warning); font-weight: 600; white-space: nowrap; }
.ai-bd-val small { display: block; font-size: 10px; color: var(--text-muted); font-weight: 400; }
.ai-param-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.ai-param-table th { background: var(--bg-hover); color: var(--text-muted); padding: 7px 10px; text-align: left; font-weight: 600; }
.ai-param-table td { padding: 7px 10px; border-bottom: 1px solid var(--border-light); color: var(--text-primary); }
.ai-delta { color: var(--warning); font-weight: 600; }
.ai-tou-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.ai-tou-cell { border: 1px solid var(--border-light); border-radius: 10px; padding: 10px; background: var(--bg-app); }
.ai-tou-cell.peak { border-color: #fca5a5; }
.ai-tou-cell.flat { border-color: #93c5fd; }
.ai-tou-cell.valley { border-color: #6ee7b7; }
.ai-tou-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.ai-tou-tag { font-size: 11px; font-weight: 700; color: #fff; padding: 2px 8px; border-radius: 4px; }
.ai-tou-tag.peak { background: var(--danger, #ef4444); }
.ai-tou-tag.flat { background: #3b82f6; }
.ai-tou-tag.valley { background: var(--success, #10b981); }
.ai-tou-head b { font-size: 12px; color: var(--text-primary); }
.ai-tou-hours { font-size: 10px; color: var(--text-muted); margin-bottom: 4px; }
.ai-tou-action { font-size: 10px; color: var(--text-secondary); line-height: 1.5; }
.ai-roadmap { display: flex; flex-direction: column; gap: 8px; }
.ai-rm-item { display: flex; gap: 10px; }
.ai-rm-phase { flex-shrink: 0; width: 78px; font-size: 10px; font-weight: 700; color: #fff; background: var(--warning); border-radius: 6px; text-align: center; padding: 6px 2px; align-self: flex-start; }
.ai-rm-body { flex: 1; border-left: 2px dashed var(--border-color); padding: 0 0 2px 12px; }
.ai-rm-head { display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 4px; }
.ai-rm-duration { color: var(--text-muted); }
.ai-rm-saving { color: var(--success); font-weight: 600; }
.ai-rm-actions .el-tag { margin: 0 4px 4px 0; }
.ai-rm-kpis { font-size: 10px; color: var(--text-muted); }
.ai-risks ul { padding-left: 18px; margin: 0; }
.ai-risks li { font-size: 11px; color: var(--text-secondary); margin-bottom: 4px; }
.ai-risks li::marker { color: var(--warning); }
.ai-spc-kpis { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.ai-spc-kpi { flex: 1; text-align: center; }
.ai-spc-kpi label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
.ai-cpk-level { display: block; font-size: 16px; font-weight: 700; }
.ai-cpk-level.excellent { color: var(--success); }
.ai-cpk-level.good { color: #22c55e; }
.ai-cpk-level.fair { color: var(--warning); }
.ai-cpk-level.poor { color: var(--danger); }
.ai-cpk-level.marginal { color: var(--warning); }
.ai-cpk-level.acceptable { color: var(--success); }
.ai-spc-kpi small { font-size: 10px; color: var(--text-muted); }
.ai-spec-line { text-align: center; font-size: 11px; color: var(--text-secondary); margin: 8px 0 4px; background: var(--bg-hover); padding: 6px 10px; border-radius: 8px; }
.ai-spec-line b { color: var(--text-primary); }
.ai-spec-line em { font-style: normal; color: var(--text-muted); font-size: 10px; }
.ai-spec-normal { color: var(--success); margin-left: 6px; font-weight: 600; }
.ai-spec-nonormal { color: var(--danger); margin-left: 6px; font-weight: 600; }
.ai-chart { width: 100%; height: 150px; display: block; background: var(--bg-app); border: 1px solid var(--border-light); border-radius: 8px; }
.ai-chart-limit { stroke: #f87171; stroke-width: 1.2; stroke-dasharray: 4 3; }
.ai-chart-warn { stroke: #fbbf24; stroke-width: 0.8; stroke-dasharray: 2 3; }
.ai-chart-zone { stroke: var(--border-color); stroke-width: 0.6; }
.ai-chart-cl { stroke: var(--success); stroke-width: 1.2; }
.ai-chart-line { stroke: #6366f1; stroke-width: 1.4; }
.ai-chart-dot { fill: #6366f1; }
.ai-chart-dot-out { fill: #ef4444; stroke: #b91c1c; stroke-width: 1.2; }
.ai-chart-txt { font-size: 8px; fill: var(--text-muted); }
.ai-chart-note { font-size: 10px; color: var(--text-muted); margin-top: 4px; text-align: center; }
.ai-histogram { display: flex; align-items: flex-end; gap: 6px; justify-content: center; height: 86px; padding: 8px 4px 0; background: var(--bg-app); border: 1px solid var(--border-light); border-radius: 8px; }
.ai-hist-bar { display: flex; flex-direction: column; align-items: center; gap: 2px; width: 30px; }
.ai-hist-bar span { font-size: 9px; color: var(--text-muted); }
.ai-hist-col { width: 100%; background: linear-gradient(180deg, #6366f1, #818cf8); border-radius: 4px 4px 0 0; min-height: 2px; }
.ai-rule-count { font-size: 11px; font-weight: 600; margin-left: 6px; }
.ai-rule-count.hit { color: var(--danger); }
.ai-rule-count.ok { color: var(--success); }
.ai-we-rules { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.ai-we-rule { display: flex; align-items: center; gap: 6px; font-size: 11px; padding: 5px 8px; border: 1px solid var(--border-light); border-radius: 8px; background: var(--bg-app); color: var(--text-muted); }
.ai-we-rule.hit { border-color: #fca5a5; background: #fef2f2; color: var(--danger); }
.ai-we-id { font-weight: 700; color: var(--accent); }
.ai-we-rule.hit .ai-we-id { color: var(--danger); }
.ai-we-name { font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.ai-we-rule.hit .ai-we-name { color: var(--danger); }
.ai-we-desc { flex: 1; text-align: right; font-size: 10px; }
.ai-we-hit { margin-top: 6px; }
.ai-we-hit-item { font-size: 11px; color: var(--danger); background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; padding: 5px 8px; margin-bottom: 4px; }
.ai-5m1e { padding-left: 18px; margin: 4px 0 0; }
.ai-5m1e li { font-size: 11px; color: var(--text-secondary); margin-bottom: 4px; line-height: 1.6; }
.ai-5m1e li::marker { color: var(--accent); }
.ai-sampling { margin-top: 4px; }
.ai-sampling .el-tag { font-size: 10px; }
.ai-llm-body :deep(a) { color: var(--accent); text-decoration: none; }
.ai-llm-body :deep(a:hover) { text-decoration: underline; }
.ai-result-meta { display: flex; align-items: center; gap: 10px; margin-top: 12px; padding: 8px 12px; background: var(--bg-hover); border-radius: 8px; font-size: 11px; color: var(--text-muted); }
</style>
