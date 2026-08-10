using System.Net.Http;
using System.Net.Http.Json;
using System.Text;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Shapes;
using System.Windows.Threading;
using MQTTnet;
using MQTTnet.Client;
using Newtonsoft.Json;

namespace MESDeviceSimulator;

public partial class MainWindow : Window
{
    private readonly HttpClient _httpClient = new();
    private readonly DispatcherTimer _simulationTimer;
    private readonly DispatcherTimer _clockTimer;
    private readonly Random _random = new();
    private long _messageCount = 0;
    private long _lastMessageCount = 0;
    private bool _isSimulating = false;
    private bool _isConnected = false;
    private bool _isDarkTheme = true;
    private readonly DispatcherTimer _sendRateTimer;

    private IMqttClient? _mqttClient;
    private bool _mqttConnected = false;
    private int _mqttReconnectAttempts = 0;
    private bool _isDisconnecting = false;

    // 批量模拟设备集合
    private readonly List<SimulatedDevice> _devices = new();
    private SimulationScenario _scenario = SimulationScenario.Presets[0];

    // 曲线图数据（温度/转速历史）
    private readonly List<double> _chartTemps = new();
    private readonly List<double> _chartSpeeds = new();
    private const int CHART_MAX_POINTS = 120;

    private double _temperature = 45.0;
    private double _speed = 1200;
    private double _pressure = 1.5;
    private double _power = 50;
    private string _status = "ONLINE";

    // Light theme colors
    private readonly SolidColorBrush lightBg = new((Color)ColorConverter.ConvertFromString("#F8FAFC"));
    private readonly SolidColorBrush lightCard = new((Color)ColorConverter.ConvertFromString("#FFFFFF"));
    private readonly SolidColorBrush lightPanel = new((Color)ColorConverter.ConvertFromString("#F1F5F9"));
    private readonly SolidColorBrush lightText = new((Color)ColorConverter.ConvertFromString("#1E293B"));
    private readonly SolidColorBrush lightMuted = new((Color)ColorConverter.ConvertFromString("#64748B"));
    private readonly SolidColorBrush lightBorder = new((Color)ColorConverter.ConvertFromString("#E2E8F0"));

    // Dark theme colors
    private readonly SolidColorBrush darkBg = new((Color)ColorConverter.ConvertFromString("#0F172A"));
    private readonly SolidColorBrush darkCard = new((Color)ColorConverter.ConvertFromString("#1E293B"));
    private readonly SolidColorBrush darkPanel = new((Color)ColorConverter.ConvertFromString("#334155"));
    private readonly SolidColorBrush darkText = new((Color)ColorConverter.ConvertFromString("#F1F5F9"));
    private readonly SolidColorBrush darkMuted = new((Color)ColorConverter.ConvertFromString("#94A3B8"));
    private readonly SolidColorBrush darkBorder = new((Color)ColorConverter.ConvertFromString("#475569"));

    public MainWindow()
    {
        InitializeComponent();

        _simulationTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(2000) };
        _simulationTimer.Tick += SimulationTimer_Tick;

        _sendRateTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _sendRateTimer.Tick += SendRateTimer_Tick;
        _sendRateTimer.Start();

        // 上位机时钟
        _clockTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _clockTimer.Tick += (_, _) => clockText.Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        _clockTimer.Start();
        clockText.Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        ApplyDarkTheme();

        // 恢复上次配置（API/MQTT地址、参数、频率、场景）
        LoadConfig();

