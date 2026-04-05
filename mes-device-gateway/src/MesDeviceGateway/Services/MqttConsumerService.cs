using System.Text;
using System.Text.Json;
using MesDeviceGateway.Config;
using MesDeviceGateway.Models;
using MesDeviceGateway.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace MesDeviceGateway.Services;

/// <summary>
/// MQTT消费者服务，用于从MQTT代理消费设备消息
/// </summary>
public class MqttConsumerService : BackgroundService
{
    private readonly GatewayConfig _config;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<MqttConsumerService> _logger;
    private IMqttClient? _mqttClient;

    /// <summary>
    /// 初始化MQTT消费者服务
    /// </summary>
    /// <param name="config">网关配置</param>
    /// <param name="scopeFactory">服务作用域工厂</param>
    /// <param name="logger">日志记录器</param>
    public MqttConsumerService(
        GatewayConfig config,
        IServiceScopeFactory scopeFactory,
        ILogger<MqttConsumerService> logger)
    {
        _config = config;
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    /// <summary>
    /// 启动MQTT消费者服务，连接到MQTT代理并订阅主题
    /// </summary>
    /// <param name="cancellationToken">取消令牌</param>
    /// <returns>异步任务</returns>
    public override async Task StartAsync(CancellationToken cancellationToken)
    {
        var factory = new MqttFactory();
        _mqttClient = factory.CreateMqttClient();

        var options = new MqttClientOptionsBuilder()
            .WithTcpServer(_config.MqttServer, _config.MqttPort)
            .WithCredentials(_config.MqttUsername, _config.MqttPassword)
            .WithClientId($"mes-gateway-{Guid.NewGuid():N}")
            .WithCleanSession()
            .Build();

        _mqttClient.ApplicationMessageReceivedAsync += OnMessageReceived;

        try
        {
            await _mqttClient.ConnectAsync(options, cancellationToken);
            _logger.LogInformation("Connected to MQTT Broker at {Server}:{Port}", _config.MqttServer, _config.MqttPort);

            var subscribeOptions = new MqttClientSubscribeOptionsBuilder()
                .WithTopicFilter("mes/device/+/data", MqttQualityOfServiceLevel.AtLeastOnce)
                .WithTopicFilter("mes/device/+/status", MqttQualityOfServiceLevel.AtLeastOnce)
                .Build();

            await _mqttClient.SubscribeAsync(subscribeOptions, cancellationToken);
            _logger.LogInformation("Subscribed to topics: mes/device/+/data, mes/device/+/status");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to connect to MQTT Broker");
            throw;
        }

        await base.StartAsync(cancellationToken);
    }

    /// <summary>
    /// 处理收到的MQTT消息
    /// </summary>
    /// <param name="arg">MQTT应用消息事件参数</param>
    /// <returns>异步任务</returns>
    private async Task OnMessageReceived(MqttApplicationMessageReceivedEventArgs arg)
    {
        try
        {
            var topic = arg.ApplicationMessage.Topic;
            var payload = Encoding.UTF8.GetString(arg.ApplicationMessage.PayloadSegment);

            _logger.LogDebug("Received message on topic {Topic}: {Payload}", topic, payload);

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
    /// 停止MQTT消费者服务
    /// </summary>
    /// <param name="cancellationToken">取消令牌</param>
    /// <returns>异步任务</returns>
    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        if (_mqttClient?.IsConnected == true)
        {
            await _mqttClient.DisconnectAsync(cancellationToken: cancellationToken);
            _logger.LogInformation("Disconnected from MQTT Broker");
        }

        await base.StopAsync(cancellationToken);
    }

    /// <summary>
    /// 执行后台任务，保持服务运行
    /// </summary>
    /// <param name="stoppingToken">停止令牌</param>
    /// <returns>异步任务</returns>
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            await Task.Delay(1000, stoppingToken);
        }
    }
}
