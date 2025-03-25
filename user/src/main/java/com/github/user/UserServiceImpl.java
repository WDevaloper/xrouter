package com.github.user;

import android.util.Log;

import com.github.core.annotation.Service;
import com.github.provider.UserService;

@Service(path = "/user/user")
public class UserServiceImpl implements UserService {
    @Override
    public void showUser() {
        Log.e("TAG", "showUser: ");
    }

    @Override
    public void showUser(String name) {
        Log.e("TAG", "showUser: " );
    }
}
