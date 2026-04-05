namespace MesDeviceGateway.Models;

/// <summary>
/// Kafka消息模型
/// </summary>
public class KafkaMessage
{
    /// <summary>
    /// Kafka主题
    /// </summary>
    public string Topic { get; set; } = string.Empty;
    /// <summary>
    /// 消息键
    /// </summary>
    public string Key { get; set; } = string.Empty;
    /// <summary>
    /// 消息值
    /// </summary>
    public object Value { get; set; } = new();
    /// <summary>
    /// 消息时间戳
    /// </summary>
    public DateTime Timestamp { get; set; }
}
