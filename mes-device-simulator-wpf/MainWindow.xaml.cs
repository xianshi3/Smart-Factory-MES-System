using System.Net.Http;
using System.Net.Http.Json;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using MQTTnet;
using MQTTnet.Client;
using Newtonsoft.Json;

namespace MESDeviceSimulator;

public partial class MainWindow : Window
{
    private readonly HttpClient _httpClient = new();
    private readonly DispatcherTimer _simulationTimer;
    private readonly Random _random = new();
    private long _messageCount = 0;
    private long _lastMessageCount = 0;
    private bool _isSimulating = false;
    private bool _isConnected = false;
    private bool _isDarkTheme = false;
    private readonly DispatcherTimer _sendRateTimer;

    private IMqttClient? _mqttClient;
    private bool _mqttConnected = false;

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

        ApplyLightTheme();

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
        connStatus.Foreground = darkMuted;
        
        // Config card
        configCard.Background = darkCard;
        lblApiAddress.Foreground = darkMuted;
        apiServerInput.Background = darkCard;
        apiServerInput.Foreground = darkText;
        apiServerInput.BorderBrush = darkBorder;

        lblMqttServer.Foreground = darkMuted;
        mqttServerInput.Background = darkCard;
        mqttServerInput.Foreground = darkText;
        mqttServerInput.BorderBrush = darkBorder;
        mqttPortInput.Background = darkCard;
        mqttPortInput.Foreground = darkText;
        mqttPortInput.BorderBrush = darkBorder;
        
        lblDeviceId.Foreground = darkMuted;
        devIdInput.Background = darkCard;
        devIdInput.Foreground = darkText;
        devIdInput.BorderBrush = darkBorder;
        
        lblDeviceName.Foreground = darkMuted;
        devNameInput.Background = darkCard;
        devNameInput.Foreground = darkText;
        devNameInput.BorderBrush = darkBorder;
        
        lblDeviceType.Foreground = darkMuted;
        
        // Param card
        paramCard.Background = darkCard;
        titleText.Foreground = darkText;
        lblStatus.Foreground = darkMuted;
        
        panelBg.Background = darkPanel;
        
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
        
        tempValue.Foreground = darkText;
        speedValue.Foreground = darkText;
        pressureValue.Foreground = darkText;
        powerValue.Foreground = darkText;
        
        // Stats
        stat1.Background = darkCard;
        stat2.Background = darkCard;
        stat3.Background = darkCard;
        stat4.Background = darkCard;
        
        lblMsgCount.Foreground = darkMuted;
        lblSimStatus.Foreground = darkMuted;
        lblLastSend.Foreground = darkMuted;
        lblSendRate.Foreground = darkMuted;
        
        txtMessageCount.Foreground = darkText;
        txtSimulationStatus.Foreground = darkText;
        txtLastSend.Foreground = darkText;
        txtSendRate.Foreground = darkText;
        
        // Data card
        dataCard.Background = darkCard;
        dataTitle.Foreground = darkText;
        
