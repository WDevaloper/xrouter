package com.github.core;


public interface RouterService {
    <T> T newServiceInstance(String path, Class<?>[] parameterTypes, Object... initargs);
}