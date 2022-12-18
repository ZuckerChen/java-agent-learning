package com.cz.premain;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * @author chenzhang
 * @date 2022/12/16 8:44 上午
 */
public class LogInterceptor {

    /**
     *
     * @param target 动态生成的目标对象
     * @param method 正在执行方法的（目标对象父类的method）
     * @param arguments 正在执行方法的全部参数
     * @param delegate 目标对象的代理
     * @param callable 方法的调用对象，用于执行原方法
     * @return
     * @throws Exception
     */
    @RuntimeType
    public static Object intercept(
        @This Object target,
        @Origin Method method,
        @AllArguments Object[] arguments,
        @Super Object delegate,
        @SuperCall Callable<?> callable) throws Exception {

        long start = System.currentTimeMillis();
        Object result = callable.call();
        long cost = System.currentTimeMillis() - start;
        System.out.println(method + " 执行耗时：" + cost + "ms");
        return result;
    }
}
