package com.github.xrouter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.core.RouterService;
import com.github.core.annotation.Route;
import com.github.provider.OrderService;
import com.github.provider.UserService;
import com.github.xrouter.interceptor.RouteInterceptor;

import javax.annotation.Nullable;

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


    }

    public void jumpPage(View view) {
        boolean result = isResult();
        Log.e("TAG", "onCreate: " + result);
    }

    private boolean isResult() {
        // 创建路由请求
        RouteRequest request = new RouteRequest("/app/main", "Main");
        request.putParam("key", "value");

        Router router = new Router(this);

        IMainService mainService =
                router.getService("/app/main", null, null);
        Log.e("TAG", "mainService: " + mainService);

        UserService userService =
                router.getService("/user/user", null, null);
        Log.e("TAG", "userService: " + userService);

        OrderService orderService =
                router.getService("/order/order", null, null);
        Log.e("TAG", "orderService: " + orderService);

        // 添加拦截器
        router.addInterceptor(new RouteInterceptor() {
            @Override
            public boolean intercept(RouteRequest request) {
                // 这里可以进行权限验证等操作
                Log.e("TAG", "intercept: 这里可以进行权限验证等操作");
                return false;
            }
        });


        // 进行路由跳转
        boolean result = router.navigate(request);
        return result;
    }


}