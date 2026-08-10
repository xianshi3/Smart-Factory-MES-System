namespace MESDeviceSimulator;

/// <summary>
/// 模拟场景预设：一键切换设备运行态势
/// </summary>
public class SimulationScenario
{
    public string Name { get; set; } = "";
    public double BaseTemp { get; set; }        // 温度基准
    public double TempJitter { get; set; }      // 温度波动幅度
    public double BaseSpeed { get; set; }       // 转速基准
    public double SpeedJitter { get; set; }     // 转速波动幅度
    public double AlarmRate { get; set; }       // 故障概率 (0~1)
    public string DefaultStatus { get; set; } = "ONLINE";
    public bool HighTemp { get; set; }          // 是否高温场景（发送时叠加）

    public static List<SimulationScenario> Presets => new()
    {
        new SimulationScenario { Name = "正常运行", BaseTemp = 45, TempJitter = 4, BaseSpeed = 1200, SpeedJitter = 120, AlarmRate = 0.02, DefaultStatus = "ONLINE" },
        new SimulationScenario { Name = "满载生产", BaseTemp = 62, TempJitter = 6, BaseSpeed = 1800, SpeedJitter = 100, AlarmRate = 0.03, DefaultStatus = "ONLINE" },
        new SimulationScenario { Name = "高温告警", BaseTemp = 78, TempJitter = 8, BaseSpeed = 1500, SpeedJitter = 150, AlarmRate = 0.35, DefaultStatus = "ALARM" },
        new SimulationScenario { Name = "突发故障", BaseTemp = 55, TempJitter = 10, BaseSpeed = 300, SpeedJitter = 200, AlarmRate = 0.8, DefaultStatus = "OFFLINE" },
        new SimulationScenario { Name = "维护停机", BaseTemp = 30, TempJitter = 2, BaseSpeed = 0, SpeedJitter = 0, AlarmRate = 0.0, DefaultStatus = "MAINTENANCE" },
    };
}
