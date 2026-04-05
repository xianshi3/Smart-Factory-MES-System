using System.Threading.Channels;
using System.Text.Json;
using Confluent.Kafka;
using MesDeviceGateway.Config;
using MesDeviceGateway.Models;
using Microsoft.Extensions.Logging;

namespace MesDeviceGateway.Services;

/// <summary>
/// Kafka生产者服务 - 优化版
/// 使用Channel实现高吞吐量消息传递
/// </summary>
public class KafkaProducerService : BackgroundService
{
    private readonly GatewayConfig _config;
    private readonly ILogger<KafkaProducerService> _logger;
    private readonly IProducer<string, string> _producer;
    private readonly Channel<KafkaMessage> _channel;
    private readonly string _topic;

    /// <summary>
    /// 初始化Kafka生产者服务
    /// </summary>
    public KafkaProducerService(GatewayConfig config, ILogger<KafkaProducerService> logger)
    {
        _config = config;
        _logger = logger;
        _topic = "mes-device-data";

        var producerConfig = new ProducerConfig
        {
            BootstrapServers = _config.KafkaBootstrapServers,
            Acks = Acks.Leader,
            LingerMs = 5,
            CompressionType = CompressionType.Snappy,
            EnableIdempotence = _config.EnableIdempotent,
            MaxInFlight = 5,
            RetryBackoffMs = 100,
            MessageTimeoutMs = 30000
        };

        _producer = new ProducerBuilder<string, string>(producerConfig)
            .SetErrorHandler((_, e) => _logger.LogError("Kafka error: {Error}", e.Reason))
            .Build();

        _channel = Channel.CreateBounded<KafkaMessage>(
            new BoundedChannelOptions(_config.ChannelBufferSize)
            {
                FullMode = BoundedChannelFullMode.Wait,
                SingleReader = true,
                SingleWriter = false
            });

        _logger.LogInformation("Kafka Producer initialized: BootstrapServers={Servers}, ChannelBuffer={Buffer}",
            _config.KafkaBootstrapServers, _config.ChannelBufferSize);
    }

    /// <summary>
    /// 生产消息到Kafka
    /// </summary>
    public async Task ProduceAsync(KafkaMessage message, CancellationToken cancellationToken = default)
    {
        await _channel.Writer.WriteAsync(message, cancellationToken);
    }

    /// <summary>
    /// 执行后台任务 - 消费Channel并发送到Kafka
    /// </summary>
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("Kafka Producer background task started");

        await foreach (var message in _channel.Reader.ReadAllAsync(stoppingToken))
        {
            try
            {
                var valueJson = JsonSerializer.Serialize(message.Value);
                
                var result = await _producer.ProduceAsync(_topic, new Message<string, string>
                {
                    Key = message.Key,
                    Value = valueJson
                }, stoppingToken);

                _logger.LogTrace("Message sent to [{Topic}] Partition:{Partition} Offset:{Offset}",
                    result.Topic, result.Partition, result.Offset);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to produce message: {Key}", message.Key);
            }
        }
    }

    /// <summary>
    /// 优雅停止
    /// </summary>
    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Kafka Producer stopping...");
        
        _channel.Writer.Complete();
        
        _producer.Flush(cancellationToken);
        _producer.Dispose();
        
        await base.StopAsync(cancellationToken);
    }
}
