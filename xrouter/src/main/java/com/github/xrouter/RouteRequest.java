package com.github.xrouter;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class RouteRequest implements Parcelable {
    private final String path;
    private final String group;
    private final Map<String, Object> params;

    public RouteRequest(String path, String group) {
        this.path = path;
        this.group = group;
        this.params = new HashMap<>();
    }

    protected RouteRequest(Parcel in) {
        path = in.readString();
        group = in.readString();
        int size = in.readInt();
        params = new HashMap<>();
        Gson gson = new Gson();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String jsonValue = in.readString();
            // 这里可以根据类型进行反序列化
            Object value = gson.fromJson(jsonValue, Object.class);
            params.put(key, value);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(group);
        dest.writeInt(params.size());
        Gson gson = new Gson();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            dest.writeString(entry.getKey());
            String jsonValue = gson.toJson(entry.getValue());
            dest.writeString(jsonValue);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RouteRequest> CREATOR = new Creator<RouteRequest>() {
        @Override
        public RouteRequest createFromParcel(Parcel in) {
            return new RouteRequest(in);
        }

        @Override
        public RouteRequest[] newArray(int size) {
            return new RouteRequest[size];
        }
    };

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