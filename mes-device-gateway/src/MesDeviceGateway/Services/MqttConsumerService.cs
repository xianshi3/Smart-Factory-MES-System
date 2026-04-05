using System.Text;
using System.Text.Json;
using System.Threading.Channels;
using MesDeviceGateway.Config;
using MesDeviceGateway.Models;
using MesDeviceGateway.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace MesDeviceGateway.Services;

/// <summary>
/// MQTT消费者服务 - 优化版
/// 支持自动重连、消息Channel处理、错误恢复
/// </summary>
public class MqttConsumerService : BackgroundService
{
    private readonly GatewayConfig _config;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<MqttConsumerService> _logger;
    private readonly Channel<MqttApplicationMessage> _messageChannel;
    private IMqttClient? _mqttClient;
    private bool _isConnected;

    public MqttConsumerService(
        GatewayConfig config,
        IServiceScopeFactory scopeFactory,
        ILogger<MqttConsumerService> logger)
    {
        _config = config;
        _scopeFactory = scopeFactory;
        _logger = logger;

        _messageChannel = Channel.CreateBounded<MqttApplicationMessage>(
            new BoundedChannelOptions(config.ChannelBufferSize / 10)
            {
                FullMode = BoundedChannelFullMode.DropOldest
            });
    }

    /// <summary>
    /// 启动服务
    /// </summary>
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("MQTT Consumer background task started");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                if (!_isConnected || _mqttClient?.IsConnected != true)
                {
                    await ConnectAsync(stoppingToken);
                }

                await foreach (var message in _messageChannel.Reader.ReadAllAsync(stoppingToken))
                {
                    await ProcessMessageAsync(message, stoppingToken);
                }
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in MQTT consumer loop");
                await Task.Delay(_config.MqttReconnectIntervalMs, stoppingToken);
            }
        }
    }

    /// <summary>
    /// 连接到MQTT服务器
    /// </summary>
    private async Task ConnectAsync(CancellationToken cancellationToken)
    {
        var factory = new MqttFactory();
        _mqttClient = factory.CreateMqttClient();

        var options = new MqttClientOptionsBuilder()
            .WithTcpServer(_config.MqttServer, _config.MqttPort)
            .WithCredentials(_config.MqttUsername, _config.MqttPassword)
            .WithClientId($"mes-gateway-{Environment.MachineName}-{Guid.NewGuid():N[..8]}")
            .WithCleanSession()
            .WithTimeout(TimeSpan.FromSeconds(30))
            .Build();

        _mqttClient.ApplicationMessageReceivedAsync += OnMessageReceived;
        _mqttClient.DisconnectedAsync += OnDisconnected;

        await _mqttClient.ConnectAsync(options, cancellationToken);
        _isConnected = true;

        _logger.LogInformation("Connected to MQTT Broker at {Server}:{Port}", _config.MqttServer, _config.MqttPort);

        var subscribeOptions = new MqttClientSubscribeOptionsBuilder()
            .WithTopicFilter("mes/device/+/data", MqttQualityOfServiceLevel.AtLeastOnce)
            .WithTopicFilter("mes/device/+/status", MqttQualityOfServiceLevel.AtLeastOnce)
            .Build();

        await _mqttClient.SubscribeAsync(subscribeOptions, cancellationToken);
        _logger.LogInformation("Subscribed to topics: mes/device/+/data, mes/device/+/status");
    }

    /// <summary>
    /// 断开连接处理
    /// </summary>
    private async Task OnDisconnected(MqttClientDisconnectedEventArgs e)
    {
        _isConnected = false;
        _logger.LogWarning("Disconnected from MQTT Broker: {Reason}", e.Reason);
        
        await Task.Delay(_config.MqttReconnectIntervalMs);
    }

    /// <summary>
    /// 消息接收处理 - 写入Channel
    /// </summary>
    private Task OnMessageReceived(MqttApplicationMessageReceivedEventArgs arg)
    {
        if (!_messageChannel.Writer.TryWrite(arg.ApplicationMessage))
        {
            _logger.LogWarning("Message channel is full, dropping message");
        }
        return Task.CompletedTask;
    }

    /// <summary>
    /// 处理MQTT消息
    /// </summary>
    private async Task ProcessMessageAsync(MqttApplicationMessage message, CancellationToken cancellationToken)
    {
        try
        {
            var topic = message.Topic;
            var payload = Encoding.UTF8.GetString(message.PayloadSegment);

            using var scope = _scopeFactory.CreateScope();
            var cleanseService = scope.ServiceProvider.GetRequiredService<DataCleanseService>();

            var segments = topic.Split('/');
            if (segments.Length >= 4)
            {
                var deviceId = segments[2];
                var messageType = segments[3];

                if (messageType == "data")
                {
                    var dataMsg = JsonSerializer.Deserialize<DeviceDataMessage>(payload);
                    if (dataMsg != null)
                    {
                        dataMsg.DeviceId = deviceId;
                        await cleanseService.ProcessDataMessageAsync(dataMsg);
                    }
                }
                else if (messageType == "status")
                {
                    var statusMsg = JsonSerializer.Deserialize<DeviceStatusMessage>(payload);
                    if (statusMsg != null)
                    {
                        statusMsg.DeviceId = deviceId;
                        await cleanseService.ProcessStatusMessageAsync(statusMsg);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing MQTT message");
        }
    }

    /// <summary>
    /// 停止服务
    /// </summary>
    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("MQTT Consumer stopping...");

        if (_mqttClient?.IsConnected == true)
        {
            await _mqttClient.DisconnectAsync(null, cancellationToken);
        }

        _messageChannel.Writer.Complete();
        _mqttClient?.Dispose();

        await base.StopAsync(cancellationToken);
    }
}
