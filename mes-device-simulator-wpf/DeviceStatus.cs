using Newtonsoft.Json;

namespace MESDeviceSimulator;

public class DeviceStatus
{
    [JsonProperty("id")]
    public long Id { get; set; }

    [JsonProperty("deviceCode")]
    public string DeviceCode { get; set; } = "";

    [JsonProperty("deviceName")]
    public string DeviceName { get; set; } = "";

    [JsonProperty("deviceType")]
    public string DeviceType { get; set; } = "";

    [JsonProperty("status")]
    public string Status { get; set; } = "";

    [JsonProperty("temperature")]
    public double Temperature { get; set; }

    [JsonProperty("speed")]
    public double Speed { get; set; }
}