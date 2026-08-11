using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace MESDeviceSimulator;

/// <summary>
/// 模拟设备（支持批量模拟：每台设备独立参数 + 个体随机偏移 + UI实时刷新）
/// </summary>
public class SimulatedDevice : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    private void Notify([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

    private string _deviceCode = "";
    public string DeviceCode
    {
        get => _deviceCode;
        set { if (_deviceCode != value) { _deviceCode = value; Notify(); } }
    }

    private string _deviceName = "";
    public string DeviceName
    {
        get => _deviceName;
        set { if (_deviceName != value) { _deviceName = value; Notify(); } }
    }

    private string _deviceType = "CNC";
    public string DeviceType
    {
        get => _deviceType;
        set { if (_deviceType != value) { _deviceType = value; Notify(); } }
    }

    private string _status = "ONLINE";
    public string Status
    {
        get => _status;
        set { if (_status != value) { _status = value; Notify(); } }
    }

    private double _temperature;
    public double Temperature
    {
        get => _temperature;
        set { if (Math.Abs(_temperature - value) > 0.001) { _temperature = value; Notify(); } }
    }

    private double _speed;
    public double Speed
    {
        get => _speed;
        set { if (Math.Abs(_speed - value) > 0.001) { _speed = value; Notify(); } }
    }

    private double _pressure;
    public double Pressure
    {
        get => _pressure;
        set { if (Math.Abs(_pressure - value) > 0.001) { _pressure = value; Notify(); } }
    }

    private double _power;
    public double Power
    {
        get => _power;
        set { if (Math.Abs(_power - value) > 0.001) { _power = value; Notify(); } }
    }

    /// <summary>个体随机偏移（批量模拟时每台设备数值略有差异）</summary>
    public double TempOffset { get; set; }
    public double SpeedOffset { get; set; }

    /// <summary>是否已在后端创建</summary>
    public bool Created { get; set; }

    private bool _selected = true;
    /// <summary>是否参与模拟（列表复选框）</summary>
    public bool Selected
    {
        get => _selected;
        set { if (_selected != value) { _selected = value; Notify(); } }
    }

    public SimulatedDevice() { }

    public SimulatedDevice(string code, string name, string type)
    {
        DeviceCode = code;
        DeviceName = name;
        DeviceType = type;
    }

    public string DisplayName => $"{DeviceName} ({DeviceCode})";
}
