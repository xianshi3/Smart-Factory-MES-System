/**
 * MES设备模拟器入口
 * 模拟2000+设备通过MQTT上报数据
 */

// 加载环境变量配置
require('dotenv').config();
const DeviceManager = require('./device/DeviceManager');

// 配置参数（从环境变量读取）
const config = {
  deviceCount: parseInt(process.env.DEVICE_COUNT) || 2000,
  mqttBroker: process.env.MQTT_BROKER || 'mqtt://localhost:1883',
  dataInterval: parseInt(process.env.DATA_INTERVAL) || 5000,
  reportInterval: parseInt(process.env.REPORT_INTERVAL) || 10000,
};

// 打印启动信息
console.log('========================================');
console.log('    MES 设备模拟器启动');
console.log('========================================');
console.log(`设备数量: ${config.deviceCount}`);
console.log(`MQTT地址: ${config.mqttBroker}`);
console.log(`数据上报间隔: ${config.dataInterval}ms`);
console.log(`状态上报间隔: ${config.reportInterval}ms`);
console.log('========================================');

// 创建设备管理器实例
const deviceManager = new DeviceManager(config);

// 监听事件：设备初始化完成
deviceManager.on('ready', () => {
  console.log(`[模拟器] 已创建 ${config.deviceCount} 个设备`);
});

// 监听事件：发生错误
deviceManager.on('error', (err) => {
  console.error(`[模拟器] 错误: ${err.message}`);
});

// 监听退出信号，优雅关闭
process.on('SIGINT', () => {
  console.log('\n[模拟器] 正在关闭...');
  deviceManager.disconnect();
  process.exit(0);
});

// 启动模拟器
deviceManager.connect().then(() => {
  console.log('[模拟器] 连接成功，开始模拟设备数据');
}).catch(err => {
  console.error('[模拟器] 连接失败:', err.message);
  process.exit(1);
});