namespace MESDeviceSimulator;

/// <summary>
/// 模拟设备（支持批量模拟：每台设备独立参数 + 个体随机偏移）
/// </summary>
public class SimulatedDevice
{
    public string DeviceCode { get; set; } = "";
    public string DeviceName { get; set; } = "";
    public string DeviceType { get; set; } = "CNC";
    public string Status { get; set; } = "ONLINE";

    public double Temperature { get; set; }
    public double Speed { get; set; }
    public double Pressure { get; set; }
    public double Power { get; set; }

    /// <summary>个体随机偏移（批量模拟时每台设备数值略有差异）</summary>
    public double TempOffset { get; set; }
    public double SpeedOffset { get; set; }

    /// <summary>是否已在后端创建</summary>
    public bool Created { get; set; }

    public SimulatedDevice() { }

    public SimulatedDevice(string code, string name, string type)
    {
        DeviceCode = code;
        DeviceName = name;
        DeviceType = type;
    }

    public string DisplayName => $"{DeviceName} ({DeviceCode})";
}
