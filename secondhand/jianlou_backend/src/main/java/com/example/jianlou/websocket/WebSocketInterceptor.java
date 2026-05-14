package com.example.jianlou.websocket;

import com.example.jianlou.entity.Cookie;
import com.example.jianlou.repository.CookieRepository;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Resource
    private CookieRepository cookieRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String cookie = servletRequest.getServletRequest().getParameter("cookie");
            
            if (cookie != null) {
                Cookie c = cookieRepository.findByCookie(cookie);
                if (c != null) {
                    attributes.put("account", c.getAccount());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}