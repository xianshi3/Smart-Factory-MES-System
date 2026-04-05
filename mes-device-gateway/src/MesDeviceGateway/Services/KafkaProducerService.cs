using System.Collections.Concurrent;
using System.Text.Json;
using Confluent.Kafka;
using MesDeviceGateway.Config;
using MesDeviceGateway.Models;
using Microsoft.Extensions.Logging;

namespace MesDeviceGateway.Services;

/// <summary>
/// Kafka生产者服务，用于将消息批量发送到Kafka
/// </summary>
public class KafkaProducerService : IDisposable
{
    private readonly GatewayConfig _config;
    private readonly ILogger<KafkaProducerService> _logger;
    private readonly IProducer<string, string> _producer;
    private readonly ConcurrentQueue<KafkaMessage> _messageQueue = new();
    private readonly Timer? _flushTimer;
    private int _currentBatchCount = 0;

    /// <summary>
    /// 初始化Kafka生产者服务
    /// </summary>
    /// <param name="config">网关配置</param>
    /// <param name="logger">日志记录器</param>
    public KafkaProducerService(GatewayConfig config, ILogger<KafkaProducerService> logger)
    {
        _config = config;
        _logger = logger;

        var producerConfig = new ProducerConfig
        {
            BootstrapServers = _config.KafkaBootstrapServers,
            Acks = Acks.All,
            LingerMs = 5,
            CompressionType = CompressionType.Snappy
        };

        var producerBuilder = new ProducerBuilder<string, string>(producerConfig);
        _producer = producerBuilder.Build();

        _flushTimer = new Timer(FlushBatch, null,
            TimeSpan.FromMilliseconds(_config.FlushIntervalMs),
            TimeSpan.FromMilliseconds(_config.FlushIntervalMs));

        _logger.LogInformation("Kafka Producer initialized with BootstrapServers: {Servers}", _config.KafkaBootstrapServers);
    }

    /// <summary>
    /// 生产Kafka消息，加入队列并在达到批处理大小时刷新
    /// </summary>
    /// <param name="message">Kafka消息</param>
    /// <returns>异步任务</returns>
    public async Task ProduceAsync(KafkaMessage message)
    {
        _messageQueue.Enqueue(message);
        Interlocked.Increment(ref _currentBatchCount);

        if (_currentBatchCount >= _config.BatchSize)
        {
            await FlushBatchAsync();
        }
    }

    private void FlushBatch(object? state)
    {
        FlushBatchAsync().GetAwaiter().GetResult();
    }

    private async Task FlushBatchAsync()
    {
        if (_messageQueue.IsEmpty) return;

        var batch = new List<KafkaMessage>();
        while (_messageQueue.TryDequeue(out var msg))
        {
            batch.Add(msg);
        }

        Interlocked.Exchange(ref _currentBatchCount, 0);

        if (batch.Count == 0) return;

        try
        {
            var tasks = batch.Select(msg => ProduceSingleAsync(msg)).ToArray();
            await Task.WhenAll(tasks);

            _logger.LogDebug("Flushed {Count} messages to Kafka", batch.Count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error flushing batch to Kafka");
        }
    }

    private async Task ProduceSingleAsync(KafkaMessage message)
    {
        try
        {
            var valueJson = JsonSerializer.Serialize(message.Value);

            var kafkaMessage = new Message<string, string>
            {
                Key = message.Key,
                Value = valueJson,
                Headers = new Headers
                {
                    { "timestamp", BitConverter.GetBytes(message.Timestamp.Ticks) }
                }
            };

            var result = await _producer.ProduceAsync(message.Topic, kafkaMessage);
            _logger.LogDebug("Message sent to {Topic} [Partition:{Partition} Offset:{Offset}]",
                result.Topic, result.Partition, result.Offset);
        }
        catch (ProduceException<string, string> ex)
        {
            _logger.LogError(ex, "Kafka produce error for topic {Topic}: {Error}",
                message.Topic, ex.Error.Reason);
            throw;
        }
    }

    /// <summary>
    /// 释放Kafka生产者资源
    /// </summary>
    public void Dispose()
    {
        _flushTimer?.Dispose();
        FlushBatchAsync().GetAwaiter().GetResult();
        _producer.Dispose();
        _logger.LogInformation("Kafka Producer disposed");
    }
}
