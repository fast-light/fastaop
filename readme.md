<div align=center>
<img width="200px;" src="http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/Qr46Q49Gg3gDWqH?x=3710&y=1192&a=true&file=logo.png&scalingup=0"/>
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

FastAop 是一款基于 Java Annotation Processing 的 AOP 框架，其原理和 Lombok 类似，通过对编译过程的拦截，修改方法的语法树 并织入切面代码从而实现了 AOP 的功能，相较于传统的
AspectJ、Spring-AOP 框架有如下特点：

1. 依赖干净，无需 Spring 等环境
1. 使用简单，仅需两个注解就能实现切面功能
1. 性能好，由于是编译过程中植入原生代码，所以性能几乎无损
1. 功能强大，支持 private、static 等各种方法切面，内部方法相互调用也会过切面逻辑
1. 扩展性好，提供了特定注解，能够在方法内部拿到当前切面上下文，便于做一些临时操作

## 使用

### 一、引入依赖

```xml

<dependency>
    <groupId>org.fastlight</groupId>
    <artifactId>fastaop</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 添加切面

这里仅拦截了方法执行前和执行后，分别打印了入参和出参，同时输出了方法耗时，切面逻辑有如下要点：

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