package com.generac.ces.systemgateway.configuration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CacheStore<T> {
    private Cache<String, T> cache;
    private final int expiryDuration;
    private final String timeUnit;

    public CacheStore(int maximumSize, int expiryDuration, TimeUnit timeUnit) {
        this.expiryDuration = expiryDuration;
        this.timeUnit = timeUnit.name();

        cache =
                CacheBuilder.newBuilder()
                        .maximumSize(maximumSize)
                        .expireAfterWrite(expiryDuration, timeUnit)
                        .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                        .build();
    }

    public T get(String key, Callable<T> valueLoader) throws ExecutionException {
        return cache.get(key, valueLoader);
    }

    public T getIfPresent(String key) {
        return cache.getIfPresent(key);
    }

    public boolean keyExists(String key) {
        return cache.getIfPresent(key) != null;
    }

    public void add(String key, T value) {
        if (key != null && value != null) {
            cache.put(key, value);
        }
    }

    public String getExpiryDuration() {
        return String.valueOf(this.expiryDuration);
    }

    public String getTimeUnit() {
        return this.timeUnit;
    }
}
