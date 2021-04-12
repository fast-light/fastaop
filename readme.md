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

FastAop 是一款基于 Java Annotation Processing 的轻量级 AOP 框架，其原理和 Lombok 类似，通过对编译过程的拦截，修改方法的语法树并织入切面代码从而实现了 AOP 的功能。

> FastAop 对运行时无要求，无须 Spring，AspectJ，CGLib 等特殊依赖。

## 使用

### 一、引入依赖

```xml
<dependency>
  <groupId>org.fastlight</groupId>
  <artifactId>fastaop</artifactId>
  <version>1.0.1</version>
</dependency>
```
### 二、添加切面
#### 2.1 添加切面逻辑
```java
/**
 * 打印方法的调用时间
 */
@Target(ElementType.METHOD)
public @interface LogAccess {
}

@FastAspectAround(support = LogAccess.class, order=1)
public class LogHandler implements FastAspectHandler {
    /**
     * 环绕模式切入方法体
     */
    @Override
    public Object processAround(FastAspectContext ctx) throws Exception {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.printf("[%s] -- [%s.%s]\n", time,
            ctx.getMetaMethod().getMetaOwner().getType().getName(),
            ctx.getMetaMethod().getName()
        );
        return ctx.proceed();
    }
}
```
#### 2.2 加入切面注解
在 src/main/resources/META-INF/aspect/fast.aspect.supports.txt 加入 LogAccess 注解全路径

```java
// 替换成自己的注解路径
org.fastlight.fastaop.example.handler.LogAccess
```

### 三、使用切面

```java

@LogAccess
public static void main(String[] args) {
  System.out.println("[FastAop Hello]");
}
//输出
//[2021-04-10 15:42:03] -- [org.fastlight.fastaop.example.AopExample.main]
//[FastAop Hello]  
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
public class AopExample {
    private static final MetaType __fast_meta_owner = MetaType.create(...);
    private static final MetaMethod[] __fast_meta_method = new MetaMethod[]{...};
  
    // @FastMarkedMethod(..) 为编译添加注解，方便 Method 精准定位
    @FastMarkedMethod(0)
    @LogAccess
    public static void main(String[] args) {
      // 1. 生成切面上下文 
      FastAspectContext __fast_context = FastAspectContext.create(__fast_meta_method[0], (Object)null, new Object[]{args});
      // 2. 调用切面逻辑 
      if (__fast_context.support()) {
            __fast_context.invoke(new Object[0]);
            return;
         }
      // 3. ctx.proceed() 会回调原始逻辑
      System.out.println("[FastAop Hello]");
    }

}

```

