package com.github.xrouter;

import android.content.Intent;

import com.github.core.RouteTable;
import com.github.xrouter.interceptor.RouteInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());
    private static Router instance;
    private final List<RouteInterceptor> interceptors;

    private Router() {
        interceptors = new ArrayList<>();
    }

    public static Router getInstance() {
        if (instance == null) {
            synchronized (Router.class) {
                if (instance == null) {
                    instance = new Router();
                }
            }
        }
        return instance;
    }

    public void addInterceptor(RouteInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    public boolean navigate(RouteRequest request) {
        try {
            for (RouteInterceptor interceptor : interceptors) {
                if (interceptor.intercept(request)) {
                    return false;
                }
            }

            ServiceLoader.load(RouteTable.class).forEach(table -> {
                Map<String, Class<?>> routeMap = table.getRouteMap();
                if (routeMap.containsKey(request.getPath())) {
                    Class<?> targetClass = routeMap.get(request.getPath());
                    try {
                        Object instance = targetClass.newInstance();
                        for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                            // 这里可以使用反射设置参数
                            new Intent(null, targetClass);
                        }
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, "Failed to create instance of target class: " + targetClass.getName(), e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred during navigation", e);
            return false;
        }

        return true;
    }
}