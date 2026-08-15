using MesDeviceGateway.Config;
using MesDeviceGateway.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.Extensions.Options;

namespace MesDeviceGateway.Extensions;

/// <summary>
/// 服务集合扩展方法 - 优化版
/// </summary>
public static class ServiceCollectionExtensions
{
    /// <summary>
    /// 添加网关服务到依赖注入容器
    /// </summary>
    public static IServiceCollection AddGatewayServices(this IServiceCollection services, IConfiguration configuration)
    {
        // 配置
        services.Configure<GatewayConfig>(configuration.GetSection("Gateway"));
        services.AddSingleton(resolver => resolver.GetRequiredService<IOptions<GatewayConfig>>().Value);

        // 核心服务
        services.AddSingleton<DataCleanseService>();
        services.AddSingleton<DeviceHeartbeatService>();

        // Kafka 生产者：注册为单例（供 DataCleanseService 等注入）+ 宿主服务（复用同一实例）
        services.AddSingleton<KafkaProducerService>(provider =>
        {
            var config = provider.GetRequiredService<GatewayConfig>();
            var logger = provider.GetRequiredService<ILogger<KafkaProducerService>>();
            return new KafkaProducerService(config, logger);
        });
        services.AddHostedService(provider => provider.GetRequiredService<KafkaProducerService>());

        // MQTT消费者
        services.AddHostedService<MqttConsumerService>();

        return services;
    }

    /// <summary>
    /// 添加健康检查
    /// </summary>
    public static IHealthChecksBuilder AddGatewayHealthChecks(this IServiceCollection services, IConfiguration configuration)
    {
        var builder = services.AddHealthChecks();

        return builder;
    }
}
