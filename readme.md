<div align=center>
<img width="200px;" src="http://pan.sudoyc.com:7878/apps/files_sharing/publicpreview/zsW4eHSPx9DKt8P?x=3710&y=1192&a=true&file=logo.png&scalingup=0"/>
</div>

<br/>

<div align=center>
  <img src="https://img.shields.io/badge/licenes-MIT-blue.svg"/>
  <img src="https://img.shields.io/badge/jdk-1.8-green.svg"/>
  <a href="https://mvnrepository.com/artifact/org.fastlight/fastaop" target="_blank">
      <img src="https://img.shields.io/badge/release-1.0.1-brightgreen.svg"/>
  </a>
</div>
<div align=center style="margin:10px 0px 10px 0px">
Java lightweight AOP framework
</div>
<div align=center >
  <span><a href="/readme-zh.md">中文文档</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href="/readme.md">English</a></span>
</div>

## Intro

A lightweight AOP framework based on Java Annotation Processing, Its principle is similar to that of Lombok

## Features

- 📦 Out of the box, compatible with any project
- 🚀 Based on Java Annotation Processing, high-performance
- ⚡️ Suitable for arbitrary modifiers methods, like private,protected,static etc..

## Guide

  please visit [FastAop Guide](http://fastlight.org).

## Development

```
$ mvn clean install
```

IDEA setting

```
setting->build->compiler->Shared build process VM options: -Djps.track.ap.dependencies=false
```

## Note

FastAop currently only supports Java 8, and does not support other versions at the moment. Later, it will gradually support and adapt to Java 9+
