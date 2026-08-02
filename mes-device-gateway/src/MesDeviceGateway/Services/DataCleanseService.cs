using System.Collections.Concurrent;
using MesDeviceGateway.Config;
using MesDeviceGateway.Models;
using Microsoft.Extensions.Logging;

namespace MesDeviceGateway.Services;

/// <summary>
/// 数据清洗服务，用于处理、去重和过滤设备数据消息
/// </summary>
public class DataCleanseService
{
    private readonly KafkaProducerService _kafkaProducer;
    private readonly DeviceHeartbeatService _heartbeat;
    private readonly GatewayConfig _config;
    private readonly ILogger<DataCleanseService> _logger;

    private readonly ConcurrentDictionary<string, string> _dedupCache = new();
    private readonly ConcurrentDictionary<string, List<DeviceDataMessage>> _slidingWindow = new();
    private readonly Timer? _windowTimer;

    /// <summary>
    /// 初始化数据清洗服务
    /// </summary>
    /// <param name="kafkaProducer">Kafka生产者服务</param>
    /// <param name="config">网关配置</param>
    /// <param name="logger">日志记录器</param>
    public DataCleanseService(
        KafkaProducerService kafkaProducer,
        DeviceHeartbeatService heartbeat,
        GatewayConfig config,
        ILogger<DataCleanseService> logger)
    {
        _kafkaProducer = kafkaProducer;
        _heartbeat = heartbeat;
        _config = config;
        _logger = logger;

        _windowTimer = new Timer(AggregateSlidingWindow, null,
            TimeSpan.FromSeconds(_config.SlidingWindowSeconds),
            TimeSpan.FromSeconds(_config.SlidingWindowSeconds));
    }

    /// <summary>
    /// 处理设备数据消息，包括验证、去重、过滤和发送到Kafka
    /// </summary>
    /// <param name="message">设备数据消息</param>
    /// <returns>异步任务</returns>
    public async Task ProcessDataMessageAsync(DeviceDataMessage message)
    {
        if (!ValidateMessage(message))
        {
            _logger.LogWarning("Invalid data message from device {DeviceId}", message.DeviceId);
            return;
        }

        var dedupKey = $"{message.DeviceId}_{message.Timestamp:yyyyMMddHHmmssfff}_{GetHashCode(message.Data)}";
        if (!_dedupCache.TryAdd(dedupKey, dedupKey))
        {
            _logger.LogDebug("Duplicate message ignored: {DedupKey}", dedupKey);
            return;
        }

        // 记录设备在线心跳（Redis不可用时静默降级）
        await _heartbeat.RecordHeartbeatAsync(message.DeviceId);

        message.Data = FilterAbnormalValues(message.Data);

        _slidingWindow.AddOrUpdate(message.DeviceId,
            _ => new List<DeviceDataMessage> { message },
            (_, list) => { lock (list) { list.Add(message); return list; } });

        var kafkaMsg = new KafkaMessage
        {
            Topic = "mes-device-data",
            Key = message.DeviceId,
            Value = message,
            Timestamp = DateTime.UtcNow
        };

        await _kafkaProducer.ProduceAsync(kafkaMsg);
    }

    /// <summary>
    /// 处理设备状态消息
    /// </summary>
    /// <param name="message">设备状态消息</param>
    /// <returns>异步任务</returns>
    public async Task ProcessStatusMessageAsync(DeviceStatusMessage message)
    {
        if (string.IsNullOrWhiteSpace(message.DeviceId) || string.IsNullOrWhiteSpace(message.Status))
        {
            _logger.LogWarning("Invalid status message");
            return;
        }

        // 状态消息同样刷新在线心跳
        await _heartbeat.RecordHeartbeatAsync(message.DeviceId, message.Status);

        var kafkaMsg = new KafkaMessage
        {
            Topic = "mes-alarm-event",
            Key = message.DeviceId,
            Value = message,
            Timestamp = DateTime.UtcNow
        };

        await _kafkaProducer.ProduceAsync(kafkaMsg);
        _logger.LogInformation("Processed status for device {DeviceId}: {Status}", message.DeviceId, message.Status);
    }

    private bool ValidateMessage(DeviceDataMessage message)
    {
        if (string.IsNullOrWhiteSpace(message.DeviceId)) return false;
        if (message.Data == null || message.Data.Count == 0) return false;
        if (message.Timestamp == default) return false;
        return true;
    }

    private Dictionary<string, object> FilterAbnormalValues(Dictionary<string, object> data)
    {
        var filtered = new Dictionary<string, object>();
        foreach (var kvp in data)
        {
            if (kvp.Value != null)
            {
                filtered[kvp.Key] = kvp.Value;
            }
        }
        return filtered;
    }

    private int GetHashCode(Dictionary<string, object> data)
    {
        unchecked
        {
            int hash = 17;
            foreach (var kvp in data.OrderBy(x => x.Key))
            {
                hash = hash * 31 + kvp.Key.GetHashCode();
                hash = hash * 31 + (kvp.Value?.GetHashCode() ?? 0);
            }
            return hash;
        }
    }

    private void AggregateSlidingWindow(object? state)
    {
        foreach (var kvp in _slidingWindow)
        {
            List<DeviceDataMessage> messages;
            lock (kvp.Value)
            {
                messages = new List<DeviceDataMessage>(kvp.Value);
                kvp.Value.Clear();
            }

            if (messages.Count > 0)
            {
                _logger.LogInformation("Sliding window aggregation for device {DeviceId}: {Count} messages aggregated",
                    kvp.Key, messages.Count);
            }
        }

        CleanExpiredDedupEntries();
    }

    private void CleanExpiredDedupEntries()
    {
        var cutoff = DateTime.UtcNow.AddMinutes(-5);
        var keysToRemove = _dedupCache.Keys.Where(k =>
        {
            var parts = k.Split('_');
            if (parts.Length >= 2 && DateTime.TryParseExact(parts[1], "yyyyMMddHHmmssfff", null,
                    System.Globalization.DateTimeStyles.None, out var ts))
            {
                return ts.ToUniversalTime() < cutoff;
            }
            return true;
        }).ToList();

        foreach (var key in keysToRemove)
        {
            _dedupCache.TryRemove(key, out _);
        }
    }
}
