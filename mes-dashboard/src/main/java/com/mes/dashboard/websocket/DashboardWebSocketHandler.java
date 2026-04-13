package com.mes.dashboard.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mes.dashboard.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket处理器
 * @author MES
 * @description 看板WebSocket连接处理，支持实时数据推送
 */
@Slf4j
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    /** WebSocket会话列表 */
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    /** 会话订阅关系 */
    private final Map<WebSocketSession, String> sessionSubscriptions = new ConcurrentHashMap<>();
    /** 定时任务调度器 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 构造函数
     * @param dashboardService 看板服务
     */
    public DashboardWebSocketHandler(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        scheduler.scheduleAtFixedRate(this::broadcastData, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 连接建立后处理
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket连接建立: {}", session.getId());
    }

    /**
     * 处理文本消息
     * @param session WebSocket会话
     * @param message 消息内容
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (payload.startsWith("SUBSCRIBE:")) {
            String workstationId = payload.substring("SUBSCRIBE:".length());
            sessionSubscriptions.put(session, workstationId);
            log.info("客户端 {} 订阅工位: {}", session.getId(), workstationId);
        } else if (payload.equals("UNSUBSCRIBE")) {
            sessionSubscriptions.remove(session);
            log.info("客户端 {} 取消订阅", session.getId());
        }
    }

    /**
     * 连接关闭后处理
     * @param session WebSocket会话
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sessionSubscriptions.remove(session);
        log.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
    }

    /**
     * 传输错误处理
     * @param session WebSocket会话
     * @param exception 异常信息
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
        sessions.remove(session);
        sessionSubscriptions.remove(session);
    }

    /**
     * 广播数据给所有客户端
     */
    private void broadcastData() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> overview = dashboardService.getOverview();
            var devices = dashboardService.getAllDeviceStatus();
            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("overview", overview);
            payload.put("devices", devices);
            payload.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(payload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                } else {
                    sessions.remove(session);
                    sessionSubscriptions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("广播数据失败", e);
        }
    }
}
