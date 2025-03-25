package com.github.order;

import android.util.Log;

import com.github.core.annotation.Service;
import com.github.provider.OrderService;

@Service(path = "/order/order")
public class OrderServiceImpl implements OrderService {
    @Override
    public void showOrder() {
        Log.e("TAG", "showOrder: ");
    }

    @Override
    public void showOrder(String name) {
        Log.e("TAG", "showOrder: ");
    }
}
