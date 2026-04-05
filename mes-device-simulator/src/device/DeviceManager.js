/**
 * 设备管理器
 * 负责管理设备生命周期、MQTT连接、数据生成与上报
 */

const mqtt = require('mqtt');
const EventEmitter = require('events');

/**
 * 设备管理器类
 * 继承EventEmitter，支持事件驱动
 */
class DeviceManager extends EventEmitter {
  /**
   * 构造函数
   * @param {Object} config 配置对象
   */
  constructor(config) {
    super();
    this.config = config;
    this.client = null;
    this.devices = [];
    this.running = false;
  }

  /**
   * 连接MQTT broker
   * @returns {Promise} 连接结果
   */
  async connect() {
    return new Promise((resolve, reject) => {
      console.log(`[MQTT] 正在连接 ${this.config.mqttBroker}...`);
      
      this.client = mqtt.connect(this.config.mqttBroker, {
        clientId: `mes-simulator-${Math.random().toString(16).slice(2, 10)}`,
        clean: true,
        connectTimeout: 10000,
        reconnectPeriod: 5000,
      });

      // 连接成功回调
      this.client.on('connect', () => {
        console.log('[MQTT] 连接成功');
        this.initializeDevices();
        this.running = true;
        this.startDataSimulation();
        resolve();
      });

      // 连接错误回调
      this.client.on('error', (err) => {
        console.error(`[MQTT] 连接错误: ${err.message}`);
        this.emit('error', err);
        reject(err);
      });

      // 离线回调
      this.client.on('offline', () => {
        console.warn('[MQTT] 连接离线');
      });

      // 重连回调
      this.client.on('reconnect', () => {
        console.log('[MQTT] 正在重连...');
      });
    });
  }

  /**
   * 初始化所有设备
   */
  initializeDevices() {
    for (let i = 1; i <= this.config.deviceCount; i++) {
      const deviceId = `DEV${String(i).padStart(4, '0')}`;
      this.devices.push(this.createDevice(deviceId));
    }
    this.emit('ready');
  }

  /**
   * 创建设备对象
   * @param {string} deviceId 设备ID
   * @returns {Object} 设备对象
   */
  createDevice(deviceId) {
    // 辅助函数：生成指定范围内的随机值
    const randomValue = (min, max) => Math.random() * (max - min) + min;
    
    return {
      deviceId,
      deviceName: `设备-${deviceId}`,
      // 随机分配设备类型：CNC/PLC/Robot/AGV/Sensor
      deviceType: ['CNC', 'PLC', 'Robot', 'AGV', 'Sensor'][Math.floor(Math.random() * 5)],
      workstationId: Math.floor(Math.random() * 10) + 1,
      productionLineId: Math.floor(Math.random() * 3) + 1,
      status: 'ONLINE',
      
      // 设备参数（实时变化）
      params: {
        temperature: randomValue(20, 80),  // 温度
        pressure: randomValue(0.5, 2.0),   // 压力
        speed: randomValue(100, 1000),      // 速度
        vibration: randomValue(0, 10),     // 振动
        power: randomValue(0, 100),        // 功率
        runtime: 0,                         // 运行时长
      },
      
      // 参数基准范围
      baseParams: {
        temperature: { min: 20, max: 80, warning: 75 },
        pressure: { min: 0.5, max: 2.0, warning: 1.8 },
        speed: { min: 100, max: 1000, warning: 950 },
        vibration: { min: 0, max: 10, warning: 8 },
        power: { min: 0, max: 100, warning: 90 },
      },

      /**
       * 生成设备数据
       * 模拟参数变化，返回变化后的数据
       */
      generateData() {
        const paramChanges = {};
        
        for (const [key, value] of Object.entries(this.params)) {
          if (key === 'runtime') {
            this.params.runtime += this.config.dataInterval / 1000;
            paramChanges.runtime = Math.floor(this.params.runtime);
            continue;
          }

          const base = this.baseParams[key];
          const delta = (Math.random() - 0.5) * 2;
          let newValue = value + delta * 0.1 * base.max;
          
          // 限制在合理范围内
          newValue = Math.max(base.min, Math.min(base.max, newValue));
          this.params[key] = Math.round(newValue * 100) / 100;
          paramChanges[key] = this.params[key];
        }

        // 随机状态变化（1%概率变为告警或维护）
        if (Math.random() < 0.01) {
          this.status = Math.random() < 0.5 ? 'ALARM' : 'MAINTENANCE';
        } else if (this.status === 'ALARM' && Math.random() < 0.8) {
          this.status = 'ONLINE';
        }

        return {
          deviceId: this.deviceId,
          timestamp: Date.now(),
          status: this.status,
          params: paramChanges,
          workstationId: this.workstationId,
          productionLineId: this.productionLineId,
        };
      },

      /**
       * 生成设备状态
       */
      generateStatus() {
        return {
          deviceId: this.deviceId,
          deviceName: this.deviceName,
          deviceType: this.deviceType,
          status: this.status,
          temperature: Math.round(this.params.temperature * 100) / 100,
          speed: Math.round(this.params.speed * 100) / 100,
          lastHeartbeat: new Date().toISOString(),
          workstationId: this.workstationId,
          productionLineId: this.productionLineId,
        };
      },
    };
  }

  /**
   * 启动数据模拟（定时上报）
   */
  startDataSimulation() {
    // 定时上报设备数据
    setInterval(() => {
      if (!this.running) return;
      
      const batchSize = 100;
      const startIdx = Math.floor(Math.random() * (this.config.deviceCount - batchSize));
      const batchDevices = this.devices.slice(startIdx, startIdx + batchSize);
      
      const messages = batchDevices.map(device => {
        const data = device.generateData();
        return {
          topic: `mes/device/${data.deviceId}/data`,
          payload: JSON.stringify(data),
        };
      });

      messages.forEach(msg => {
        this.client.publish(msg.topic, msg.payload);
      });

      console.log(`[上报] 已发送 ${messages.length} 条设备数据`);
    }, this.config.dataInterval);

    // 定时上报设备状态
    setInterval(() => {
      if (!this.running) return;
      
      const statusMessages = this.devices.slice(0, 50).map(device => {
        const status = device.generateStatus();
        return {
          topic: `mes/device/${status.deviceId}/status`,
          payload: JSON.stringify(status),
        };
      });

      statusMessages.forEach(msg => {
        this.client.publish(msg.topic, msg.payload);
      });

      console.log(`[状态] 已发送 ${statusMessages.length} 条设备状态`);
    }, this.config.reportInterval);
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.running = false;
    if (this.client) {
      this.client.end();
    }
    console.log('[模拟器] 已断开连接');
  }
}

module.exports = DeviceManager;