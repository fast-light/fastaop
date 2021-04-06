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

> 如果 在 IDEA 里面运行 example 报错，请在项目根目录先执行 mvn clean 再去 IDEA 里面运行

```xml

<dependency>
    <groupId>org.fastlight</groupId>
    <artifactId>fastaop</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 二、添加切面

这里使用了一个简单的 LogHandler 去打印了方法信息，关注点如下：

1. @FastAspectAround 标记切面逻辑
   
   > 标记的类必须含有无参构造函数，执行的时候会以单例模式运行
1. 实现 FastAspectHandler 接口，覆盖 processAround 方法
   
   > 原方法为 ctx.proceed(...args)，如果不注入 args 那么以原始参数执行原方法
1. getOrder() 来决定多个切面逻辑的执行顺序

   > order 小的先执行

1. support() 结果对于每个 MetaMethod 都会全局缓存用于提升切面执行效率

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

    /**
     * 判断是否切入某个方法，该 support 决定了后面的 processAround 是否被调用，结果会被缓存（提高执行效率）
     * 如果想动态调整对某个方法的支持，请返回 true，且在 processAround 进行判断
     * 默认返回为 true
     */
    @Override
    public boolean support(MetaMethod metaMethod) {
        return true;
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
| extensions | 上下文扩展，仅在本次执行有效 |

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

```java
/**
 * 修复一个损坏的计算器
 *
 * @author ychost@outlook.com
 * @date 2021-04-02
 */
@FastAspect
public class FastCalculatorTest {

    /**
     * 单测入口
     */
    @Test
    public void calcTest() {
        int res = add(3, 2);
        Assert.assertEquals(5, res);
    }

    /**
     * 待修复的加法逻辑
     */
    @CalcRepair
    int add(int a, int b) {
        throw new RuntimeException("this is a broken calculator");
    }

    /**
     * 修复注解
     */
    @Target(ElementType.METHOD)
    public @interface CalcRepair {
    }

    /**
     * 修复 calc 的切面
     */
    @FastAspectAround
    public static class CalcRepairHandler implements FastAspectHandler {
        @Override
        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(CalcRepair.class);
        }

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            int a = (int)ctx.getArgs()[0];
            int b = (int)ctx.getArgs()[1];
            return a + b;
        }
    }
}
```



## 原理

通过在编译的时候拦截「注解处理」过程，对标记的方法和类注入切入代码，其核心代码为：

```java
 if (__fast_context.support()) {
        __fast_context.invoke(new Object[0]);
 }
```

@FastAspectAround 是标记切面逻辑为一个 SPI 服务，通过 __fast_context.invoke 去递归调用切面服务，从而实现了 around 拦截

> 方法原始逻辑也被算作一个切面服务，且被最后执行，如果有切面没有调用 ctx.proceed() 那么原始方法不会被执行，整个递归逻辑会立刻返回

```java
/**
 * 反编译后的代码，隐藏了元数据 create 细节
 */
public class FastCalculatorTest {
    private static final MetaType __fast_meta_owner = MetaType.create(...);
    private static final MetaMethod[] __fast_meta_method;

    public FastCalculatorTest() {
    }

    //@FastMarkedMethod(0) 为编译添加，用于精准定位 Method
    @Test
    @FastMarkedMethod(0)
    public void calcTest() {
        // 编译新增代码，所有标记的方法都会埋入这几行代码
        // 1. 生产切面上下文
        FastAspectContext __fast_context = FastAspectContext.create(__fast_meta_method[0], this, new Object[0]);
        // 2. 检查是否有切面支持该方法
        if (__fast_context.support()) {
            // 3. 调用切面逻辑，里面是递归调用，最终会根 MetaMethod 的 ThreadLocal 索引条件
            // 再调用最多 1 次 calcTest() 且 __fast_context.support() 返回 false，执行原始代码
            // 如果切面逻辑里面没有调用 ctx.proceed() 那么，原始代码不会被执行，逻辑会立即 return
            __fast_context.invoke(new Object[0]);
            return;
        } 
        // 原始代码
        int res = this.add(3, 2);
        Assert.assertEquals(5L, (long)res);   
    }

    @FastMarkedMethod(1)
    @FastCalculatorTest.CalcRepair
    int add(int a, int b) {
        FastAspectContext __fast_context = FastAspectContext.create(__fast_meta_method[1], this, new Object[]{a, b});
        if (__fast_context.support()) {
            return (Integer)__fast_context.invoke(new Object[0]);
        } else {
            throw new RuntimeException("this is a broken calculator");
        }
    }

    static {
        __fast_meta_method = new MetaMethod[]{...}
    }
    public static class CalcRepairHandler implements FastAspectHandler {
        public CalcRepairHandler() {
        }

        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(FastCalculatorTest.CalcRepair.class);
        }

        public Object processAround(FastAspectContext ctx) throws Exception {
            int a = (Integer)ctx.getArgs()[0];
            int b = (Integer)ctx.getArgs()[1];
            return a + b;
        }
    }

    @Target({ElementType.METHOD})
    public @interface CalcRepair {
    }
}
```

