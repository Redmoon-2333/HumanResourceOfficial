package com.redmoon2333.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * 请求日志过滤器
 * 用于调试CORS和请求问题
 */
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String origin = httpRequest.getHeader("Origin");
        
        // 记录所有请求（排除静态资源）
        if (!uri.contains("favicon.ico") && !uri.contains(".css") && !uri.contains(".js")) {
            logger.info("收到请求 - 方法: {}, URI: {}, Origin: {}", method, uri, origin);
            
            // 如果是OPTIONS预检请求，记录详细信息
            if ("OPTIONS".equals(method)) {
                logger.info("OPTIONS预检请求 - Headers: Access-Control-Request-Method: {}, Access-Control-Request-Headers: {}", 
                    httpRequest.getHeader("Access-Control-Request-Method"),
                    httpRequest.getHeader("Access-Control-Request-Headers"));
            }
        }
        
        try {
            chain.doFilter(request, response);
            
            // 记录响应状态
            if (!uri.contains("favicon.ico") && !uri.contains(".css") && !uri.contains(".js")) {
                logger.info("响应状态 - URI: {}, Status: {}, CORS Headers: {}", 
                    uri, 
                    httpResponse.getStatus(),
                    httpResponse.getHeader("Access-Control-Allow-Origin"));
            }
        } catch (Exception e) {
            logger.error("请求处理异常 - URI: {}, 错误: {}", uri, e.getMessage());
            throw e;
        }
    }
}
