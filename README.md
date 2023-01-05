# 走近Java Agent
## 前言
提到Java Agent(Java探针)这个话题，我们先看下Java Agent有哪些应用场景
1. APM应用监控(Skywalking、Pinpoint)
2. 热部署(Jrebel、美团Sonic)
3. Debug(Idea、Eclipse)
4. JVM诊断(阿里Arthas)
5. 故障注入(阿里Chaos)
6. 还有其他..

我们看到JavaAgent有着这么广泛的应用，那么它是如何做到的呢？
JavaAgent是JVM提供的一套后门工具，可以让我们对运行中的JVM进行监控、分析、修改字节码，并且没有业务侵入性。  
下面我们一起学习一下Agent的基本概念和基本使用

## 基本概念
- **JVMTI(JVM Tool Interface)** 是一套由JVM直接提供的native接口，通过这些接口我们可以查看JVM运行状态，设置回调函数。我们编写的Agent程序底层就是使用了JVMTI
- **JVMTIAgent** 在JDK1.5之前通过c、c++来实现Agent程序(非本篇重点)
- **JPLISAgent(Java Programming Language Instrumentation Services Agent)** 在JDK1.5之后提供了一个"插桩"工具包java.lang.instrument
可以方便的实现Java版本的Agent程序，这种方式实现的Agent就叫做JPLISAgent

## "插桩"工具
简单使用java.lang.instrument工具包，既可以实现JVM级别的AOP
我们先看下Instrumentation的核心方法:
- addTransformer方法用于注册ClassFileTransformer的实现类。JVM读取字节码文件时，会触发JVMTI的ClassFileLoadHook回调事件，触发事件后最终会调用到ClassFileTransformer的transform方法，开发Agent的主要工作就是在transform方法中进行字节码操作  
- redefineClasses和retransformClasses都可以对字节码进行修改和重新加载，区别是如果在类加载之后去重定义就需要使用retransformClasses方法
```
addTransformer(ClassFileTransformer transformer);
redefineClasses(ClassDefinition... definitions) 
retransformClasses(Class<?>... classes) 
```

