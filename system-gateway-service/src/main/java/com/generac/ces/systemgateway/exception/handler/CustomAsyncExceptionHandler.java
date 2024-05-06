package com.generac.ces.systemgateway.exception.handler;

import java.lang.reflect.Method;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

@Log4j2
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        StringBuilder errMsg =
                new StringBuilder("Async exception: ")
                        .append(ex.getMessage())
                        .append(" | method name: ")
                        .append(method.getName())
                        .append(" | parameter values: ");
        for (Object param : params) {
            errMsg.append(param).append(" ");
        }
        log.error(errMsg, ex);
    }
}
