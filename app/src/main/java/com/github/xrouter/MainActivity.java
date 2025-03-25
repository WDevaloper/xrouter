package com.github.xrouter;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.core.annotation.Route;
import com.github.xrouter.interceptor.RouteInterceptor;

@Route(path = "/app/main", group = "Main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // 添加拦截器
        Router.getInstance().addInterceptor(new RouteInterceptor() {
            @Override
            public boolean intercept(RouteRequest request) {
                // 这里可以进行权限验证等操作
                Log.e("TAG", "intercept: 这里可以进行权限验证等操作");
                return false;
            }
        });

        // 创建路由请求
        RouteRequest request = new RouteRequest("/app/service", "app");
        request.putParam("key", "value");

        // 进行路由跳转
        boolean result = Router.getInstance().navigate(request);
        Log.e("TAG", "onCreate: " + result);
    }
}