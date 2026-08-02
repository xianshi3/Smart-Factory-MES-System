using MesDeviceGateway.Config;
using Microsoft.Extensions.Logging;
using StackExchange.Redis;

namespace MesDeviceGateway.Services;

/// <summary>
/// 设备在线心跳存储服务
/// 将设备心跳写入Redis（key: device:online:{deviceId}），
/// Redis不可用时自动降级，不影响网关主流程。
/// </summary>
public class DeviceHeartbeatService
{
    private const string HeartbeatKeyPrefix = "device:online:";

    private readonly ILogger<DeviceHeartbeatService> _logger;
    private readonly IConnectionMultiplexer? _redis;
    private readonly int _ttlSeconds;

    /// <summary>
    /// Redis是否可用
    /// </summary>
    public bool Enabled => _redis != null && _redis.IsConnected;

    /// <summary>
    /// 初始化设备心跳服务（Redis连接失败时自动降级）
    /// </summary>
    public DeviceHeartbeatService(GatewayConfig config, ILogger<DeviceHeartbeatService> logger)
    {
        _logger = logger;
        _ttlSeconds = config.DeviceHeartbeatTtlSeconds;

        if (string.IsNullOrWhiteSpace(config.RedisServer))
        {
            _logger.LogWarning("Redis server not configured, device heartbeat disabled");
            _redis = null;
            return;
        }

        try
        {
            var options = new ConfigurationOptions
            {
                EndPoints = { $"{config.RedisServer}:{config.RedisPort}" },
                Password = config.RedisPassword,
                AbortOnConnectFail = false,
                ConnectRetry = 1,
                ConnectTimeout = 1000,
                SyncTimeout = 1000,
                AsyncTimeout = 1000
            };
            _redis = ConnectionMultiplexer.Connect(options);
            _redis.ConnectionFailed += (_, e) =>
                _logger.LogWarning("Redis connection failed: {EndPoint}", e.EndPoint);
            _redis.ConnectionRestored += (_, _) =>
                _logger.LogInformation("Redis connection restored");
            _logger.LogInformation("Device heartbeat Redis initialized at {Server}:{Port}",
                config.RedisServer, config.RedisPort);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to connect to Redis, device heartbeat disabled (graceful degradation)");
            _redis = null;
        }
    }

    /// <summary>
    /// 记录设备心跳（Redis不可用时静默降级）
    /// </summary>
    public async Task RecordHeartbeatAsync(string deviceId, string? status = null)
    {
        if (!Enabled)
        {
            return;
        }

        try
        {
            var db = _redis!.GetDatabase();
            var value = status is null
                ? DateTime.UtcNow.ToString("O")
                : $"{DateTime.UtcNow:O}|{status}";
            await db.StringSetAsync(HeartbeatKeyPrefix + deviceId, value,
                TimeSpan.FromSeconds(_ttlSeconds));
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Failed to record heartbeat for device {DeviceId}", deviceId);
        }
    }
}
