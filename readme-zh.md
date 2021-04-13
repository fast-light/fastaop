<div align=center>
<img width="200px;" src="http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/zsW4eHSPx9DKt8P?x=3710&y=1192&a=true&file=logo.png&scalingup=0"/>
</div>
<br/>

<div align=center>
  <img src="https://img.shields.io/badge/licenes-MIT-blue.svg"/>
  <img src="https://img.shields.io/badge/jdk-1.8-green.svg"/>
  <a href="https://mvnrepository.com/artifact/org.fastlight/fastaop">
      <img src="https://img.shields.io/badge/release-1.0.1-brightgreen.svg"/>
  </a>
</div>

<div align=center>
Java 轻量级 AOP 框架
</div>

<div align=center>
  <span><a href="/readme-zh.md">中文文档</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href="/readme.md">English</a></span>
</div>


## 简介

FastAop 是一款基于 Java Annotation Processing 的轻量级 AOP 框架，其原理和 Lombok 类似

## 特性

- 📦 开箱即用，适用于任意项目
- 🚀 基于 Java Annotation Processing，运行时无性能损耗
- ⚡️ 兼容 private/protected/static 等各种修饰符的方法

## 使用

 [FastAop 使用教程](http://doc.fastlight.org:7878/zh-CN).

## 编译项目

```
$ mvn clean install
```

IDEA 配置

```
setting->build->compiler->Shared build process VM options: -Djps.track.ap.dependencies=false
```

## 说明

FastAop 目前仅支持 Java8，暂不支持其它版本，后面会逐步对 Java9+ 进行支持适配