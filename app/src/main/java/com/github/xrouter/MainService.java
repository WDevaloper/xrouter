package com.github.xrouter;

import com.github.core.annotation.Service;

@Service(path = "/app/main")
public class MainService implements IMainService{
    @Override
    public void showMainPage() {

    }

    @Override
    public void showMainPage(String name) {

    }

    @Override
    public void showMainPage(String name, int age) {

    }
}
