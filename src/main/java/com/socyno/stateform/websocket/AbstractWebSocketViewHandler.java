package com.socyno.stateform.websocket;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.socyno.base.bscmodel.R;
import com.socyno.webbsc.websocket.BaseWebSocketHandler;


public abstract class AbstractWebSocketViewHandler extends BaseWebSocketHandler implements WebSocketConfigurer {
    
    private final AbstractWebSocketViewHandler instance;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(instance, getRequestPath()).setAllowedOrigins("*");
    }
    
    public AbstractWebSocketViewHandler() {
        instance = this;
    }
    
    public abstract String getRequestPath();
    
    public abstract WebSocketViewDefinition getFormViewDefinition(WebSocketRequest request, WebSocketSession session) throws Exception;
    
    @Override
    protected void preHandle(WebSocketSession session, WebSocketRequest request) throws Exception {
        writeResponse(session, R.ok().setData(getFormViewDefinition(request, session)));
    }
}
