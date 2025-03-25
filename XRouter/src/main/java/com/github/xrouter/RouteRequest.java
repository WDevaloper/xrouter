package com.github.xrouter;


import java.util.HashMap;
import java.util.Map;

public class RouteRequest {
    private String path;
    private String group;
    private Map<String, Object> params;

    public RouteRequest(String path, String group) {
        this.path = path;
        this.group = group;
        this.params = new HashMap<>();
    }

    public String getPath() {
        return path;
    }

    public String getGroup() {
        return group;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void putParam(String key, Object value) {
        params.put(key, value);
    }
}