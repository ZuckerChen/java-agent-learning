package com.cz.bytebuddy;

import com.sun.tools.javac.util.Log;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * @author chenzhang
 * @date 2022/12/15 6:30 下午
 */
public class ByteBuddyDemo {

    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        // 创建ByteBuddy对象
        DemoService demoService= new ByteBuddy()
            // subclass增强方式
            .subclass(DemoService.class)
            // 新类型的类名
            .name("com.cz.bytebuddy.DynamicDemo")
            // 拦截其中的get开头的方法
            .method(ElementMatchers.nameStartsWith("get"))
            .intercept(MethodDelegation.to(new LogInterceptor()))
            .make()
            // 加载新类型，默认WRAPPER策略
            .load(ByteBuddy.class.getClassLoader(), Default.WRAPPER)
            .getLoaded()
            // 通过 Java反射创建 实例
            .newInstance();

        demoService.getName();
        demoService.getAge();
    }
}
