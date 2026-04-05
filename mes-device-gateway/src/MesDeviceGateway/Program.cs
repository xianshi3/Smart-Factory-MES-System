using MesDeviceGateway.Config;
using MesDeviceGateway.Extensions;
using Serilog;
using Serilog.Events;

Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .MinimumLevel.Override("Microsoft", LogEventLevel.Information)
    .Enrich.FromLogContext()
    .Enrich.WithProperty("Application", "MesDeviceGateway")
    .WriteTo.Console(outputTemplate: "[{Timestamp:HH:mm:ss} {Level:u3}] {Message:lj}{NewLine}{Exception}")
    .CreateBootstrapLogger();

try
{
    Log.Information("Starting MES Device Gateway...");

    IHost host = Host.CreateDefaultBuilder(args)
        .UseSerilog()
        .ConfigureServices((context, services) =>
        {
            services.AddGatewayServices(context.Configuration);
            
            // 健康检查
            services.AddHealthChecks()
                .AddCheck("self", () => HealthCheckResult.Healthy("Gateway is running"));
        })
        .ConfigureAppConfiguration((context, config) =>
        {
            config.AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);
            config.AddEnvironmentVariables("MES_");
        })
        .Build();

    var config = host.Services.GetRequiredService<GatewayConfig>();
    Log.Information("Gateway configuration loaded: MaxDevices={MaxDevices}, BatchSize={BatchSize}, ChannelBuffer={Buffer}",
        config.MaxDevices, config.BatchSize, config.ChannelBufferSize);

    await host.RunAsync();
    
    Log.Information("MES Device Gateway stopped");
}
catch (Exception ex)
{
    Log.Fatal(ex, "Application terminated unexpectedly");
    return 1;
}
finally
{
    await Log.CloseAndFlushAsync();
}

return 0;
