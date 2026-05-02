# MES设备模拟器 (WPF)

## 简介

WPF桌面设备模拟器，用于模拟MES系统设备数据并推送到后端。

## 功能

- 设备管理：创建、更新、删除设备
- 数据模拟：温度、速度、压力、功率参数控制
- 自动波动：支持温度、速度自动波动
- 主题切换：亮色/暗色主题

## 运行

```bash
cd mes-device-simulator-wpf
dotnet run
```

## 使用

1. 点击"连接"按钮连接后端API
2. 在设备列表中选择或创建新设备
3. 调整参数（温度、速度等）
4. 点击"开始模拟"启动数据推送

## 技术

- .NET 8 + WPF
- HttpClient调用后端REST API
- DispatcherTimer定时推送数据