package com.cz.premain;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * @author chenzhang
 * @date 2022/12/15 11:18 上午
 */
public class LogAgentmain {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("agent premain attach success");

        AgentBuilder.Transformer transformer = new AgentBuilder.Transformer() {
            @Override
            public Builder<?> transform(
                Builder<?> builder,
                TypeDescription typeDescription,
                ClassLoader classLoader,
                JavaModule javaModule,
                ProtectionDomain protectionDomain) {
                return builder
                    //匹配任何方法
                    .method(ElementMatchers.any())
                    //注册拦截器
                    .intercept(MethodDelegation.to(LogInterceptor.class));
            }
        };

        new AgentBuilder.Default()
            //拦截匹配方式：
            .type(ElementMatchers.nameStartsWith("com.cz.demo.apm.client"))
            //拦截到的类由transformer处理
            .transform(transformer)
            .installOn(inst);

    }
}
