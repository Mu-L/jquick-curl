# JQuick-Curl 拦截器实战：统一 Token、日志、请求预处理，让第三方接口调用真正工程化

**项目地址**：`https://github.com/dromara/jquick-curl`

**Maven坐标**
```xml
<dependency>
    <groupId>io.github.paohaijiao</groupId>
    <artifactId>jquick-curl</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## 前言

很多团队在引入一个 java http 客户端 之后，最开始都能顺利把请求发出去，但很快就会遇到第二阶段问题：Token 怎么统一加？请求日志怎么统一打？某些外部接口发起前要不要做参数预处理？如果这些能力都散落在业务代码里，最开始可能还看不出来问题，但接口一多，维护成本马上就会上来。

JQuick-Curl 并不是只能写几条 curl 命令跑跑 Demo。它同样提供了面向工程场景的扩展点，其中一个非常关键的能力就是拦截器。拦截器的作用，就是把通用处理逻辑从业务请求定义中抽出来，形成统一治理。

对第三方接口调用来说，这一步非常重要。因为真正复杂的往往不是请求本身，而是请求周边的公共策略。

## 正文

### 为什么拦截器对第三方接口调用特别重要

一个项目中只要有多个外部系统，通常就会出现这些共性需求：

- 每次请求前统一加鉴权 Token
- 打印请求日志和响应日志
- 记录调用耗时
- 对某些 Header 做统一补充
- 对请求参数做发送前调整

如果这些逻辑写在每个 `@JCurlCommand` 方法旁边，代码会非常分散。拦截器的意义，就是把这些行为抽成统一入口。

### 拦截器能做什么

从设计上讲，拦截器最适合做横切逻辑，而不是业务判断。你应该用它处理“所有请求都要做”或者“某一类请求都要做”的动作。

比如统一 Token 注入，就是最典型的拦截器场景。日志打印和链路观测，也非常适合放这里。

### 统一 Token 的思路

很多外部平台使用 Bearer Token、自定义 Header Token 或签名 Header。你可以在请求执行前，把这些鉴权信息统一补进去，而不是在每条 curl 中重复写死。

## 实战代码块

下面先给一个基于变量的统一认证思路示例。业务侧只关心传入参数，curl 模板不硬编码敏感信息。

```java
import com.github.paohaijiao.anno.JCurlCommand;
import com.github.paohaijiao.domain.req.JQuickCurlReq;
import com.github.paohaijiao.executor.JCurlInvoker;

public interface SecureApi {
    @JCurlCommand("curl -X GET https://api.example.com/orders -H 'Authorization: Bearer ${token}'")
    String queryOrders(JQuickCurlReq request);

    static void main(String[] args) throws Exception {
        SecureApi api = JCurlInvoker.createProxy(SecureApi.class);
        JQuickCurlReq req = new JQuickCurlReq();
        req.put("token", "demo-access-token");
        String result = api.queryOrders(req);
        System.out.println(result);
    }
}
```

如果你的项目结合全局配置或拦截器扩展点，就可以把 `token` 的注入从每个调用方手里再往外收一层。

### 日志统一处理的价值

很多团队到了线上排查时才意识到，请求日志不是“有没有都行”的附属品，而是外部接口治理的基本盘。尤其在第三方接口调用里，请求失败时常常需要知道：

- 发给谁
- 用了什么方法
- 带了哪些 Header
- 请求体长什么样
- 响应内容是什么
- 耗时多久

JQuick-Curl 的一个优势是，请求本身就是 curl 命令表达。日志如果围绕这个命令做统一记录，可读性会很好。

### 请求预处理怎么理解

请求预处理并不一定是“修改业务参数”，更多是做一些标准化动作。例如：

- 给所有请求加上统一客户端标识 Header
- 对部分路径自动拼接环境前缀
- 对某些变量做空值保护
- 在请求发出前记录 traceId

这些都属于典型工程治理动作，不应该散落在每个业务方法中。

### 一个更稳的组合方式

在实际项目里，比较推荐的组合是：

- curl 模板中保留清晰业务意图
- 敏感值和环境值走变量
- 公共 Header 和日志策略放到统一扩展层
- 业务方法只表达“调哪个接口”

这样结构会比较清楚，也方便后续做 RestTemplate 对比 或迁移治理。

### 它和传统 java http 客户端 的差别

传统 java http 客户端 也有拦截器，比如 OkHttp 非常成熟。但 JQuick-Curl 的独特价值在于：你处理的仍然是一条 curl 所表达的请求。也就是说，请求定义足够直观，而公共治理又能独立出去。这种组合在外部系统接入上非常顺手。

## 注意点 / 踩坑提示

### 1. 不要把业务分支逻辑塞进拦截器

拦截器适合处理横切能力，不适合承载复杂业务判断。

### 2. Token 尽量变量化，不要写死在命令里

这是最基本的安全要求，也便于环境切换和轮换。

### 3. 日志注意脱敏

请求里如果包含密码、身份证号、手机号、密钥字段，统一日志时必须做脱敏处理。

### 4. 请求预处理要可预测

不要让拦截器在开发者不知情的情况下大幅修改原命令语义，否则会增加排查难度。

## 总结

拦截器是 JQuick-Curl 从“能发请求”走向“可工程治理”的关键能力之一。统一 Token、统一日志、统一请求预处理，本质上都是在降低第三方接口调用的维护成本。对于业务项目来说，这部分能力的重要性并不比发送请求本身低。

JQuick-Curl 的优势在于，它既保留了 curl 转 java 的天然直观，又给了你走向工程化的扩展空间。这样一来，它就不仅适合快速接入，也适合长期维护。

## 下一篇预告

下一篇我们继续往工程化走：多个 curl 命令如何批量执行，批量 HTTP 请求时怎样组织更稳，哪些场景适合批量模式，哪些不适合。

#Java #JQuickCurl #拦截器 #第三方接口调用 #HTTP客户端