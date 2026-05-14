package com.example.jianlou.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    // account -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String account, WebSocketSession session) {
        sessions.put(account, session);
    }

    public void removeSession(String account) {
        sessions.remove(account);
    }

    public WebSocketSession getSession(String account) {
        return sessions.get(account);
    }

    public boolean isOnline(String account) {
        return sessions.containsKey(account) && sessions.get(account).isOpen();
    }
}