        // 启动时自动探测 API 网关与 EMQX 地址
        _ = AutoDetectServicesAsync();
    }

    /// <summary>
    /// 自动探测本地已运行的服务并填入配置（API 网关 9090 &gt; 看板 8085，EMQX 1883）
    /// </summary>
    private async Task AutoDetectServicesAsync()
    {
        // API 网关优先，其次看板服务
        foreach (var port in new[] { 9090, 8085 })
        {
            if (await IsPortOpenAsync("localhost", port, 800))
            {
                Dispatcher.Invoke(() => apiServerInput.Text = $"http://localhost:{port}");
                break;
            }
        }

        // EMQX MQTT Broker
        if (await IsPortOpenAsync("localhost", 1883, 800))
        {
            Dispatcher.Invoke(() =>
            {
                mqttServerInput.Text = "localhost";
                mqttPortInput.Text = "1883";
            });
        }
    }

    private static async Task<bool> IsPortOpenAsync(string host, int port, int timeoutMs)
    {
        try
        {
            using var client = new System.Net.Sockets.TcpClient();
            var task = client.ConnectAsync(host, port);
            var completed = await Task.WhenAny(task, Task.Delay(timeoutMs));
            if (completed != task) return false;
            return client.Connected;
        }
        catch
        {
            return false;
        }
    }

    private void BtnTheme_Click(object sender, RoutedEventArgs e)
    {
        _isDarkTheme = !_isDarkTheme;

        if (_isDarkTheme)
        {
            ApplyDarkTheme();
            btnTheme.Content = "☀️ 亮色模式";
        }
        else
        {
            ApplyLightTheme();
            btnTheme.Content = "🌙 暗色模式";
        }
    }

    private void ApplyDarkTheme()
    {
        Background = darkBg;
        
        // Top
        subtitleText.Foreground = darkText;
        descText.Foreground = darkMuted;
        apiStatusText.Foreground = darkMuted;
        mqttStatusText.Foreground = darkMuted;
        clockText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#38BDF8"));
        
        // Cards
        configCard.Background = darkCard;
        paramCard.Background = darkCard;
        batchCard.Background = darkCard;
        deviceCard.Background = darkCard;
        paramControlCard.Background = darkCard;
        lblApiAddress.Foreground = darkMuted;
        lblMqttServer.Foreground = darkMuted;
        lblDeviceId.Foreground = darkMuted;
        lblDeviceName.Foreground = darkMuted;
        lblDeviceType.Foreground = darkMuted;
        lblStatus.Foreground = darkMuted;
        titleText.Foreground = darkText;

        // Inputs
        apiServerInput.Background = darkPanel;
        apiServerInput.Foreground = darkText;
        apiServerInput.BorderBrush = darkBorder;
        mqttServerInput.Background = darkPanel;
        mqttServerInput.Foreground = darkText;
        mqttServerInput.BorderBrush = darkBorder;
        mqttPortInput.Background = darkPanel;
        mqttPortInput.Foreground = darkText;
        mqttPortInput.BorderBrush = darkBorder;
        devIdInput.Background = darkPanel;
        devIdInput.Foreground = darkText;
        devIdInput.BorderBrush = darkBorder;
        devNameInput.Background = darkPanel;
        devNameInput.Foreground = darkText;
        devNameInput.BorderBrush = darkBorder;
        batchCountInput.Background = darkPanel;
        batchCountInput.Foreground = darkText;
        batchCountInput.BorderBrush = darkBorder;
        
        // Param panels
        bgTemp.Background = darkPanel;
        bgSpeed.Background = darkPanel;
        bgPressure.Background = darkPanel;
        bgPower.Background = darkPanel;
        bgAuto.Background = darkPanel;
        
        lblTemp.Foreground = darkMuted;
        lblSpeed.Foreground = darkMuted;
        lblPressure.Foreground = darkMuted;
        lblPower.Foreground = darkMuted;
        lblAuto.Foreground = darkMuted;
        
        chkAutoTemperature.Foreground = darkText;
        chkAutoSpeed.Foreground = darkText;
        chkAutoStatus.Foreground = darkText;
        
        tempValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#F59E0B"));
        speedValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#06B6D4"));
        pressureValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#8B5CF6"));
        powerValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#34D399"));
        
        // Stats
        stat1.Background = darkCard;
        stat2.Background = darkCard;
        stat3.Background = darkCard;
        stat4.Background = darkCard;
        
        lblMsgCount.Foreground = darkMuted;
        lblSimStatus.Foreground = darkMuted;
        lblLastSend.Foreground = darkMuted;
        lblSendRate.Foreground = darkMuted;
        
        txtMessageCount.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#38BDF8"));
        txtSimulationStatus.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#F87171"));
        txtLastSend.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#34D399"));
        txtSendRate.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#A78BFA"));
        
        // Chart / Data
        chartCard.Background = darkCard;
        dataCard.Background = darkCard;
        dataTitle.Foreground = darkText;
        txtRealTimeData.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#4ADE80"));
        
        // Bottom
        bottomBar.Background = darkCard;
        apiEndpoint.Foreground = darkMuted;
        statusBar.Foreground = darkMuted;
    }

    private void ApplyLightTheme()
    {
        Background = lightBg;
        
        apiStatusText.Foreground = lightMuted;
        mqttStatusText.Foreground = lightMuted;
        
        subtitleText.Foreground = lightText;
        descText.Foreground = lightMuted;
        clockText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#0EA5E9"));
        
        configCard.Background = lightCard;
        paramCard.Background = lightCard;
        batchCard.Background = lightCard;
        deviceCard.Background = lightCard;
        paramControlCard.Background = lightCard;
        lblApiAddress.Foreground = lightMuted;
        lblMqttServer.Foreground = lightMuted;
        lblDeviceId.Foreground = lightMuted;
        lblDeviceName.Foreground = lightMuted;
        lblDeviceType.Foreground = lightMuted;
        lblStatus.Foreground = lightMuted;
        titleText.Foreground = lightText;

        apiServerInput.Background = lightCard;
        apiServerInput.Foreground = lightText;
        apiServerInput.BorderBrush = lightBorder;
        mqttServerInput.Background = lightCard;
        mqttServerInput.Foreground = lightText;
        mqttServerInput.BorderBrush = lightBorder;
        mqttPortInput.Background = lightCard;
        mqttPortInput.Foreground = lightText;
        mqttPortInput.BorderBrush = lightBorder;
        devIdInput.Background = lightCard;
        devIdInput.Foreground = lightText;
        devIdInput.BorderBrush = lightBorder;
        devNameInput.Background = lightCard;
        devNameInput.Foreground = lightText;
        devNameInput.BorderBrush = lightBorder;
        batchCountInput.Background = lightCard;
        batchCountInput.Foreground = lightText;
        batchCountInput.BorderBrush = lightBorder;
        
        bgTemp.Background = lightPanel;
        bgSpeed.Background = lightPanel;
        bgPressure.Background = lightPanel;
        bgPower.Background = lightPanel;
        bgAuto.Background = lightPanel;
        
        lblTemp.Foreground = lightMuted;
        lblSpeed.Foreground = lightMuted;
        lblPressure.Foreground = lightMuted;
        lblPower.Foreground = lightMuted;
        lblAuto.Foreground = lightMuted;
        
        chkAutoTemperature.Foreground = lightText;
        chkAutoSpeed.Foreground = lightText;
        chkAutoStatus.Foreground = lightText;
        
        tempValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#D97706"));
        speedValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#0891B2"));
        pressureValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#7C3AED"));
        powerValue.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#059669"));
        
        stat1.Background = lightCard;
        stat2.Background = lightCard;
        stat3.Background = lightCard;
        stat4.Background = lightCard;
        
        lblMsgCount.Foreground = lightMuted;
        lblSimStatus.Foreground = lightMuted;
        lblLastSend.Foreground = lightMuted;
        lblSendRate.Foreground = lightMuted;
        
        txtMessageCount.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#0284C7"));
        txtSimulationStatus.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#DC2626"));
        txtLastSend.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#059669"));
        txtSendRate.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#7C3AED"));
        
        chartCard.Background = lightCard;
        dataCard.Background = lightCard;
        dataTitle.Foreground = lightText;
        txtRealTimeData.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#059669"));
        
        bottomBar.Background = lightCard;
        apiEndpoint.Foreground = lightMuted;
        statusBar.Foreground = lightMuted;
    }

    private async void BtnConnect_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.GetAsync($"{apiBase}/api/dashboard/devices");

            if (response.IsSuccessStatusCode)
            {
                _isConnected = true;
                apiStatusText.Text = "API:已连";
                apiDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
                statusBar.Text = $"已连接到 {apiBase}";

                btnConnect.IsEnabled = false;
                btnDisconnect.IsEnabled = true;
                btnStartSimulation.IsEnabled = true;
                LoadDeviceList();

                // 连接 MQTT（失败不阻断，仅提示）

                await ConnectMqttAsync();
            }
            else
            {
                throw new HttpRequestException($"HTTP {(int)response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show($"连接失败: {ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            statusBar.Text = $"连接失败: {ex.Message}";
        }
    }

    /// <summary>
    /// 连接 EMQX MQTT Broker（网关订阅 mes/device/+/data），断开后自动重连
    /// </summary>
    private async Task ConnectMqttAsync()
    {
        try
        {
            var factory = new MqttFactory();
            _mqttClient = factory.CreateMqttClient();

            string host = mqttServerInput.Text.Trim();
            int port = int.TryParse(mqttPortInput.Text.Trim(), out var p) ? p : 1883;
            string deviceCode = devIdInput.Text.Trim();

            var options = new MqttClientOptionsBuilder()
                .WithTcpServer(host, port)
                .WithClientId($"mes-simulator-{deviceCode}-{Guid.NewGuid().ToString("N")[..8]}")
                .WithCleanSession()
                .WithTimeout(TimeSpan.FromSeconds(10))
                .Build();

            _mqttClient.ConnectedAsync += args =>
            {
                _mqttConnected = true;
                _mqttReconnectAttempts = 0;
                Dispatcher.Invoke(() =>
                {
                    mqttStatusText.Text = "MQTT:已连";
                    mqttDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
                    statusBar.Text = $"已连接 API + MQTT ({host}:{port})";
                });
                return Task.CompletedTask;
            };
            _mqttClient.DisconnectedAsync += args =>
            {
                _mqttConnected = false;
                Dispatcher.Invoke(() =>
                {
                    mqttStatusText.Text = "MQTT:断连";
                    mqttDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
                    statusBar.Text = "MQTT 已断开，模拟数据仅走HTTP";
                });
                // 自动重连（最多重试 5 次，间隔 5 秒）
                if (_isConnected && !_isDisconnecting)
                {
                    _ = Task.Run(async () =>
                    {
                        await Task.Delay(5000);
                        if (_isConnected && !_isDisconnecting && _mqttClient != null && !_mqttClient.IsConnected)
                        {
                            _mqttReconnectAttempts++;
                            if (_mqttReconnectAttempts <= 5)
                            {
                                Dispatcher.Invoke(() => statusBar.Text = $"MQTT 重连中 ({_mqttReconnectAttempts}/5)...");
                                try { await ConnectMqttAsync(); } catch { /* 下轮再试 */ }
                            }
                        }
                    });
                }
                return Task.CompletedTask;
            };

            await _mqttClient.ConnectAsync(options);
            _mqttConnected = _mqttClient.IsConnected;
            if (_mqttConnected)
            {
                mqttStatusText.Text = "MQTT:已连";
                mqttDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
                statusBar.Text = $"已连接 API + MQTT ({host}:{port})";
            }
        }
        catch (Exception ex)
        {
            _mqttConnected = false;
            mqttStatusText.Text = "MQTT:未连";
            mqttDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
            statusBar.Text = $"API已连接，MQTT连接失败: {ex.Message}（模拟数据仅走HTTP）";
        }
    }

    /// <summary>
    /// 通过 MQTT 发布遥测数据（topic: mes/device/{deviceCode}/data）
    /// </summary>
    private async Task<bool> PublishMqttTelemetryAsync(string deviceCode, string status,
        double? temperature = null, double? speed = null)
    {
        if (_mqttClient?.IsConnected != true) return false;

        try
        {
            var payload = new
            {
                timestamp = DateTime.UtcNow,
                dataType = "telemetry",
                status = status,
                data = new
                {
                    temperature = Math.Round(temperature ?? _temperature, 2),
                    speed = Math.Round(speed ?? _speed, 2),
                    pressure = Math.Round(_pressure, 2),
                    power = Math.Round(_power, 2)
                }
            };

            var message = new MqttApplicationMessageBuilder()
                .WithTopic($"mes/device/{deviceCode}/data")
                .WithPayload(JsonConvert.SerializeObject(payload))
                .WithQualityOfServiceLevel(MQTTnet.Protocol.MqttQualityOfServiceLevel.AtLeastOnce)
                .Build();

            await _mqttClient.PublishAsync(message);
            return true;
        }
        catch (Exception ex)
        {
            statusBar.Text = $"MQTT发布失败: {ex.Message}";
            return false;
        }
    }

    private async void LoadDeviceList()
    {
        try
        {
            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.GetAsync($"{apiBase}/api/dashboard/devices");
            
            if (response.IsSuccessStatusCode)
            {
                var json = await response.Content.ReadAsStringAsync();
                // 后端返回包装结构 {code, message, data:[...]}
                var wrapper = JsonConvert.DeserializeObject<dynamic>(json);
                var data = wrapper?.data as Newtonsoft.Json.Linq.JArray;
                var devices = data?.ToObject<List<DeviceStatus>>();
                if (devices != null)
                {
                    deviceListBox.ItemsSource = devices;
                    statusBar.Text = $"设备列表加载成功：{devices.Count} 台";
                }
            }
        }
        catch (Exception ex)
        {
            statusBar.Text = $"加载设备列表失败: {ex.Message}";
        }
    }

    private void DeviceListBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (deviceListBox.SelectedItem is DeviceStatus selected)
        {
            devIdInput.Text = selected.DeviceCode;
            devNameInput.Text = selected.DeviceName;
            
            for (int i = 0; i < cmbDeviceType.Items.Count; i++)
            {
                var item = cmbDeviceType.Items[i] as ComboBoxItem;
                if (item?.Content?.ToString() == selected.DeviceType)
                {
                    cmbDeviceType.SelectedIndex = i;
                    break;
                }
            }
        }
    }

    private async void BtnUpdate_Click(object sender, RoutedEventArgs e)
    {
        if (!_isConnected)
        {
            MessageBox.Show("请先连接API", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        try
        {
            string deviceCode = devIdInput.Text.Trim();
            string deviceName = devNameInput.Text.Trim();
            string deviceType = (cmbDeviceType.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "CNC";

            var deviceData = new
            {
                deviceCode = deviceCode,
                deviceName = deviceName,
                deviceType = deviceType
            };

            string json = JsonConvert.SerializeObject(deviceData);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.PutAsync($"{apiBase}/api/dashboard/device", content);

            if (response.IsSuccessStatusCode)
            {
                statusBar.Text = $"设备 {deviceCode} 更新成功";
                LoadDeviceList();
            }
            else
            {
                throw new HttpRequestException($"HTTP {(int)response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show($"更新设备失败: {ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            statusBar.Text = $"更新设备失败: {ex.Message}";
        }
    }

    private async void BtnCreate_Click(object sender, RoutedEventArgs e)
    {
        if (!_isConnected)
        {
            MessageBox.Show("请先连接API", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        try
        {
            string deviceCode = devIdInput.Text.Trim();
            string deviceName = devNameInput.Text.Trim();
            string deviceType = (cmbDeviceType.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "CNC";
            string status = (cmbStatus.SelectedItem as ComboBoxItem)?.Tag?.ToString() ?? "ONLINE";

            var deviceData = new
            {
                deviceCode = deviceCode,
                deviceName = deviceName,
                deviceType = deviceType,
                status = status,
                temperature = Math.Round(_temperature, 1),
                speed = _speed
            };

            string json = JsonConvert.SerializeObject(deviceData);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.PostAsync($"{apiBase}/api/dashboard/device", content);

            if (response.IsSuccessStatusCode)
            {
                statusBar.Text = $"设备 {deviceCode} 创建成功";
                MessageBox.Show($"设备 {deviceCode} 创建成功", "成功", MessageBoxButton.OK, MessageBoxImage.Information);
                LoadDeviceList();
            }
            else
            {
                throw new HttpRequestException($"HTTP {(int)response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show($"创建设备失败: {ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            statusBar.Text = $"创建设备失败: {ex.Message}";
        }
    }

    private async void BtnDelete_Click(object sender, RoutedEventArgs e)
    {
        if (!_isConnected)
        {
            MessageBox.Show("请先连接API", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        try
        {
            var result = MessageBox.Show($"确定要删除设备 {devIdInput.Text} 吗?", "确认删除", MessageBoxButton.YesNo, MessageBoxImage.Question);
            if (result != MessageBoxResult.Yes) return;

            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.DeleteAsync($"{apiBase}/api/dashboard/device/" + devIdInput.Text.Trim());

            if (response.IsSuccessStatusCode)
            {
                statusBar.Text = $"设备 {devIdInput.Text} 删除成功";
                MessageBox.Show($"设备 {devIdInput.Text} 删除成功", "成功", MessageBoxButton.OK, MessageBoxImage.Information);
                LoadDeviceList();
            }
            else
            {
                throw new HttpRequestException($"HTTP {(int)response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show($"删除设备失败: {ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            statusBar.Text = $"删除设备失败: {ex.Message}";
        }
    }

    private async void BtnDisconnect_Click(object sender, RoutedEventArgs e)
    {
        _isSimulating = false;
        _simulationTimer.Stop();
        _isDisconnecting = true;

        _isConnected = false;
        apiStatusText.Text = "API:未连";
        apiDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
        mqttStatusText.Text = "MQTT:未连";
        mqttDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
        statusBar.Text = "已断开连接";

        btnConnect.IsEnabled = true;
        btnDisconnect.IsEnabled = false;
        btnStartSimulation.IsEnabled = false;
        btnStartSimulation.Content = "开始模拟";
        txtSimulationStatus.Text = "停止";

        // 断开 MQTT
        if (_mqttClient != null)
        {
            try
            {
                if (_mqttClient.IsConnected)
                {
                    await _mqttClient.DisconnectAsync();
                }
                _mqttClient.Dispose();
            }
            catch { /* 忽略断开异常 */ }
            _mqttClient = null;
            _mqttConnected = false;
        }
        _isDisconnecting = false;
    }

    private void BtnStartSimulation_Click(object sender, RoutedEventArgs e)
    {
        if (_isSimulating)
        {
            _isSimulating = false;
            _simulationTimer.Stop();
            btnStartSimulation.Content = "开始模拟";
            txtSimulationStatus.Text = "停止";
            txtSimulationStatus.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
        }
        else
        {
            if (!_isConnected)
            {
                MessageBox.Show("请先连接后端API", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            _isSimulating = true;
            _simulationTimer.Start();
            btnStartSimulation.Content = "停止模拟";
            txtSimulationStatus.Text = "运行中";
            txtSimulationStatus.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
        }
    }

    private async void SimulationTimer_Tick(object? sender, EventArgs e)
    {
        if (!_isConnected) return;

        var scenario = _scenario;

        // 单设备模式（未批量创建时）：沿用现有控件参数 + 场景基准
        if (_devices.Count == 0)
        {
            if (chkAutoTemperature.IsChecked == true)
            {
                _temperature += (_random.NextDouble() - 0.5) * (scenario.TempJitter * 2);
                _temperature = Math.Max(20, Math.Min(100, _temperature));
                sliderTemp.Value = _temperature;
            }

            if (chkAutoSpeed.IsChecked == true)
            {
                _speed += (_random.NextDouble() - 0.5) * (scenario.SpeedJitter * 2);
                _speed = Math.Max(0, Math.Min(2000, _speed));
                sliderSpeed.Value = _speed;
            }

            if (chkAutoStatus.IsChecked == true && _random.NextDouble() < scenario.AlarmRate)
            {
                string[] statuses = { "ONLINE", "ONLINE", "OFFLINE", "ALARM", "MAINTENANCE" };
                _status = statuses[_random.Next(statuses.Length)];
                cmbStatus.SelectedIndex = _status switch { "ONLINE" => 0, "OFFLINE" => 1, "ALARM" => 2, "MAINTENANCE" => 3, _ => 0 };
            }

            await SendDeviceDataAsync();
        }
        else
        {
            // 批量设备模式：每台设备按场景随机游走
            foreach (var dev in _devices)
            {
                dev.Temperature += (_random.NextDouble() - 0.5) * (scenario.TempJitter * 2);
                dev.Temperature = Math.Max(20, Math.Min(100, dev.Temperature));

                dev.Speed += (_random.NextDouble() - 0.5) * (scenario.SpeedJitter * 2);
                dev.Speed = Math.Max(0, Math.Min(2000, dev.Speed));

                if (_random.NextDouble() < scenario.AlarmRate)
                {
                    string[] statuses = { "ONLINE", "ONLINE", "OFFLINE", "ALARM", "MAINTENANCE" };
                    dev.Status = statuses[_random.Next(statuses.Length)];
                }
            }

            await SendBatchDeviceDataAsync();
        }

        // 曲线图数据（使用当前选中设备或第一台）
        var chartSrc = _devices.Count > 0 ? _devices[0] : null;
        var chartTemp = chartSrc?.Temperature ?? _temperature;
        var chartSpeed = chartSrc?.Speed ?? _speed;
        _chartTemps.Add(chartTemp);
        _chartSpeeds.Add(chartSpeed);
        if (_chartTemps.Count > CHART_MAX_POINTS) { _chartTemps.RemoveAt(0); _chartSpeeds.RemoveAt(0); }
        DrawChart();
    }

    private void SendRateTimer_Tick(object? sender, EventArgs e)
    {
        long sendRate = _messageCount - _lastMessageCount;
        txtSendRate.Text = $"{sendRate}/s";
        _lastMessageCount = _messageCount;
    }

    private async Task SendDeviceDataAsync()
    {
        if (!_isConnected) return;

        try
        {
            string deviceId = devIdInput.Text;
            string deviceName = devNameInput.Text;
            string deviceType = (cmbDeviceType.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "CNC";
            var statusTag = ((cmbStatus.SelectedItem as ComboBoxItem)?.Tag?.ToString()) ?? "ONLINE";

            var deviceData = new
            {
                deviceCode = deviceId,
                deviceName = deviceName,
                deviceType = deviceType,
                status = statusTag,
                temperature = Math.Round(_temperature, 2),
                speed = Math.Round(_speed, 2),
                lastHeartbeat = DateTime.UtcNow
            };

            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.PostAsJsonAsync($"{apiBase}/api/dashboard/device/simulate", deviceData);

            if (response.IsSuccessStatusCode)
            {
                _messageCount++;
                txtMessageCount.Text = _messageCount.ToString();
                txtLastSend.Text = DateTime.Now.ToString("HH:mm:ss");

                var json = JsonConvert.SerializeObject(deviceData, Formatting.None);
                AppendRealTimeData($"[{DateTime.Now:HH:mm:ss}] {json}");
                statusBar.Text = "数据已发送";
            }
            else
            {
                AppendRealTimeData($"[ERROR] HTTP {(int)response.StatusCode}");
            }

            // 双通道：同时发布 MQTT 遥测（走 EMQX → .NET网关 → Kafka → 看板）
            await PublishMqttTelemetryAsync(deviceId, statusTag);
        }
        catch (Exception ex)
        {
            AppendRealTimeData($"[ERROR] {ex.Message}");
            statusBar.Text = $"发送失败: {ex.Message}";
        }
    }

    /// <summary>
    /// 批量设备模拟：全部设备 MQTT 推送 + 首台 HTTP（避免大量 HTTP 请求）
    /// </summary>
    private async Task SendBatchDeviceDataAsync()
    {
        if (!_isConnected || _devices.Count == 0) return;

        try
        {
            string apiBase = apiServerInput.Text.TrimEnd('/');
            int sent = 0;

            // MQTT 推送全部设备（高吞吐链路）
            foreach (var dev in _devices)
            {
                if (await PublishMqttTelemetryAsync(dev.DeviceCode, dev.Status, dev.Temperature, dev.Speed))
                    sent++;
            }

            // 首台设备同时走 HTTP（演示数据链路兜底）
            var first = _devices[0];
            var deviceData = new
            {
                deviceCode = first.DeviceCode,
                deviceName = first.DeviceName,
                deviceType = first.DeviceType,
                status = first.Status,
                temperature = Math.Round(first.Temperature, 2),
                speed = Math.Round(first.Speed, 2),
                lastHeartbeat = DateTime.UtcNow
            };
            var response = await _httpClient.PostAsJsonAsync($"{apiBase}/api/dashboard/device/simulate", deviceData);

            if (response.IsSuccessStatusCode)
            {
                _messageCount += sent + 1;
                txtMessageCount.Text = _messageCount.ToString();
                txtLastSend.Text = DateTime.Now.ToString("HH:mm:ss");
                AppendRealTimeData($"[{DateTime.Now:HH:mm:ss}] 批量推送 {_devices.Count} 台设备 (MQTT {sent}+HTTP 1)");
                statusBar.Text = $"批量推送 {_devices.Count} 台设备";
            }
            else
            {
                AppendRealTimeData($"[ERROR] HTTP {(int)response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            AppendRealTimeData($"[ERROR] {ex.Message}");
            statusBar.Text = $"批量发送失败: {ex.Message}";
        }
    }

    /// <summary>
    /// 批量创建设备（调用后端 batch 接口）
    /// </summary>
    private async void BtnBatchCreate_Click(object sender, RoutedEventArgs e)
    {
        if (!_isConnected)
        {
            MessageBox.Show("请先连接API", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        int count = 0;
        if (!int.TryParse(batchCountInput.Text.Trim(), out count) || count <= 0)
        {
            MessageBox.Show("请输入有效的设备数量(1-1000)", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }
        count = Math.Min(count, 1000);

        try
        {
            statusBar.Text = $"正在批量创建 {count} 台设备...";
            var payload = new List<object>();
            for (int i = 1; i <= count; i++)
            {
                var code = $"DEV-{i:D3}";
                var dev = new SimulatedDevice(code, $"设备{i:D3}", "CNC")
                {
                    Temperature = _scenario.BaseTemp + (_random.NextDouble() - 0.5) * 6,
                    Speed = _scenario.BaseSpeed + (_random.NextDouble() - 0.5) * _scenario.SpeedJitter * 2,
                    Pressure = 1.5,
                    Power = 50,
                    Status = _scenario.DefaultStatus,
                    TempOffset = (_random.NextDouble() - 0.5) * 4,
                    SpeedOffset = (_random.NextDouble() - 0.5) * 100,
                };
                _devices.Add(dev);
                payload.Add(new
                {
                    deviceCode = dev.DeviceCode,
                    deviceName = dev.DeviceName,
                    deviceType = dev.DeviceType,
                    status = dev.Status,
                    temperature = Math.Round(dev.Temperature, 2),
                    speed = Math.Round(dev.Speed, 2),
                    lastHeartbeat = DateTime.UtcNow
                });
            }

            string apiBase = apiServerInput.Text.TrimEnd('/');
            var response = await _httpClient.PostAsJsonAsync($"{apiBase}/api/dashboard/device/batch", payload);
            if (response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync();
                var result = JsonConvert.DeserializeObject<dynamic>(body);
                int created = result?.data ?? count;
                statusBar.Text = $"批量创建完成：{created} 台设备（共 {_devices.Count} 台）";
                AppendRealTimeData($"[{DateTime.Now:HH:mm:ss}] 批量创建 {created} 台设备成功");
                RefreshDeviceListBox();
                // 批量创建后自动开始模拟
                if (!_isSimulating && btnStartSimulation.IsEnabled)
                {
                    _isSimulating = true;
                    _simulationTimer.Start();
                    btnStartSimulation.Content = "停止模拟";
                    txtSimulationStatus.Text = "运行中";
                    txtSimulationStatus.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
                }
            }
            else
            {
                statusBar.Text = $"批量创建失败: HTTP {(int)response.StatusCode}";
                _devices.Clear();
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show($"批量创建失败: {ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            statusBar.Text = $"批量创建失败: {ex.Message}";
        }
    }

    private void RefreshDeviceListBox()
    {
        deviceListBox.ItemsSource = null;
        deviceListBox.ItemsSource = _devices;
    }

    private void AppendRealTimeData(string text)
    {
        Dispatcher.Invoke(() =>
        {
            var sb = new StringBuilder();
            sb.AppendLine(text);
            var lines = txtRealTimeData.Text.Split('\n');
            for (int i = 0; i < Math.Min(lines.Length, 50); i++)
                if (!string.IsNullOrWhiteSpace(lines[i])) sb.AppendLine(lines[i]);
            txtRealTimeData.Text = sb.ToString();
            txtRealTimeData.ScrollToEnd();
        });
    }

    private void SliderTemp_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (tempValue == null) return;
        _temperature = e.NewValue;
        tempValue.Text = Math.Round(_temperature, 1).ToString("F1");
    }

    private void SliderSpeed_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (speedValue == null) return;
        _speed = e.NewValue;
        speedValue.Text = Math.Round(_speed, 0).ToString("F0");
    }

    private void SliderPressure_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (pressureValue == null) return;
        _pressure = e.NewValue;
        pressureValue.Text = Math.Round(_pressure, 2).ToString("F2");
    }

    private void SliderPower_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (powerValue == null) return;
        _power = e.NewValue;
        powerValue.Text = Math.Round(_power, 0).ToString("F0");
    }

    private void BtnApplyParams_Click(object sender, RoutedEventArgs e)
    {
        _status = ((cmbStatus.SelectedItem as ComboBoxItem)?.Tag?.ToString()) ?? "ONLINE";
        statusBar.Text = $"参数已更新 - {_status}";
    }

    // ─── 场景预设 ───
    private void CmbScenario_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        // XAML 初始化时 SelectedIndex="0" 会触发本事件，此时控件可能尚未全部初始化
        if (sliderTemp == null || sliderSpeed == null || cmbStatus == null) return;
        if (cmbScenario.SelectedItem is not ComboBoxItem item || cmbScenario.SelectedIndex < 0) return;
        var name = item.Tag?.ToString() ?? "正常运行";
        var scenario = SimulationScenario.Presets.FirstOrDefault(s => s.Name == name) ?? SimulationScenario.Presets[0];
        _scenario = scenario;

        // 应用场景基准值到当前参数
        _temperature = scenario.BaseTemp;
        _speed = scenario.BaseSpeed;
        _status = scenario.DefaultStatus;
        sliderTemp.Value = _temperature;
        sliderSpeed.Value = _speed;
        cmbStatus.SelectedIndex = _status switch { "ONLINE" => 0, "OFFLINE" => 1, "ALARM" => 2, "MAINTENANCE" => 3, _ => 0 };

        // 批量设备：全部重置为场景状态
        foreach (var dev in _devices)
        {
            dev.Status = scenario.DefaultStatus;
            dev.Temperature = scenario.BaseTemp + dev.TempOffset;
            dev.Speed = scenario.BaseSpeed + dev.SpeedOffset;
        }

        statusBar.Text = $"已切换场景：{scenario.Name} (温度 {scenario.BaseTemp}°C / 转速 {scenario.BaseSpeed}rpm / 故障率 {(int)(scenario.AlarmRate * 100)}%)";
    }

    // ─── 发送频率 ───
    private void CmbSendInterval_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_simulationTimer == null || cmbSendInterval.SelectedItem is not ComboBoxItem item) return;
        if (int.TryParse(item.Tag?.ToString(), out var ms) && ms >= 100)
        {
            _simulationTimer.Interval = TimeSpan.FromMilliseconds(ms);
        }
    }

    // ─── 实时曲线 ───
    private void ChartCanvas_SizeChanged(object sender, SizeChangedEventArgs e)
    {
        DrawChart();
    }

    private void DrawChart()
    {
        if (chartCanvas == null) return;
        chartCanvas.Children.Clear();
        var w = chartCanvas.ActualWidth;
        var h = chartCanvas.ActualHeight;
        if (w <= 10 || h <= 10 || _chartTemps.Count < 2) return;

        // 网格线
        for (int i = 0; i <= 4; i++)
        {
            var gy = h * i / 4;
            chartCanvas.Children.Add(new Line
            {
                X1 = 0, Y1 = gy, X2 = w, Y2 = gy,
                Stroke = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#334155")),
                StrokeThickness = 0.5
            });
        }

        // 温度曲线（橙色） — 左侧 0~100 轴
        DrawSeries(_chartTemps, 0, 100, "#F59E0B", w, h);
        // 转速曲线（青色） — 右侧 0~2000 轴
        DrawSeries(_chartSpeeds, 0, 2000, "#06B6D4", w, h);

        // 统计文本
        var curT = _chartTemps[^1];
        var curS = _chartSpeeds[^1];
        chartStatsText.Text = $"当前 {curT:F1}°C / {curS:F0}rpm";
    }

    private void DrawSeries(List<double> data, double min, double max, string color, double w, double h)
    {
        if (data.Count < 2) return;
        var brush = new SolidColorBrush((Color)ColorConverter.ConvertFromString(color));
        var points = new PointCollection();
        for (int i = 0; i < data.Count; i++)
        {
            var x = w * i / (CHART_MAX_POINTS - 1);
            var y = h - (h - 8) * ((data[i] - min) / (max - min)) - 4;
            points.Add(new Point(x, y));
        }
        var poly = new Polyline { Points = points, Stroke = brush, StrokeThickness = 1.5 };
        poly.StrokeLineJoin = PenLineJoin.Round;
        chartCanvas.Children.Add(poly);
    }

    // ─── 配置持久化 ───
    private string ConfigPath => System.IO.Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "MESDeviceSimulator", "config.json");

    private void SaveConfig()
    {
        try
        {
            var dir = System.IO.Path.GetDirectoryName(ConfigPath);
            if (!Directory.Exists(dir)) Directory.CreateDirectory(dir!);
            var cfg = new
            {
                ApiServer = apiServerInput.Text,
                MqttServer = mqttServerInput.Text,
                MqttPort = mqttPortInput.Text,
                BatchCount = batchCountInput.Text,
                Scenario = cmbScenario.SelectedItem is ComboBoxItem si ? si.Tag?.ToString() : "正常运行",
                SendIntervalMs = cmbSendInterval.SelectedItem is ComboBoxItem ii ? ii.Tag?.ToString() : "2000",
                Temperature = _temperature,
                Speed = _speed,
                Pressure = _pressure,
                Power = _power,
                Status = _status,
                AutoTemp = chkAutoTemperature.IsChecked == true,
                AutoSpeed = chkAutoSpeed.IsChecked == true,
                AutoStatus = chkAutoStatus.IsChecked == true,
            };
            File.WriteAllText(ConfigPath, JsonConvert.SerializeObject(cfg, Formatting.Indented));
        }
        catch { /* 忽略持久化失败 */ }
    }

    private void LoadConfig()
    {
        try
        {
            if (!File.Exists(ConfigPath)) return;
            var cfg = JsonConvert.DeserializeObject<Dictionary<string, string>>(File.ReadAllText(ConfigPath));
            if (cfg == null) return;

            if (cfg.TryGetValue("ApiServer", out var api) && !string.IsNullOrWhiteSpace(api)) apiServerInput.Text = api;
            if (cfg.TryGetValue("MqttServer", out var mqtt) && !string.IsNullOrWhiteSpace(mqtt)) mqttServerInput.Text = mqtt;
            if (cfg.TryGetValue("MqttPort", out var port) && !string.IsNullOrWhiteSpace(port)) mqttPortInput.Text = port;
            if (cfg.TryGetValue("BatchCount", out var bc) && !string.IsNullOrWhiteSpace(bc)) batchCountInput.Text = bc;
            if (cfg.TryGetValue("SendIntervalMs", out var si))
            {
                foreach (var item in cmbSendInterval.Items)
                {
                    if (item is ComboBoxItem ci && ci.Tag?.ToString() == si) { cmbSendInterval.SelectedItem = item; break; }
                }
            }
            if (cfg.TryGetValue("Scenario", out var sc))
            {
                for (int i = 0; i < cmbScenario.Items.Count; i++)
                {
                    if (cmbScenario.Items[i] is ComboBoxItem ci && ci.Tag?.ToString() == sc) { cmbScenario.SelectedIndex = i; break; }
                }
            }
            if (cfg.TryGetValue("Temperature", out var t) && double.TryParse(t, out var tv)) { _temperature = tv; sliderTemp.Value = tv; }
            if (cfg.TryGetValue("Speed", out var sp) && double.TryParse(sp, out var sv)) { _speed = sv; sliderSpeed.Value = sv; }
            if (cfg.TryGetValue("Pressure", out var pr) && double.TryParse(pr, out var pv)) { _pressure = pv; sliderPressure.Value = pv; }
            if (cfg.TryGetValue("Power", out var pw) && double.TryParse(pw, out var wv)) { _power = wv; sliderPower.Value = wv; }
        }
        catch { /* 忽略配置加载失败 */ }
    }

    protected override void OnClosed(EventArgs e)
    {
        SaveConfig();
        _simulationTimer.Stop();
        _sendRateTimer.Stop();
        _httpClient.Dispose();
        base.OnClosed(e);
    }
}
