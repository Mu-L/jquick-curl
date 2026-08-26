# JQuickCurl

[![GitHub Stars](https://img.shields.io/github/stars/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/network/members)
[![License](https://img.shields.io/github/license/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.paohaijiao/jquick-curl?style=flat-square)](https://central.sonatype.com/artifact/io.github.paohaijiao/jquick-curl)
[![Awesome Java](https://awesome.re/badge.svg)](https://github.com/akullpp/awesome-java)

**简体中文** | [English](./README-EN.md)

JQuickCurl 是一款面向 Java 的 curl 命令式 HTTP 客户端框架：把浏览器、Postman 或终端中可复用的原生 curl 命令直接解析为 Java 请求，支持注解与 XML 双配置、变量替换、条件渲染、文件传输、批量执行和动态代理调用。

> 归属 [Dromara](https://dromara.org/)，主仓库：[dromara/jquick-curl](https://github.com/dromara/jquick-curl)。

## 目录

- [核心优势](#核心优势)
- [适用场景](#适用场景)
- [快速开始](#快速开始)
- [核心功能详解](#核心功能详解)
- [完整使用示例](#完整使用示例)
- [高级特性](#高级特性)
- [项目架构设计](#项目架构设计)
- [更新日志与版本特性](#更新日志与版本特性)
- [开源协议](#开源协议)
- [贡献指南](#贡献指南)
- [项目归属与支持](#项目归属与支持)
- [Awesome Java](#awesome-java)

## 核心优势

| 对比维度 | JQuickCurl | OkHttp / RestTemplate / HttpClient |
| --- | --- | --- |
| 请求表达 | 直接复用原生 curl 命令 | 需要手写 Request、参数、Header 或客户端配置 |
| 调试协作 | 后端、前端、测试可共享同一条 curl | 常需要在不同表达方式之间转换 |
| 配置方式 | `@JCurlCommand` 注解 + XML 配置 | 以 Java API、Builder 或 Spring 配置为主 |
| 动态请求 | 原生命令中使用变量和 XML 条件语法 | 通常需要手动拼装 URL、Body 和条件分支 |
| API 封装 | 接口 + 动态代理即可调用 | 需要手动封装服务类或模板代码 |
| 文件与方法 | 覆盖常用 HTTP 方法、multipart、下载 | 能力强，但请求描述与业务代码耦合度更高 |

核心创新是 **原生 curl 命令解析**、**零手写请求构建代码** 和 **XML 动态条件语法**。JQuickCurl 底层使用 OkHttp 执行网络请求，并通过 ANTLR 解析 curl 语法，不是调用系统中的 curl 进程。

## 适用场景

- 将 Postman、浏览器开发者工具或接口文档中的 curl 快速迁移到 Java。
- 微服务、支付、数据采集、第三方开放平台等大量 HTTP 集成场景。
- 需要把请求定义与 Java 业务代码分离的配置化 API 客户端。
- 需要统一处理认证变量、环境域名、条件 Header 和请求体的项目。
- 文件上传、批量接口调用、文件下载和接口自动化测试。

## 快速开始

### Maven 依赖

当前项目版本为 `2.2.0`：

```xml
<dependency>
    <groupId>io.github.paohaijiao</groupId>
    <artifactId>jquick-curl</artifactId>
    <version>${version}</version>
</dependency>
```

### 最简示例

`@JCurlCommand` 用于在接口方法上声明 curl。代理对象负责解析、执行并把响应转换为返回类型。

```java
import com.github.paohaijiao.anno.JCurlCommand;
import com.github.paohaijiao.domain.req.JQuickCurlReq;
import com.github.paohaijiao.executor.JCurlInvoker;

public interface EchoApi {
    @JCurlCommand("curl -X GET https://httpbin.org/get")
    String get(JQuickCurlReq request);
}

class Application {
    public static void main(String[] args) throws Exception {
        EchoApi api = JCurlInvoker.createProxy(EchoApi.class);
        String response = api.get(new JQuickCurlReq());
        System.out.println(response);
    }
}
```

## 核心功能详解

### 1. 注解方式

适合请求数量较少、请求定义与代码紧密关联的场景：

```java
public interface UserApi {
    @JCurlCommand("curl -X GET 'https://api.example.com/users/${id}'")
    String getUser(JQuickCurlReq request);
}

JQuickCurlReq request = new JQuickCurlReq();
request.put("id", 1001);
UserApi api = JCurlInvoker.createProxy(UserApi.class);
String body = api.getUser(request);
```

`@JCurlCommand` 还支持 `execute`、`expectedStatus`、`expectedBusinessStatus` 和 `validationScript` 等执行与校验属性。

### 2. XML 配置方式

适合集中维护大量接口，将 curl、返回类型和 Java 接口解耦。XML 使用项目内置 DTD：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE curls PUBLIC "-//PAOHAIJIAO//DTD API CURL 1.0//EN"
        "classpath:paohaijiao/dtd/Jquick-curl.dtd">
<curls namespace="com.example.UserApi">
    <curl name="getUser" returnClass="java.lang.String">
        curl -X GET https://api.example.com/users/#{id}
    </curl>
</curls>
```

Java 接口的方法名必须与 `<curl name="...">` 一致：

```java
import com.github.paohaijiao.domain.req.JQuickCurlReq;

public interface UserApi {
    String getUser(JQuickCurlReq request);
}
```

加载 XML 并创建代理：

```java
import com.github.paohaijiao.xml.JQuickCurlXmlParseFactory;
import com.github.paohaijiao.xml.factory.JQuickXmlFactory;
import com.github.paohaijiao.xml.handler.JQuickParseHandler;

JQuickParseHandler parser = new JQuickCurlXmlParseFactory();
JQuickXmlFactory factory = new JQuickXmlFactory(parser, "apis.xml");
UserApi api = factory.createApi(UserApi.class);
```

### 3. 变量替换

- 注解代理结合 `JQuickCurlReq` 时，使用 `${name}` 从请求参数容器取值。
- XML 代理支持上下文变量，常用 `#{name}`，并可与方法参数或请求上下文绑定。

```java
public interface AuthApi {
    @JCurlCommand("curl -u ${user}:${password} https://api.example.com/me")
    String currentUser(JQuickCurlReq request);
}

JQuickCurlReq request = new JQuickCurlReq();
request.put("user", "demo");
request.put("password", "secret");
String result = JCurlInvoker.createProxy(AuthApi.class).currentUser(request);
```

请勿把真实密码、Token 或私钥提交到源码和 XML；建议在运行时注入变量。

### 4. XML 条件语法

XML curl 文本中可使用 `<if test="...">...</if>` 条件片段，条件成立时才渲染其中的 Header、参数或命令选项：

```xml
<curl name="search" returnClass="java.lang.String">
    curl -X GET #{host}/search
    <if test="withTrace == true"> -H "X-Trace: #{traceId}" </if>
</curl>
```

条件表达式应使用 XML 可解析的属性名和上下文变量。复杂条件建议拆分为多个 `<if>`，保持请求定义可读。

### 5. 文件上传与下载

- `-F "file=@/path/to/file"`：单文件上传。
- 多个 `-F`：同名字段上传多个文件或混合普通表单字段。
- `--output` / `-o`：执行器将响应字节写入指定文件；Java 返回值也可使用 `byte[]`。

### 6. 批量请求

给同一个类中的多个无参方法标注 `@JCurlCommand`，即可通过 `JQuickCurlBatchRunner` 批量执行：

```java
public class BatchCommands {
    @JCurlCommand("curl -X GET https://httpbin.org/get")
    public String first() { return null; }

    @JCurlCommand("curl -X GET https://httpbin.org/uuid")
    public String second() { return null; }
}

JQuickCurlBatchRunner runner = new JQuickCurlBatchRunner();
List<JQuickCurlResponseBody> results = runner.runCurlCommands(
        new BatchCommands(), JQuickCurlResponseBody.class);
```

### 7. 动态代理与 Lambda/方法引用调用

注解接口通过 `JCurlInvoker.createProxy` 调用；已有带 `@JCurlCommand` 的方法也可以使用 `JCurlInvoker.invoke` 和方法引用执行，并指定返回类型。

## 完整使用示例

下面的接口覆盖 GET、POST、PUT、PATCH、DELETE、HEAD、OPTIONS、TRACE、文件上传、文件下载和混合表单参数。示例地址仅用于演示，请替换为实际服务地址。

```java
import com.github.paohaijiao.anno.JCurlCommand;
import com.github.paohaijiao.domain.req.JQuickCurlReq;

public interface CompleteApi {
    @JCurlCommand("curl -X GET https://api.example.com/users")
    String get(JQuickCurlReq request);

    @JCurlCommand("curl -X POST https://api.example.com/users -H 'Content-Type: application/json' -d '{\"name\":\"Ada\"}'")
    String post(JQuickCurlReq request);

    @JCurlCommand("curl -X PUT https://api.example.com/users/1 -H 'Content-Type: application/json' -d '{\"name\":\"Ada Lovelace\"}'")
    String put(JQuickCurlReq request);

    @JCurlCommand("curl -X PATCH https://api.example.com/users/1 -H 'Content-Type: application/json' -d '{\"active\":true}'")
    String patch(JQuickCurlReq request);

    @JCurlCommand("curl -X DELETE https://api.example.com/users/1")
    Void delete(JQuickCurlReq request);

    @JCurlCommand("curl -X HEAD https://api.example.com/users/1")
    Void head(JQuickCurlReq request);

    @JCurlCommand("curl -X OPTIONS https://api.example.com/users/1")
    String options(JQuickCurlReq request);

    @JCurlCommand("curl -X TRACE https://api.example.com/trace -H 'Content-Type: text/plain' -d 'trace'")
    String trace(JQuickCurlReq request);

    @JCurlCommand("curl -X POST https://api.example.com/files -F 'file=@./example.txt'")
    String upload(JQuickCurlReq request);

    @JCurlCommand("curl -X GET https://api.example.com/files/example.txt --output './download/example.txt'")
    byte[] download(JQuickCurlReq request);

    @JCurlCommand("curl -X POST https://api.example.com/import -F 'userId=1001' -F 'description=example' -F 'file=@./example.txt'")
    String uploadWithForm(JQuickCurlReq request);
}
```

调用方式：

```java
CompleteApi api = JCurlInvoker.createProxy(CompleteApi.class);
JQuickCurlReq request = new JQuickCurlReq();
System.out.println(api.get(request));
System.out.println(api.post(request));
byte[] file = api.download(request);
```

## 高级特性

### 超时、重试、重定向与连接池

`JQuickCurlConfig` 是全局配置单例，支持超时、连接池、失败重试、重定向和拦截器配置：

```java
import com.github.paohaijiao.config.JQuickCurlConfig;
import java.util.concurrent.TimeUnit;

JQuickCurlConfig.getInstance()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .maxRetryCount(2)
        .followRedirects(true);
```

方法级别也可使用 `@JTimeout(connect = ..., read = ..., write = ...)` 覆盖超时设置。

### 拦截器

拦截器采用 OkHttp `Interceptor`，可统一记录日志、注入请求头或处理响应：

```java
import com.github.paohaijiao.config.JQuickCurlConfig;
import okhttp3.Interceptor;

Interceptor auth = chain -> chain.proceed(
        chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + System.getenv("API_TOKEN"))
                .build());

JQuickCurlConfig.getInstance().addInterceptor(auth);
```

### 批量执行

使用 `JQuickCurlBatchRunner.runCurlCommands(Object, Class<T>)` 扫描命令类并返回结果列表；需要方法无参且标注 `@JCurlCommand`。`@JTimeout` 可用于单个批量命令。

### 代理工厂

- 注解代理：`JCurlInvoker.createProxy(Api.class)`。
- XML 代理：`new JQuickXmlFactory(new JQuickCurlXmlParseFactory(), "apis.xml").createApi(Api.class)`。
- 方法引用：`JCurlInvoker.invoke(Service::method, request, ReturnType.class)`。

## 项目架构设计

```text
curl 字符串 / 注解 / XML
          |
          v
  ANTLR Lexer + Parser       <- 解析 curl 语法
          |
          v
  Visitor + JContext         <- 变量、条件和请求上下文
          |
          v
  OkHttp 执行层              <- 连接池、超时、重试、拦截器
          |
          v
  JQuickCurlResponseBody     <- 原始响应
          |
          v
  ResponseConvert / ResultFactory <- String、对象、集合、byte[] 等
```

核心模块包括 `anno`（注解）、`parser`（ANTLR 解析器）、`visitor`（命令访问器）、`executor`（执行器）、`xml`（XML 代理）、`handler`（动态代理）、`result`（响应转换）和 `config`（全局配置）。

## 更新日志与版本特性

### 2.1.0

- 完善 curl 命令解析与 HTTP 请求执行链路。
- 支持注解代理、XML 配置代理和方法引用调用。
- 支持变量替换、XML 条件渲染、文件上传下载和批量执行。
- 支持超时、重试、重定向、连接池及 OkHttp 拦截器配置。

更多版本信息请查看 [Releases](https://github.com/dromara/jquick-curl/releases) 与提交记录。

## 开源协议

JQuickCurl 使用 [Apache License 2.0](./LICENSE) 开源。使用、修改和分发本项目时，请遵守许可证中的版权、专利和声明保留条款。

## 贡献指南

欢迎通过以下方式参与：

1. 使用前先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)。
2. 提交 Issue 时提供版本、运行环境、最小复现代码和完整错误信息。
3. 提交 Pull Request 前补充或更新测试，并保持改动聚焦。
4. 不要提交密钥、Token、个人数据或生产环境配置。

## 项目归属与支持

JQuickCurl 现归属于 [Dromara 开源组织](https://dromara.org/)，项目主仓库为 [github.com/dromara/jquick-curl](https://github.com/dromara/jquick-curl)。

如果 JQuickCurl 帮助你减少了 HTTP 请求代码，欢迎在 GitHub 上 [Star](https://github.com/dromara/jquick-curl) 和 [Fork](https://github.com/dromara/jquick-curl/fork)，也欢迎提交 Issue 和 Pull Request。

## Awesome Java

JQuickCurl 已收录至 [Awesome Java](https://github.com/akullpp/awesome-java) 的 HTTP Clients 分类。