## 如何实现一个Agent
Agent的实现有以下两种方式
- JDK1.5开始提供的premain，启动期加载
- JDK1.6开始提供的agentmain，运行期加载
### 加载Agent的流程
![](https://yppphoto.hellobixin.com/yppphoto/b2f76796eebb4f929b982544dfdda5eb.png)


### 创建一个通过premain实现Agent的过程： 
#### 1、定义MANIFEST.MF文件
```
Manifest-Version: 1.0
Can-Redefine-Classes: true
Can-Retransform-Classes: true
Premain-Class: xxx.PremainAgent
```
#### 、创建包含premain方法的类
``` java
public class PremainAgent {
    public static void premain(String agentArgs, Instrumentation inst) {}
}
```
#### 3、打包agent jar
将包含premain方法的类和MANIFEST.MF打包成jar
```
mvn clean package
```
#### 4、通过增加启动参数将agent加载到目标JVM
```
-javaagent:/jar包路径=[agentArgs 参数]  将主程序与agent工程关联
```

### 创建一个通过agentmain实现Agent的过程：
#### 1、定义MANIFEST.MF文件
```
Manifest-Version: 1.0
Can-Redefine-Classes: true
Can-Retransform-Classes: true
Agent-Class: xxx.AgentMainAgent
```
#### 2、创建包含agentmain方法的类
``` java
public class TimeAgentmain {
    public static void agentmain(String agentArgs, Instrumentation inst) {}
}
```
#### 3、打包agent jar
将包含agentmain方法的类和MANIFEST.MF打包成jar
```
mvn clean package
```
#### 4、通过Attach加载agent到目标JVM
通过attach机制，则无需再添加启动参数
```
VirtualMachine virtualMachine = VirtualMachine.attach(pid);
virtualMachine.loadAgent("/xxx/agentmain-1.0-SNAPSHOT.jar");
virtualMachine.detach();
```

### 优劣对比
premain的缺点：  
1. 需要在业务系统增加启动参数
2. 只能在类加载之前修改字节码

agentmain解决了premain的这两个缺点，同时也带来另外的局限性  
对类的修改有如下限制：
1. 父类是同一个；
2. 实现的接口数也要相同；
3. 类访问符必须一致；
4. 字段数和字段名必须一致；
5. 新增的方法必须是 private static/final 的；
6. 可以删除修改方法；

对正在执行中的class进行修改，会造成不可预估的业务响应时间，原因是会暂停业务操作等class重新加载完成之后再继续执行，所以这个一般仅用在本地开发环境

## Agent实例
下面我们用premain和agentmain分别实现一个最简单的Agent程序
### 1. 基于premain实现接口耗时的打印
> 使用了ByteBuddy进行字节码操作，原因是ByteBuddy性能好，Api对开发友好，此处只是解释使用ByteBuddy的原因，不在单独介绍Api的使用

1、创建premain类
``` java
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
            //安装到Instrumentation中
            .installOn(inst);

    }
}
```

2、创建Interceper类
``` java
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
```

3、模拟一个目标程序，并添加启动参数-javaagent:/Users/dzsb-002298/project/java-agent-learning/agent-apm/target/agent-apm-1.0-SNAPSHOT.jar
``` java
public class LogApplicationMain {
    public static void main(String[] args) {
        System.out.println("main project start");
        BusinessManager businessManager = new BusinessManager();
        businessManager.doBusiness();
    }
}

public class RpcClient {
    public void invoke() {
        int i = new Random().nextInt(5000);
        try {
            TimeUnit.MILLISECONDS.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("rpc invoke");
    }
}
```

4、执行目标程序LogApplicationMain，可以看到是具体的方法已经被增强，打印了耗时日志
``` 
/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/bin/java -javaagent:/Users/dzsb-002298/project/java-agent-learning/agent-apm/target/agent-apm-1.0-SNAPSHOT.jar -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=59434:/Applications/IntelliJ IDEA.app/Contents/bin -Dfile.encoding=UTF-8 -classpath /Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/tools.jar:/Users/dzsb-002298/project/java-agent-learning/project-demo/target/classes com.cz.demo.apm.LogApplicationMain
agent premain attach success
main project start
rpc invoke
public void com.cz.demo.apm.client.RpcClient.invoke() 执行耗时：1900ms
db invoke
public void com.cz.demo.apm.client.DbClient.invoke() 执行耗时：2885ms
redis invoke
public void com.cz.demo.apm.client.RedisClient.invoke() 执行耗时：1235ms
do business invoke
public void com.cz.demo.apm.client.BusinessManager.doBusiness() 执行耗时：6089ms

Process finished with exit code 0

```


### 2. 基于agentmain实现的class文件热更新
1、创建agentmain类
``` java
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
```
2、创建Attach程序
``` java 
public class HotDeployAttach {
    public static void main(String[] args)
        throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            if (descriptor.displayName().equals("com.cz.demo.hotdeploy.service.ApplicationMain")) {
                VirtualMachine virtualMachine = VirtualMachine.attach(descriptor.id());
                virtualMachine.loadAgent("/Users/dzsb-002298/project/java-agent-learning/agent-hot-deploy/target/agent-hot-deploy-1.0-SNAPSHOT.jar");
                virtualMachine.detach();
            }
        }
    }
}
```
3、模拟业务程序
``` java
public class ApplicationMain {
    public static void main(String[] args) throws InterruptedException {
        PayService payService = new PayService();
        while (true) {
            payService.pay("wechat");
        }
    }
}

public class PayService {
    private final static Random random = new Random();

    public void pay(String channel) throws InterruptedException {

        TimeUnit.MILLISECONDS.sleep(random.nextInt(5000));
        System.out.println("channel:" + channel + "invoke pay");
    }
}
```

4、我们先执行业务程序ApplicationMain看到日志
```
/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=61703:/Applications/IntelliJ IDEA.app/Contents/bin -Dfile.encoding=UTF-8 -classpath /Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/lib/tools.jar:/Users/dzsb-002298/project/java-agent-learning/project-demo/target/classes com.cz.demo.hotdeploy.service.ApplicationMain
channel:wechatinvoke pay
channel:wechatinvoke pay
channel:wechatinvoke pay
channel:wechatinvoke pay
```
5、再对PayService类进行修改，如下添加了一个方法addMethod()
``` java
public class PayService {
    private final static Random random = new Random();

    public void pay(String channel) throws InterruptedException {

        TimeUnit.MILLISECONDS.sleep(random.nextInt(5000));
        System.out.println("channel:" + channel + "invoke pay");
        addMethod();
    }

    private static void addMethod(){
        System.out.println("add method!");
    }
}
```

6、执行attach程序`HotDeployAttach`,日志中可以看到addMethod日志的打印，说明class已经在程序运行时加载成功了
```
channel:wechatinvoke pay
channel:wechatinvoke pay
agentmain start
redefine success
channel:wechatinvoke pay
channel:wechatinvoke pay
add method!
channel:wechatinvoke pay
add method!
```

以上两个例子的完整代码上传到了：https://github.com/ZuckerChen/java-agent-learning

## 写在最后
- 此篇文章只是对Java Agent做了最基本的介绍和最简单的用法，目的是拓宽视野，碰到问题是能有更多的解决方案，如果要在生产上使用Java Agent那肯定还需要系统的学习insurment和classfile操作 。  
- 我个人认为Java Agent的核心优势是没有代码侵入性，可以很方便的进行装载、拆卸、升级，尤其适合做监控类的辅助功能。大家也可以自己思考下我们还可以用Java Agent来做什么。  


全文终！
