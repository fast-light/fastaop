<div align=center>
<img width="200px;" src="http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/5aGw56qsdoJ6XTs?x=3710&y=1192&a=true&file=logo2.png&scalingup=0"/>
</div>

<br/>

<div align=center>
<img src="https://img.shields.io/badge/licenes-MIT-brightgreen.svg"/>
<img src="https://img.shields.io/badge/jdk-1.8-brightgreen.svg"/>
<img src="https://img.shields.io/badge/release-master-brightgreen.svg"/>
</div>

<div align=center>
Java 高性能 AOP 框架
</div>

## 框架简介

FastAop 是一款基于 Java Annotation Processing 的 AOP 框架，其原理和 Lombok 类似，通过对编译过程的拦截，修改方法的语法树并织入切面代码从而实现了 AOP 的功能，相较于传统的
AspectJ、Spring-AOP 框架有如下特点：

1. 依赖干净，无需 Spring 等环境
1. 使用简单，仅需两个注解就能实现切面功能
1. 性能好，由于是编译过程中植入原生代码，所以性能几乎无损
1. 功能强大，支持 private、static 等各种方法切面，内部方法相互调用也会过切面逻辑
1. 扩展性好，提供了特定注解，能够在方法内部拿到当前切面上下文，便于做一些临时操作

## 使用

### 一、引入依赖

> Maven 中央仓库还在审核，暂时先手工编译

```xml

<dependency>
    <groupId>org.fastlight</groupId>
    <artifactId>fastaop</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 添加切面

这里仅拦截了方法执行前和执行后，分别打印了入参和出参，同时输出了方法耗时，其关键元素如下：

1. @FastAspectMark 标记切面逻辑
   
   > 标记的类必须含有无参构造函数，执行的时候会以单例模式运行
1. 实现 FastAspectHandler 接口，拦截切面的生命周期
   
   > 可以拦截方法的执行前、返回语句、发生异常、执行后等生命周期
1. getOrder() 来决定多个切面逻辑的执行顺序
   
   > order 小的先执行

```java

@FastAspectMark
public class LogHandler implements FastAspectHandler {
    public static final String START_MS = "log.start";

    @Override
    public boolean support(FastAspectContext ctx) {
        return true;
    }

    @Override
    public void preHandle(FastAspectContext ctx) {
        ctx.addExtension(START_MS, System.currentTimeMillis());
        System.out.printf("[preHandle] [%s.%s] [input]==> %s \n",
                ctx.getMetaMethod().getMetaOwner().getType(),
                ctx.getMetaMethod().getName(), ctx.getParamMap()
        );
    }

    @Override
    public void postHandle(FastAspectContext ctx) {
        Long cost = System.currentTimeMillis() - (Long) ctx.getExtension(START_MS);
        System.out.printf("[postHandle] [%s.%s] [output]==> %s [cost]==> %sms \n",
                ctx.getMetaMethod().getMetaOwner().getType(),
                ctx.getMetaMethod().getName(),
                ctx.getReturnVal(),
                cost
        );
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
```

### 二、使用切面

使用切面的方法如下：

1. 在需要切入的 Class 或者 Method 上面添加 @FastAspect 即可
1. 可通过 @FastAspectVar 直接在方法内部使用切面上下文

```java
public class AopExample {
    public static void main(String[] args) {
        System.out.println("==>invoked: " + hello("[FastAop]"));
    }

    @FastAspect
    public static String hello(String name) {
        System.out.println("[hello] [input]==> " + name);
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        return "hello-->>" + ctx.getArgs()[0] + "eq(" + (ctx.getArgs()[0] == name) + ")";
    }
}
// output
// [preHandle] [class org.fastlight.fastaop.example.AopExample.hello] [input]==> {name=[FastAop]}
// [hello] [input]==> [FastAop]
// [postHandle] [class org.fastlight.fastaop.example.AopExample.hello] [output]==> hello-->>[FastAop]eq(true) [cost]==> 9ms
// ==>invoked: hello-->>[FastAop]eq(true)
```

## 切面上下文

FastAspectContext 内部属性分为动态属性和静态属性，其中动态属性是方法执行的时候注入的，静态属性为元数据方法在编译的时候就确定的

### 动态属性

| 属性       | 描述                                                         |
| :--------- | ------------------------------------------------------------ |
| owner      | 方法所有者，对于 Bean 就是 this，对于静态方法为 null         |
| returnVal  | 返回值，仅在 returnHandle 和 postHandle 里面能获取           |
| args       | 入参值                                                       |
| extensions | 上下文扩展，仅在本次执行有效，比如上文的 LogHandler 利用它实现了耗时打印 |
| ctrlFlow   | 用于控制方法流程，比如可以在 preHandle 里面直接终止方法运行  |

### 静态属性

FastAspectContext#getMetaMethod()

| 属性           | 描述                                               |
| -------------- | -------------------------------------------------- |
| cacheIndex     | 方法元数据缓存索引                                 |
| isStatic       | 是否为静态方法                                     |
| name           | 方法名字                                           |
| returnType     | 返回类型                                           |
| metaOwner      | 方法所在类的元数据（含类型、类上面的注解）         |
| parameters     | 方法入参元数据（含参数名称和参数上面的注解）       |
| annotations    | 方法上面的注解信息                                 |
| method         | 反射获取的方法信息，有缓存                         |
| metaExtensions | 元素数据扩展，生命周期为全局，仅在当前 Method 可见 |

## 原理

### 1. 切面代码织入

对比 AopExample 编译前和编译后的两段代码，可以知道通过 @FastAspect 对 hello 方法进行了相关的改变，主要是在方法执行的各个生命周期回调了切面的相关接口

![原理](http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/fqMTNMmHwJFa6xC?x=3710&y=1192&a=true&file=%25E5%258E%259F%25E7%2590%2586.png&scalingup=0)

### 2. 切面逻辑注入

```
切面逻辑是通过 @FastAspectMark 标记的，其功能是在 META-INF/services/org.fastlight.aop.handler.FastAspectHandler 
注入了标记的服务这里是 org.fastlight.fastaop.example.handler.LogHandler
```

```java
@FastAspectMark
public class LogHandler implements FastAspectHandler {
	//...
}
```

织入的切面代码默认调用的是 FastAspectSpiHandler，它在启动的时候去加载上面注入的 SPI 元素，并在切面逻辑中按 order 的顺序依次调用，其初始化方法如下：

```java
/**
 * 通过 SPI 注入 Handlers
 */
public void initHandlers() {
  if (isInit) {
    return;
  }
  synchronized (initLock) {
    if (isInit) {
      return;
    }
    try {
      spiHandlers.clear();
      ServiceLoader<FastAspectHandler> serviceLoader = ServiceLoader.load(FastAspectHandler.class);
      for (FastAspectHandler handler : serviceLoader) {
        // 防止重复添加
        if (spiHandlers.stream().noneMatch(v -> v.getClass().equals(handler.getClass()))) {
          spiHandlers.add(handler);
        }
      }
      spiHandlers.sort(Comparator.comparingInt(FastAspectHandler::getOrder));
    } finally {
      isInit = true;
    }
  }
}
```

