package com.cz.agentmain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.CircularityLock;
import net.bytebuddy.agent.builder.AgentBuilder.Default;
import net.bytebuddy.agent.builder.AgentBuilder.DescriptionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.FallbackStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.InstallationListener;
import net.bytebuddy.agent.builder.AgentBuilder.Listener;
import net.bytebuddy.agent.builder.AgentBuilder.LocationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.PoolStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RawMatcher;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * @author chenzhang
 * @date 2022/12/15 11:18 上午
 */
public class HotDeployAgentmain {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("agentmain start");

        try {
            Class<?> aClass = Class.forName("com.cz.demo.hotdeploy.service.PayService");
            File file = new File(
                    "/Users/dzsb-002298/project/java-agent-learning/project-demo/target/classes/com/cz/demo/hotdeploy/service/PayService.class");
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            inst.redefineClasses(new ClassDefinition(aClass, bytes));
            System.out.println("redefine success");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
