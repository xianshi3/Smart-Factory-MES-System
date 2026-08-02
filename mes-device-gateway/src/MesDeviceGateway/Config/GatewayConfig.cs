namespace MesDeviceGateway.Config;

/// <summary>
/// 网关配置类
/// </summary>
public class GatewayConfig
{
    /// <summary>
    /// MQTT服务器地址
    /// </summary>
    public string MqttServer { get; set; } = "localhost";
    /// <summary>
    /// MQTT服务器端口
    /// </summary>
    public int MqttPort { get; set; } = 1883;
    /// <summary>
    /// MQTT用户名
    /// </summary>
    public string MqttUsername { get; set; } = string.Empty;
    /// <summary>
    /// MQTT密码
    /// </summary>
    public string MqttPassword { get; set; } = string.Empty;
    /// <summary>
    /// Kafka引导服务器地址
    /// </summary>
    public string KafkaBootstrapServers { get; set; } = "localhost:9092";
    /// <summary>
    /// 批处理大小
    /// </summary>
    public int BatchSize { get; set; } = 100;
    /// <summary>
    /// 刷新间隔(毫秒)
    /// </summary>
    public int FlushIntervalMs { get; set; } = 1000;
    /// <summary>
    /// 滑动窗口秒数
    /// </summary>
    public int SlidingWindowSeconds { get; set; } = 10;
    /// <summary>
    /// 最大设备数量
    /// </summary>
    public int MaxDevices { get; set; } = 2000;
    /// <summary>
    /// MQTT重连间隔(毫秒)
    /// </summary>
    public int MqttReconnectIntervalMs { get; set; } = 5000;
    /// <summary>
    /// Kafka生产者 idempotent
    /// </summary>
    public bool EnableIdempotent { get; set; } = true;
    /// <summary>
    /// 消息处理通道缓冲区大小
    /// </summary>
    public int ChannelBufferSize { get; set; } = 10000;
    /// <summary>
    /// 启用健康检查
    /// </summary>
    public bool EnableHealthChecks { get; set; } = true;
    /// <summary>
    /// Redis服务器地址（用于设备在线心跳）
    /// </summary>
    public string RedisServer { get; set; } = "localhost";
    /// <summary>
    /// Redis服务器端口
    /// </summary>
    public int RedisPort { get; set; } = 6379;
    /// <summary>
    /// Redis密码
    /// </summary>
    public string RedisPassword { get; set; } = string.Empty;
    /// <summary>
    /// 设备心跳TTL秒数（超过该时间未上报视为离线）
    /// </summary>
    public int DeviceHeartbeatTtlSeconds { get; set; } = 90;
}
