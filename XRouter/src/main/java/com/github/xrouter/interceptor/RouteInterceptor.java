package com.github.xrouter.interceptor;


import com.github.xrouter.RouteRequest;

public interface RouteInterceptor {
    boolean intercept(RouteRequest request);
}