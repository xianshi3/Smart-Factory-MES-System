namespace MesDeviceGateway.Models;

/// <summary>
/// 设备状态消息模型
/// </summary>
public class DeviceStatusMessage
{
    /// <summary>
    /// 设备唯一标识
    /// </summary>
    public string DeviceId { get; set; } = string.Empty;
    /// <summary>
    /// 设备状态
    /// </summary>
    public string Status { get; set; } = string.Empty;
    /// <summary>
    /// 心跳时间
    /// </summary>
    public DateTime HeartbeatTime { get; set; }
    /// <summary>
    /// 额外信息
    /// </summary>
    public Dictionary<string, object> ExtraInfo { get; set; } = new();
}
