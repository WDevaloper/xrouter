package com.github.xrouter;

import android.content.Context;
import android.content.Intent;

import com.github.core.RouteTable;
import com.github.core.RouterService;
import com.github.xrouter.interceptor.RouteInterceptor;
import com.github.xrouter.utils.ParameterInjector;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());
    private final List<RouteInterceptor> interceptors = new ArrayList<>();
    private final Map<String, RouteTable> loadedRouteTables = new HashMap<>();
    private final Map<String, Class<?>> dynamicRouteMap = new HashMap<>();
    private final Map<Class<?>, Object> serviceInstances = new HashMap<>();
    private final Context context;

    public Router(Context context) {
        this.context = context;
    }

    public void addInterceptor(RouteInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    public void registerRoute(String path, Class<?> targetClass) {
        dynamicRouteMap.put(path, targetClass);
    }

    public void unregisterRoute(String path) {
        dynamicRouteMap.remove(path);
    }

    public boolean navigate(RouteRequest request) {
        try {
            for (RouteInterceptor interceptor : interceptors) {
                if (interceptor.intercept(request)) {
                    return false;
                }
            }

            String path = request.getPath();
            Class<?> targetClass = null;

            // 先检查动态路由表
            if (dynamicRouteMap.containsKey(path)) {
                targetClass = dynamicRouteMap.get(path);
            } else {
                String group = request.getGroup();
                RouteTable routeTable = getRouteTableByGroup(group);
                if (routeTable != null) {
                    Map<String, Class<?>> routeMap = routeTable.getRouteMap();
                    for (Map.Entry<String, Class<?>> entry : routeMap.entrySet()) {
                        String routePath = entry.getKey();
                        if (path.startsWith(routePath)) {
                            targetClass = entry.getValue();
                            break;
                        }
                    }
                }
            }

            if (targetClass != null) {
                try {
                    Intent intent = new Intent(context, targetClass);
                    for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            intent.putExtra(key, (String) value);
                        } else if (value instanceof Integer) {
                            intent.putExtra(key, (Integer) value);
                        } else if (value instanceof Boolean) {
                            intent.putExtra(key, (Boolean) value);
                        }
                        // 可以根据需要添加更多类型的处理
                    }
                    if (!(context instanceof android.app.Activity)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);
                    Object instance = targetClass.newInstance();
                    ParameterInjector.inject(instance, intent);
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.log(Level.SEVERE, "Failed to create instance of target class: " + targetClass.getName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred during navigation", e);
            return false;
        }

        return true;
    }

    private RouteTable getRouteTableByGroup(String group) {
        if (!loadedRouteTables.containsKey(group)) {
            ServiceLoader<RouteTable> serviceLoader = ServiceLoader.load(RouteTable.class);
            for (RouteTable table : serviceLoader) {
                // 假设 RouteTable 实现类的命名规则为 moduleName$RouteTable_$group
                if (table.getClass().getSimpleName().endsWith(group)) {
                    loadedRouteTables.put(group, table);
                    break;
                }
            }
        }
        return loadedRouteTables.get(group);
    }

    public <T> T getService(String path,
                            Class<?>[] parameterTypes,
                            Object[] initArgs) {
        ServiceLoader<RouterService> serviceLoader = ServiceLoader.load(RouterService.class);
        for (RouterService manager : serviceLoader) {
            return  manager.newServiceInstance(path, parameterTypes, initArgs);
        }
        return null;
    }
}