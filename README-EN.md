# JQuickCurl

[![GitHub Stars](https://img.shields.io/github/stars/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/network/members)
[![License](https://img.shields.io/github/license/dromara/jquick-curl?style=flat-square)](https://github.com/dromara/jquick-curl/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.paohaijiao/jquick-curl?style=flat-square)](https://central.sonatype.com/artifact/io.github.paohaijiao/jquick-curl)
[![Awesome Java](https://awesome.re/badge.svg)](https://github.com/akullpp/awesome-java)

[简体中文](./README.md) | **English**

JQuickCurl is a curl-command-oriented HTTP client framework for Java. It parses reusable native curl commands from a browser, Postman, or terminal into executable Java requests, with annotation and XML configuration, variable substitution, conditional rendering, file transfer, batch execution, and dynamic proxy clients.

> A [Dromara](https://dromara.org/) project. Main repository: [dromara/jquick-curl](https://github.com/dromara/jquick-curl).

## Contents

- [Key Advantages](#key-advantages)
- [Use Cases](#use-cases)
- [Supported curl Command Formats](#supported-curl-command-formats)
- [Quick Start](#quick-start)
- [Core Features](#core-features)
- [Complete Example](#complete-example)
- [Advanced Features](#advanced-features)
- [Architecture](#architecture)
- [Release Notes](#release-notes)
- [License](#license)
- [Contributing](#contributing)
- [Project Ownership and Support](#project-ownership-and-support)
- [Awesome Java](#awesome-java)

## Key Advantages

| Dimension | JQuickCurl | OkHttp / RestTemplate / HttpClient |
| --- | --- | --- |
| Request definition | Reuse native curl commands directly | Manually build requests, headers, parameters, and bodies |
| Collaboration | Share the same request format across frontend, backend, and QA | Translate between curl and client-specific APIs |
| Configuration | `@JCurlCommand` annotations plus XML | Primarily Java builders, templates, or framework configuration |
| Dynamic requests | Variables and XML conditions inside the command | Manually concatenate URLs, bodies, and conditional branches |
| API integration | Interface plus dynamic proxy | Write and maintain service wrappers by hand |
| Files and methods | Common HTTP methods, multipart upload, and downloads | Powerful, but request descriptions are more tightly coupled to code |

The project is built around three distinctive ideas: **native curl parsing**, **zero hand-written request construction**, and **dynamic XML conditions**. It uses OkHttp as the transport layer and ANTLR to parse curl syntax; it does not start a system curl process.

## Use Cases

- Move curl snippets from Postman or browser developer tools into Java quickly.
- Integrate payment providers, microservices, data platforms, and third-party APIs.
- Keep a large collection of API definitions in XML and separate them from business code.
- Centralize authentication variables, environment hosts, conditional headers, and request bodies.
- Implement file uploads, batch API calls, downloads, and HTTP integration tests.

## Quick Start

### Maven dependency

The current project version is `2.2.0`:

```xml
<dependency>
    <groupId>io.github.paohaijiao</groupId>
    <artifactId>jquick-curl</artifactId>
    <version>${version}</version>
</dependency>
```

### Minimal example

Declare a curl command with `@JCurlCommand`. The generated proxy parses and executes the command, then converts the response to the method's declared return type.

```java
import com.github.paohaijiao.anno.JCurlCommand;
import com.github.paohaijiao.domain.req.JQuickCurlReq;
import com.github.paohaijiao.executor.JCurlInvoker;

public interface EchoApi {
    @JCurlCommand("curl -X GET https://xxx.org/get")
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

## Supported curl Command Formats

JQuickCurl parses commands that start with `curl` and use `-X` or `--request` to specify the HTTP method. The current test suite covers these 8 methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`, and `TRACE`.

### Request methods

```bash
# GET: read resources; normally has no request body
curl -X GET https://api.example.com/users

# POST: create a resource or submit JSON
curl -X POST https://api.example.com/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada"}'

# PUT: replace a resource
curl -X PUT https://api.example.com/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace"}'

# PATCH: partially update a resource
curl -X PATCH https://api.example.com/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"active":true}'

# DELETE: delete a resource
curl -X DELETE https://api.example.com/users/1

# HEAD: fetch response headers only
curl -X HEAD https://api.example.com/users/1

# OPTIONS: inspect server-supported methods
curl -X OPTIONS https://api.example.com/users/1

# TRACE: echo a request for diagnostics; treated as bodyless by the current executor
curl -X TRACE https://api.example.com/trace
```

### Implemented curl options

| Category | Supported format | Purpose |
| --- | --- | --- |
| Request method | `-X <METHOD>`, `--request <METHOD>` | Select one of the HTTP methods above |
| Headers | `-H 'Name: value'`, `--header 'Name: value'` | Add a request header such as `Content-Type` |
| Request data | `-d 'data'`, `--data 'data'`, `--data-ascii`, `--data-binary`, `--data-raw` | Send a request body |
| Form encoding | `--data-urlencode 'key=value'` | Send URL-encoded form data |
| Basic authentication | `-u 'user:password'`, `--user 'user:password'` | Generate a Basic Authorization header |
| Redirects | `-L`, `--location`, `--max-redirs <N>` | Follow redirects and configure the maximum count |
| File upload | `-F 'file=@/path/to/file'`, `--form 'key=value'` | Send multipart files or regular form fields |
| File download | `-o './file'`, `--output './file'` | Write response bytes to a local file |
| Proxy | `-x 'host:port'`, `--proxy 'host:port'`, `--socks5-hostname 'host:port'` | Use an HTTP or SOCKS5 proxy |
| Protocol and logging | `--http2`, `-k`, `--insecure`, `-v`, `--verbose`, `-s`, `--silent` | HTTP/2, skip certificate checks, verbose, or silent output |

### Use from Java

Put the command in `@JCurlCommand` and execute it through a dynamic proxy. Return types may be `String`, a domain object, `JResult`, `byte[]`, or `Void`:

```java
public interface UserApi {
    @JCurlCommand("curl -X GET https://api.example.com/users")
    String get(JQuickCurlReq request);

    @JCurlCommand("curl -X POST https://api.example.com/users -H 'Content-Type: application/json' -d '{\"name\":\"Ada\"}'")
    String create(JQuickCurlReq request);
}

UserApi api = JCurlInvoker.createProxy(UserApi.class);
String result = api.get(new JQuickCurlReq());
```

> Note: JQuickCurl is not a complete replacement for the system curl command. The formats above are confirmed by the current parser and test cases. Verify any unlisted curl option or HTTP method before using it. Although `CONNECT` exists in an internal enum, it is not promised as a stable documented capability.

## Core Features

### 1. Annotation-based requests

Use annotations when request definitions belong close to the Java API interface:

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

`@JCurlCommand` also exposes execution and validation attributes such as `execute`, `expectedStatus`, `expectedBusinessStatus`, and `validationScript`.

### 2. XML configuration

Use XML to centralize a collection of APIs and keep curl definitions separate from Java code. The project provides the following DTD:

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

The Java method name must match the `<curl name="...">` attribute:

```java
import com.github.paohaijiao.domain.req.JQuickCurlReq;

public interface UserApi {
    String getUser(JQuickCurlReq request);
}
```

Load the XML file and create a proxy:

```java
import com.github.paohaijiao.xml.JQuickCurlXmlParseFactory;
import com.github.paohaijiao.xml.factory.JQuickXmlFactory;
import com.github.paohaijiao.xml.handler.JQuickParseHandler;

JQuickParseHandler parser = new JQuickCurlXmlParseFactory();
JQuickXmlFactory factory = new JQuickXmlFactory(parser, "apis.xml");
UserApi api = factory.createApi(UserApi.class);
```

### 3. Variable substitution

- For annotation proxies, `${name}` resolves values from `JQuickCurlReq`.
- XML proxies commonly use `#{name}` for values in the execution context, including method-bound parameters.

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

Never commit real passwords, tokens, or private keys to Java source or XML. Inject them at runtime instead.

### 4. Conditional XML rendering

Use `<if test="...">...</if>` inside XML curl content. The enclosed headers, parameters, or options are rendered only when the expression is true:

```xml
<curl name="search" returnClass="java.lang.String">
    curl -X GET #{host}/search
    <if test="withTrace == true"> -H "X-Trace: #{traceId}" </if>
</curl>
```

Use XML-safe attribute values and context variables. For complex requests, prefer several small `<if>` blocks over one opaque expression.

### 5. File upload and download

- `-F "file=@/path/to/file"`: upload one file.
- Multiple `-F` options: upload several files or combine files with regular form fields.
- `--output` / `-o`: the executor reads the response bytes and writes them to the local path in the command.
- Without `--output`, declare a Java method that returns `byte[]` and save the bytes in application code.

Let the curl command write the file:

```java
@JCurlCommand("curl -X GET https://api.example.com/files/report.pdf --output './download/report.pdf'")
byte[] downloadToFile(JQuickCurlReq request);
```

Return bytes and save them in Java:

```java
@JCurlCommand("curl -X GET https://api.example.com/files/report.pdf")
byte[] download(JQuickCurlReq request);

byte[] bytes = api.download(new JQuickCurlReq());
Files.write(Paths.get("./download/report.pdf"), bytes);
```

### 6. Batch requests

Annotate several no-argument methods in one class with `@JCurlCommand`, then execute them with `JQuickCurlBatchRunner`:

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

### 7. Dynamic proxies and method references

Use `JCurlInvoker.createProxy` for annotated interfaces. Existing methods carrying `@JCurlCommand` can also be invoked through `JCurlInvoker.invoke` with a method reference and an explicit return type.

## Complete Example

The following interface covers GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE, file upload, file download, and multipart forms. Replace the example endpoints with your own service.

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

Invoke the proxy:

```java
CompleteApi api = JCurlInvoker.createProxy(CompleteApi.class);
JQuickCurlReq request = new JQuickCurlReq();
System.out.println(api.get(request));
System.out.println(api.post(request));
byte[] file = api.download(request);
```

## Advanced Features

### Timeouts, retries, redirects, and connection pooling

`JQuickCurlConfig` is the global configuration singleton. It supports timeouts, connection pools, retries, redirects, and interceptors:

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

Use `@JTimeout(connect = ..., read = ..., write = ...)` to override timeout values for an individual method.

### Interceptors

Interceptors use the OkHttp `Interceptor` API. They can add authentication, log requests, or inspect responses:

```java
import com.github.paohaijiao.config.JQuickCurlConfig;
import okhttp3.Interceptor;

Interceptor auth = chain -> chain.proceed(
        chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + System.getenv("API_TOKEN"))
                .build());

JQuickCurlConfig.getInstance().addInterceptor(auth);
```

### Batch execution

Call `JQuickCurlBatchRunner.runCurlCommands(Object, Class<T>)` to scan a command class and return its results. Methods must be no-argument methods annotated with `@JCurlCommand`; `@JTimeout` can configure an individual batch command.

### Proxy factories

- Annotation proxy: `JCurlInvoker.createProxy(Api.class)`.
- XML proxy: `new JQuickXmlFactory(new JQuickCurlXmlParseFactory(), "apis.xml").createApi(Api.class)`.
- Method reference: `JCurlInvoker.invoke(Service::method, request, ReturnType.class)`.

## Architecture

```text
curl string / annotation / XML
              |
              v
      ANTLR lexer + parser       <- curl syntax
              |
              v
      visitor + JContext         <- variables, conditions, request context
              |
              v
      OkHttp transport           <- pool, timeouts, retries, interceptors
              |
              v
      JQuickCurlResponseBody     <- raw response
              |
              v
      response converters        <- String, objects, collections, byte[]
```

The main modules are `anno` (annotations), `parser` (ANTLR parser), `visitor` (command visitors), `executor` (execution), `xml` (XML proxy), `handler` (dynamic proxy), `result` (response conversion), and `config` (global configuration).

## Release Notes

### 2.1.0

- Refined curl parsing and HTTP request execution.
- Added or expanded annotation proxies, XML-configured proxies, and method-reference invocation.
- Supports variable substitution, conditional XML rendering, file upload and download, and batch execution.
- Supports timeout, retry, redirect, connection-pool, and OkHttp interceptor configuration.

See [Releases](https://github.com/dromara/jquick-curl/releases) and the commit history for additional details.

## License

JQuickCurl is released under the [Apache License 2.0](./LICENSE). When using, modifying, or distributing the project, comply with the license terms, including copyright, patent, and notice requirements.

## Contributing

Contributions are welcome:

1. Read [CONTRIBUTING-EN.md](./CONTRIBUTING-EN.md) before making changes.
2. Include the version, environment, minimal reproduction, and complete error output in an Issue.
3. Add or update tests before submitting a Pull Request, and keep changes focused.
4. Never commit secrets, tokens, personal data, or production configuration.

## Project Ownership and Support

JQuickCurl is maintained under the [Dromara open-source organization](https://dromara.org/). The main repository is [github.com/dromara/jquick-curl](https://github.com/dromara/jquick-curl).

If JQuickCurl saves you from writing repetitive HTTP request code, please [Star](https://github.com/dromara/jquick-curl) or [Fork](https://github.com/dromara/jquick-curl/fork) the repository. Issues and Pull Requests are also welcome.

## Awesome Java

JQuickCurl is listed in the HTTP Clients section of [Awesome Java](https://github.com/akullpp/awesome-java).
JQuickCurl is listed in the HTTP Clients section of [Awesome Java](https://github.com/akullpp/awesome-java).