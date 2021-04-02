<div align=center>
<img width="200px;" src="http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/zsW4eHSPx9DKt8P?x=3710&y=1192&a=true&file=logo.png&scalingup=0"/>
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

FastAop 是一款基于 Java Annotation Processing 的 AOP 框架，其原理和 Lombok 类似，通过对编译过程的拦截，修改方法的语法树并织入切面代码从而实现了 AOP 的功能，相较于传统的 AspectJ、Spring-AOP 框架有如下特点：

1. ✨依赖干净，无需 Spring 等环境

1. ✨使用简单，仅需两个注解就能实现切面功能

1. ✨性能好，由于是编译过程中植入原生代码，所以性能几乎无损

1. ✨功能强大，支持 private、static 等各种方法切面，内部方法相互调用也会过切面逻辑

1. ✨扩展性好，提供了特定注解，能够在方法内部拿到当前切面上下文，便于做一些临时操作

   > @FastAspectVar
   >  FastAspectContext ctx = FastAspectContext.currentContext();

1. ✨支持 Around 模式，使用上和 AspectJ 类似，能够完整控制方法的执行逻辑

1. ✨可以基于此工程在编译期间生成任何模板代码

## 使用

### 一、引入依赖
1. 如果 IDEA 报空指针，配置如下：setting->build->compiler->Shared build process VM options
> -Djps.track.ap.dependencies=false
2. 暂时手工编译，Maven 中央仓库还未发布

```xml

<dependency>
    <groupId>org.fastlight</groupId>
    <artifactId>fastaop</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 二、添加切面

这里仅拦截了方法执行前和执行后，分别打印了入参和出参，同时输出了方法耗时，其关键元素如下：

1. @FastAspectAround 标记切面逻辑
   
   > 标记的类必须含有无参构造函数，执行的时候会以单例模式运行
1. 实现 FastAspectHandler 接口，覆盖 processAround 方法
   
   > 原方法为 ctx.proceed(...args)，如果不注入 args 那么以原始参数执行原方法
1. getOrder() 来决定多个切面逻辑的执行顺序
   
   > order 小的先执行

```java
/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@FastAspectAround
public class LogHandler implements FastAspectHandler {

    @Override
    public Object processAround(FastAspectContext ctx) throws Exception {
        System.out.printf("[processAround] %s.%s \n", ctx.getMetaMethod().getMetaOwner().getType().getName(),
            ctx.getMetaMethod().getName()
        );
        return ctx.proceed();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

```

### 三、使用切面

使用切面的方法如下：

1. 在需要切入的 Class 或者 Method 上面添加 @FastAspect 即可
1. 可通过 @FastAspectVar 直接在方法内部使用切面上下文

```java
/**
 * 一个简单的 FastAop 调用 demo
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class AopExample {
    public static void main(String[] args) {
        String fastAop = hello("[FastAop]");
        System.out.println("hello: " + fastAop);
    }

    @FastAspect
    public static String hello(String name) {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        return String.valueOf(ctx.getArgs()[0]);
    }
}
// output
// [processAround] org.fastlight.fastaop.example.AopExample.hello 
// hello: [FastAop]
```

## 切面上下文

FastAspectContext 内部属性分为动态属性和静态属性，其中动态属性是方法执行的时候注入的，静态属性为元数据方法在编译的时候就确定的

### 动态属性

| 属性       | 描述                                                         |
| :--------- | ------------------------------------------------------------ |
| owner      | 方法所有者，对于 Bean 就是 this，对于静态方法为 null         |
| args       | 入参值                                                       |
| extensions | 上下文扩展，仅在本次执行有效，比如上文的 LogHandler 利用它实现了耗时打印 |

### 静态属性

FastAspectContext#getMetaMethod()

| 属性           | 描述                                                 |
| -------------- | ---------------------------------------------------- |
| isStatic       | 是否为静态方法                                       |
| name           | 方法名字                                             |
| returnType     | 返回类型                                             |
| metaOwner      | 方法所在类的元数据（含类型、类上面的注解）           |
| parameters     | 方法入参元数据（含参数名称和参数上面的注解）         |
| annotations    | 方法上面的注解信息                                   |
| method         | 反射获取的方法信息，有缓存仅静态初始化的时候执行反射 |
| metaExtensions | 元数据扩展，生命周期为全局，仅在当前 Method 可见     |

## 最佳实践

这里通过切面逻辑实现了修复一个 add 方法的运算，且仅仅针对于标注了 @CalcRepair 的方法做修复

![practice](http://pan.sudoyc.com:7878/s/Y69Xg7QtNXYGrPk/download)

## 原理

通过在编译的时候拦截「注解处理」过程，对标记的方法和类注入切入代码，其核心代码为：

```java
if (__fast_context.hasNextHandler()) {
    return (Integer)__fast_context.invoke(new Object[0]);
}
```

@FastAspectAround 是标记切面逻辑为一个 SPI 服务，通过 __fast_context.invoke 去递归调用切面服务，从而实现了 around 拦截

> 方法原始逻辑也被算作一个切面服务，且被最后执行，如果有切面没有调用 ctx.proceed() 那么原始方法不会被执行，整个递归逻辑会立刻返回

![decompile](http://pan.sudoyc.com:7878/s/Zbr4nb55pfoLgjP/download)