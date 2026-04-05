namespace MesDeviceGateway.Models;

/// <summary>
/// 设备数据消息模型
/// </summary>
public class DeviceDataMessage
{
    /// <summary>
    /// 设备唯一标识
    /// </summary>
    public string DeviceId { get; set; } = string.Empty;
    /// <summary>
    /// 数据时间戳
    /// </summary>
    public DateTime Timestamp { get; set; }
    /// <summary>
    /// 设备数据字典
    /// </summary>
    public Dictionary<string, object> Data { get; set; } = new();
    /// <summary>
    /// 数据类型
    /// </summary>
    public string DataType { get; set; } = string.Empty;
}