        // Bottom
        bottomBar.Background = darkCard;
        apiEndpoint.Foreground = darkMuted;
        statusBar.Foreground = darkMuted;
    }

    private void ApplyLightTheme()
    {
        Background = lightBg;
        
        connStatus.Foreground = lightMuted;
        
        subtitleText.Foreground = lightText;
        descText.Foreground = lightMuted;
        
        configCard.Background = lightCard;
        lblApiAddress.Foreground = lightMuted;
        apiServerInput.Background = lightCard;
        apiServerInput.Foreground = lightText;
        apiServerInput.BorderBrush = lightBorder;

        lblMqttServer.Foreground = lightMuted;
        mqttServerInput.Background = lightCard;
        mqttServerInput.Foreground = lightText;
        mqttServerInput.BorderBrush = lightBorder;
        mqttPortInput.Background = lightCard;
        mqttPortInput.Foreground = lightText;
        mqttPortInput.BorderBrush = lightBorder;
        
        lblDeviceId.Foreground = lightMuted;
        devIdInput.Background = lightCard;
        devIdInput.Foreground = lightText;
        devIdInput.BorderBrush = lightBorder;
        
        lblDeviceName.Foreground = lightMuted;
        devNameInput.Background = lightCard;
        devNameInput.Foreground = lightText;
        devNameInput.BorderBrush = lightBorder;
        
        lblDeviceType.Foreground = lightMuted;
        
        paramCard.Background = lightCard;
        titleText.Foreground = lightText;
        lblStatus.Foreground = lightMuted;
        
        panelBg.Background = lightPanel;
        
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
        
        tempValue.Foreground = lightText;
        speedValue.Foreground = lightText;
        pressureValue.Foreground = lightText;
        powerValue.Foreground = lightText;
        
        stat1.Background = lightCard;
        stat2.Background = lightCard;
        stat3.Background = lightCard;
        stat4.Background = lightCard;
        
        lblMsgCount.Foreground = lightMuted;
        lblSimStatus.Foreground = lightMuted;
        lblLastSend.Foreground = lightMuted;
        lblSendRate.Foreground = lightMuted;
        
        txtMessageCount.Foreground = lightText;
        txtSimulationStatus.Foreground = lightText;
        txtLastSend.Foreground = lightText;
        txtSendRate.Foreground = lightText;
        
        dataCard.Background = lightCard;
        dataTitle.Foreground = lightText;
        
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
                connStatus.Text = "已连接";
                connDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#10B981"));
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
    /// 连接 EMQX MQTT Broker（网关订阅 mes/device/+/data）
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
                Dispatcher.Invoke(() =>
                    statusBar.Text = $"已连接 API + MQTT ({host}:{port})");
                return Task.CompletedTask;
            };
            _mqttClient.DisconnectedAsync += args =>
            {
                _mqttConnected = false;
                Dispatcher.Invoke(() =>
                    statusBar.Text = $"MQTT 已断开，模拟数据仅走HTTP");
                return Task.CompletedTask;
            };

            await _mqttClient.ConnectAsync(options);
            _mqttConnected = _mqttClient.IsConnected;
            if (_mqttConnected)
            {
                statusBar.Text = $"已连接 API + MQTT ({host}:{port})";
            }
        }
        catch (Exception ex)
        {
            _mqttConnected = false;
            statusBar.Text = $"API已连接，MQTT连接失败: {ex.Message}（模拟数据仅走HTTP）";
        }
    }

    /// <summary>
    /// 通过 MQTT 发布遥测数据（topic: mes/device/{deviceCode}/data）
    /// </summary>
    private async Task PublishMqttTelemetryAsync(string deviceCode, string status)
    {
        if (_mqttClient?.IsConnected != true) return;

        try
        {
            var payload = new
            {
                timestamp = DateTime.UtcNow,
                dataType = "telemetry",
                status = status,
                data = new
                {
                    temperature = Math.Round(_temperature, 2),
                    speed = Math.Round(_speed, 2),
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
        }
        catch (Exception ex)
        {
            statusBar.Text = $"MQTT发布失败: {ex.Message}";
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
                var devices = JsonConvert.DeserializeObject<List<DeviceStatus>>(json);
                if (devices != null)
                {
                    deviceListBox.ItemsSource = devices;
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

        _isConnected = false;
        connStatus.Text = "未连接";
        connDot.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444"));
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

        if (chkAutoTemperature.IsChecked == true)
        {
            _temperature += (_random.NextDouble() - 0.5) * 10;
            _temperature = Math.Max(20, Math.Min(100, _temperature));
            sliderTemp.Value = _temperature;
        }

        if (chkAutoSpeed.IsChecked == true)
        {
            _speed += (_random.NextDouble() - 0.5) * 200;
            _speed = Math.Max(0, Math.Min(2000, _speed));
            sliderSpeed.Value = _speed;
        }

        if (chkAutoStatus.IsChecked == true && _random.NextDouble() < 0.05)
        {
            string[] statuses = { "ONLINE", "ONLINE", "OFFLINE", "ALARM", "MAINTENANCE" };
            _status = statuses[_random.Next(statuses.Length)];
            cmbStatus.SelectedIndex = _status switch { "ONLINE" => 0, "OFFLINE" => 1, "ALARM" => 2, "MAINTENANCE" => 3, _ => 0 };
        }

        await SendDeviceDataAsync();
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

    protected override void OnClosed(EventArgs e)
    {
        _simulationTimer.Stop();
        _sendRateTimer.Stop();
        _httpClient.Dispose();
        base.OnClosed(e);
    }
}