package com.github.xrouter.utils;

import android.content.Intent;


import com.github.core.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.Objects;

public class ParameterInjector {
    public static void inject(Object target, Intent intent) {
        Class<?> clazz = target.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Autowired autowired = field.getAnnotation(Autowired.class);
                assert autowired != null;
                String name = autowired.name();
                if (name.isEmpty()) {
                    name = field.getName();
                }
                try {
                    field.setAccessible(true);
                    Object value = Objects.requireNonNull(intent.getExtras()).get(name);
                    if (value != null) {
                        field.set(target, value);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}