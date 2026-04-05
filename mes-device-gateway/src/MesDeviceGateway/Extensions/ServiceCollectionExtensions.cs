using MesDeviceGateway.Config;
using MesDeviceGateway.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace MesDeviceGateway.Extensions;

/// <summary>
/// 服务集合扩展方法
/// </summary>
public static class ServiceCollectionExtensions
{
    /// <summary>
    /// 添加网关服务到依赖注入容器
    /// </summary>
    /// <param name="services">服务集合</param>
    /// <param name="configuration">配置</param>
    /// <returns>服务集合</returns>
    public static IServiceCollection AddGatewayServices(this IServiceCollection services, IConfiguration configuration)
    {
        services.Configure<GatewayConfig>(configuration.GetSection("Gateway"));
        services.AddSingleton(resolver => resolver.GetRequiredService<IOptions<GatewayConfig>>().Value);

        services.AddSingleton<KafkaProducerService>();
        services.AddSingleton<DataCleanseService>();
        services.AddHostedService<MqttConsumerService>();

        return services;
    }
}
