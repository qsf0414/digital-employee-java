# digital-employee-java 架构开发规范与实施规划

> **项目名称**：`digital-employee-java`（后端单体） + `digital-employee-web`（前端）
> **文档版本**：v2.5
> **生效日期**：2026-09-06
> **基准调整**：全面采纳 **Spring Boot 4.x** 标准生态，适配 `sa-token-spring-boot4-starter` 与 Boot 4 全新依赖体系（`webmvc` 与 `aspectj` 模块切分）。业务模型收敛为**"用户仅绑定单角色"的 4 表 RBAC 模型**（废弃 `sys_user_role` 关联表，`role_id` 直接内嵌 `sys_user`），权限缓存升级为**角色维度共享缓存**（`cache:role:perms:{roleKey}:{ver}`），消除多角色组合爆炸与用户级并集缓存混乱。

---

## 1. 架构总览与交互模型

系统采用前后端分离的单体工程架构。认证状态统一由服务端的 Redis 承载（唯一状态源），基于 Sa-Token 的"单 Token 绝对过期 + 活跃超时滑动续约"机制，实现平滑无感续签与精确的"同账号新登录挤掉旧会话"拦截。

### 1.1 架构总览

```mermaid
graph TB
    subgraph Frontend ["digital-employee-web (前端)"]
        A1["Pinia UserStore"]
        A2["Axios (Bearer Token / 401 拦截)"]
        A3["动态路由生成"]
        A4["v-hasPermi 指令 + checkPermi 函数"]
        A1 --- A2
        A3 --- A4
    end

    subgraph Backend ["digital-employee-java (后端)"]
        B1["SaInterceptor — 全局路由认证拦截"]
        B2["@SaCheck... — AOP 注解细粒度鉴权"]
        B3["StpInterface — 统一权限与超管旁路 (*:*:*)"]
        B4["Unified Advice — 异常映射与分流"]
        B1 --> B2 --> B3
        B3 --> B4
    end

    subgraph Storage ["Redis (唯一会话真相源)"]
        C1["satoken:login:token:xxx — 会话状态 (TTL / 活跃时长)"]
        C2["cache:role:perms:{roleKey}:{ver} — 角色权限码共享缓存 (版本号)"]
    end

    Frontend -->|"HTTP / JSON (Bearer Token)"| Backend
    Backend -->|"会话读写"| Storage
```

### 1.2 交互模型

#### 登录认证流程

```mermaid
sequenceDiagram
    participant U as 用户浏览器
    participant FE as 前端 (Pinia + Axios)
    participant BE as 后端 (SaInterceptor)
    participant Redis as Redis

    U->>FE: 输入账号密码 + 验证码
    FE->>BE: POST /api/v1/auth/login
    BE->>Redis: LoginRateLimiter 双 Key 限流检查 (IP + IP:account)
    alt 触发限流阈值
        Redis-->>BE: count > 5
        BE-->>FE: 429 TOO_MANY_REQUESTS
    else 未触发限流
        BE->>BE: 校验密码 + is-concurrent 判断
        alt 同账号已存在会话
            BE->>Redis: 删除旧 Token 会话
        end
        BE->>Redis: 写入新 Token (timeout=7d, active-timeout=30min)
        BE->>Redis: 读取/回填 cache:role:perms:{roleKey}:{ver}
        BE-->>FE: 返回 Bearer Token
    end
    FE->>FE: 存入 Pinia UserStore + Storage
    FE-->>U: 跳转主页，动态路由注入
```

#### API 请求鉴权流程

```mermaid
sequenceDiagram
    participant U as 用户浏览器
    participant FE as 前端 Axios 拦截器
    participant Interceptor as SaInterceptor (路由层)
    participant AOP as @SaCheck Permission (AOP 层)
    participant Redis as Redis

    U->>FE: 触发操作（按钮/路由）
    FE->>Interceptor: 请求 + Authorization: Bearer Token
    Interceptor->>Redis: 查询会话状态
    alt 会话不存在或已过期
        Redis-->>Interceptor: null / 已过期
        Interceptor-->>FE: 401 UNAUTHORIZED
        FE->>FE: 清除 Storage，跳转 /login
    else 会话已被新登录替换
        Redis-->>Interceptor: SESSION_REPLACED
        Interceptor-->>FE: 401 SESSION_REPLACED
        FE->>FE: ElMessageBox 强制提示，跳转 /login
    else 会话有效
        Redis-->>Interceptor: 会话正常
        Interceptor->>AOP: 路由校验通过，进入业务层
        AOP->>AOP: 检查 @SaCheckPermission 注解
        alt 无权限
            AOP-->>FE: 403 PERMISSION_DENIED
        else 有权限（超管 *:*:* 直接放行）
            AOP-->>FE: 200 业务数据
        end
    end
```

> **产品侧已知限制**：Sa-Token 的"被顶下线"检测基于 HTTP 请求拦截机制。浏览器已打开的标签页**不会实时弹出下线提示**，只有在该标签页发起下一次 HTTP 请求时，拦截器捕获到 `SESSION_REPLACED` 后才会弹窗。若需实时推送下线通知，需引入 WebSocket / SSE 全双工通道，当前版本不纳入。

---

## 2. 后端技术规范（`digital-employee-java`）

### 2.1 技术栈与基线选型

* **运行环境**：JDK **21**
* **核心框架**：Spring Boot **4.0.3+**
* **安全框架**：Sa-Token **1.45.0+**（采用针对 Boot 4 的 `sa-token-spring-boot4-starter` 与 `sa-token-redis-template`）
* **ORM 框架**：MyBatis-Plus **3.5.9+**（须使用 `mybatis-plus-spring-boot4-starter`，适配 Jakarta 命名空间）
* **连接池**：HikariCP（Spring Boot 默认，零配置引入，性能优于 Druid）
* **密码加密**：Spring Security Crypto `BCryptPasswordEncoder`
* **存储引擎**：PostgreSQL 15+（JDBC Driver 42.7.x，由 Boot BOM 管理）、Redis 6.2+

### 2.2 Maven 核心依赖清单（Spring Boot 4 规范）

> **Boot 4 变化提示**：旧版的 `spring-boot-starter-web` 与 `spring-boot-starter-aop` 已废弃，必须替换为 `spring-boot-starter-webmvc` 与 `spring-boot-starter-aspectj`。

#### 版本管理（properties）

| 属性 | 版本 | 说明 |
| :--- | :--- | :--- |
| `java.version` | 21 | JDK 固定 21，LTS 版本 |
| `sa-token.version` | 1.45.0 | Sa-Token 核心 + Redis 集成，统一版本锁定 |
| `mybatis-plus.version` | 3.5.9 | MyBatis-Plus，须使用 Boot 4 / Jakarta 命名空间适配版 |

#### 依赖清单

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <sa-token.version>1.45.0</sa-token.version>
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
</properties>

<dependencies>
    <!-- ==================== Spring Boot 4 核心 ==================== -->

    <!--
        spring-boot-starter-webmvc
        Boot 4 新模块，替代原 spring-boot-starter-web。
        内嵌 Tomcat + Spring MVC 自动装配，提供 REST Controller 能力。
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <!--
        spring-boot-starter-aspectj
        Boot 4 新模块，替代原 spring-boot-starter-aop。
        提供 AspectJ 编译时织入，保证 @SaCheckPermission 等注解鉴权生效。
        若仍使用旧 starter-aop，Boot 4 启动时会因类路径冲突报错。
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aspectj</artifactId>
    </dependency>

    <!-- ==================== Sa-Token 认证鉴权 ==================== -->

    <!--
        sa-token-spring-boot4-starter
        Sa-Token 官方 Boot 4 专用 Starter，自动注册 SaInterceptor、
        StpInterface 等核心 Bean，兼容 Jakarta Servlet API。
        注意：不要使用旧版 sa-token-spring-boot-starter，该版本基于
        Spring Boot 3.x 的 javax 命名空间，Boot 4 下无法启动。
    -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot4-starter</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!--
        sa-token-redis-template
        Sa-Token 会话持久化到 Redis 的集成模块，基于 Spring Data RedisTemplate。
        登录会话（satoken:login:token:xxx）与角色权限缓存（cache:role:perms:xxx）
        统一写入 Redis，实现分布式会话共享。
    -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-template</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!--
        commons-pool2
        Redis 连接池（Lettuce 底层依赖），管理 Redis 长连接复用，
        避免每次请求新建连接导致性能下降。
    -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>

    <!-- ==================== 数据库层 ==================== -->

    <!--
        mybatis-plus-spring-boot4-starter (版本 3.5.9+)
        MyBatis-Plus 官方 Boot 4 专用 Starter，核心要点：
        ① artifactId 必须是 mybatis-plus-spring-boot4-starter（不是 mybatis-plus-boot-starter），
           后者基于 javax 命名空间，Boot 4 下启动报 ClassNotFoundException。
        ② 内置分页插件 PaginationInnerInterceptor，需在 Configuration 中注册：
           @Bean public MybatisPlusInterceptor mybatisPlusInterceptor() {
               MybatisPlusInterceptor i = new MybatisPlusInterceptor();
               i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
               return i;
           }
        ③ 兼容 Jakarta 持久化注解（jakarta.persistence 而非 javax.persistence），
           实体类使用 @TableName、@TableId、@TableField 等注解无冲突。
        ④ 若官方 Boot 4 Starter 尚未发布稳定版，可降级使用原生 mybatis-spring-boot-starter
           + 手动配置，但会失去自动 CRUD 与分页等便利能力。
    -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!--
        PostgreSQL JDBC Driver
        版本由 Spring Boot Parent BOM 统一管理（当前 Boot 4.0.3 对应 42.7.x），
        无需显式声明 <version>。
        scope=runtime：仅运行时需要，编译期不直接引用 JDBC API。
        application.yml 连接地址格式：
          url: jdbc:postgresql://localhost:5432/digital_employee
          driver-class-name: org.postgresql.Driver
          username/password: 环境变量注入
    -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!--
        HikariCP（Spring Boot 默认连接池，无需额外引入依赖）
        选用理由：
        ① Spring Boot 4 自动装配 HikariCP，引入 spring-boot-starter-jdbc 或
           mybatis-plus-spring-boot4-starter 后自动生效，零配置即可使用。
        ② 性能优于 Druid（延迟低、吞吐高），JMH 基准测试领先 20%-40%。
        ③ 轻量级（约 130KB），无额外监控页面开销，适合生产环境。
        如需 SQL 监控能力，可通过 Micrometer + Prometheus 指标暴露替代 Druid 监控页。
    -->

    <!-- ==================== 安全 ==================== -->

    <!--
        spring-security-crypto
        仅引入加密模块（BCryptPasswordEncoder），不引入完整 Spring Security 过滤链。
        用于密码哈希存储：PasswordEncoder.encode() / matches()。
        由 Boot Parent BOM 管理版本，无需显式声明。
    -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- ==================== 工具 ==================== -->

    <!--
        spring-boot-configuration-processor
        编译期生成 configuration-metadata.json，IDE 自动提示
        application.yml 中 sa-token.* / mybatis-plus.* 等自定义配置项。
        optional=true：不打入最终 Fat Jar。
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### HikariCP 连接池配置示例（`application.yml`）

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:digital_employee}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
    hikari:
      pool-name: digital-employee-pool
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000          # 5 分钟空闲回收
      max-lifetime: 1200000         # 20 分钟连接最大存活（须小于 PostgreSQL max_connections timeout）
      connection-timeout: 30000     # 30 秒获取连接超时
      leak-detection-threshold: 60000  # 60 秒未归还视为泄漏，打印警告日志
```

### 2.3 Sa-Token 配置 (`application.yml`)

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 604800         # 绝对过期：7 天无条件失效（秒）
  active-timeout: 1800    # 活跃超时：30 分钟无操作自动冻结退出（秒）
  is-concurrent: false    # 严格单会话：同账号新登录顶掉旧登录
  is-share: false         # 每次登录更新独立的 Token
  token-style: random-64  # 64 位高熵随机串 (opaque token)
  is-read-cookie: false   # 纯 Token 模式，关闭 Cookie 读取
  is-log: false           # 生产关闭冗余日志
```

> **产品侧约束**：`is-concurrent: false` 意味着同一账号在新终端登录后，旧终端的其他浏览器标签页将在下一次请求时被强制踢下线。产品与测试团队需提前知晓此行为，避免误报为缺陷。

### 2.4 后端核心代码落地

**1. 统一信封 (`Result.java`)**

```java
package com.digital.employee.common.core;

import lombok.Data;

@Data
public class Result<T> {
    private String code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = "OK";
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}

**2. Redis Key 常量管理 (`RedisConstants.java`)**

统一抽离所有 Redis Key 前缀与 TTL 常量，杜绝硬编码散落在各 Service 中：

```java
package com.digital.employee.common.redis;

public final class RedisConstants {

    private RedisConstants() {}

    /** 角色权限缓存 Key 前缀，完整格式：cache:role:perms:{roleKey}:{roleVersion}（同角色用户共享） */
    public static final String ROLE_PERM_CACHE_PREFIX = "cache:role:perms:";

    /** 角色版本号 Key 前缀，完整格式：sys:role:version:{roleKey} */
    public static final String ROLE_VERSION_PREFIX = "sys:role:version:";

    /** 登录限流 Key 前缀（IP 维度），完整格式：login:rate:ip:{ip} */
    public static final String LOGIN_RATE_IP = "login:rate:ip:";

    /** 登录限流 Key 前缀（IP+账号维度），完整格式：login:rate:ipacct:{ip}:{username} */
    public static final String LOGIN_RATE_IP_ACCT = "login:rate:ipacct:";

    /** 角色权限缓存 TTL（小时） */
    public static final long ROLE_PERM_CACHE_TTL_HOURS = 24;

    /** 登录限流窗口（秒） */
    public static final int LOGIN_RATE_WINDOW_SECONDS = 900;

    /** 登录限流最大次数 */
    public static final int LOGIN_RATE_MAX_ATTEMPTS = 5;
}
```

**3. CORS 跨域配置 (`CorsConfigure.java`)**

```java
package com.digital.employee.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfigure {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://your-domain.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

**4. 拦截器路由配置 (`SaTokenConfigure.java`)**

分工明确：拦截器负责"要不要登录"，AOP 注解负责"有没有权限"。

```java
package com.digital.employee.system.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/api/**")
                    .notMatch(
                        "/api/v1/auth/login",
                        "/api/v1/auth/captcha",
                        "/api/v1/health"
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
```

> **接口版本管理约束**：当前统一使用 `/api/v1/**` 前缀。未来引入 v2 版本时，在 `SaRouter` 中追加 `/api/v2/**` 匹配规则；旧版本接口保持兼容，新功能仅在 v2 路径下开发，直至 v1 下线。

**5. 权限数据源与超管旁路 — 单角色版本号共享缓存 (`StpInterfaceImpl.java`)**

收敛单角色模型后，用户鉴权退化为"查角色 → 读该角色版本缓存"两步。严禁直接将 `loginId` 强转为 `Long`（Redis 反序列化底层通常为 String）；通过返回 `*:*:*` 统一旁路 `super_admin`。同角色的所有用户共享同一份 `cache:role:perms:{roleKey}:{ver}` 缓存，命中率接近 100%：

```java
package com.digital.employee.system.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class StpInterfaceImpl implements StpInterface {

    /** 空权限占位符：防止无权限角色频繁穿透查库 */
    private static final String EMPTY_FLAG = ":empty:";

    private final ISysMenuService menuService;
    private final ISysRoleService roleService;
    private final StringRedisTemplate redisTemplate;

    public StpInterfaceImpl(ISysMenuService menuService, ISysRoleService roleService,
                            StringRedisTemplate redisTemplate) {
        this.menuService = menuService;
        this.roleService = roleService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysRole role = roleService.getRoleByUserId(userId);
        if (role == null || role.getStatus() == 0) {
            return List.of();
        }

        // 1. 超管通配旁路
        if ("super_admin".equals(role.getRoleKey())) {
            return List.of("*:*:*");
        }

        // 2. 单角色版本缓存（全局所有同角色用户共享此缓存）
        String roleKey = role.getRoleKey();
        String ver = redisTemplate.opsForValue().get(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
        ver = (ver != null) ? ver : "0";
        String cacheKey = RedisConstants.ROLE_PERM_CACHE_PREFIX + roleKey + ":" + ver;

        Set<String> cached = redisTemplate.opsForSet().members(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached.contains(EMPTY_FLAG) ? List.of() : new ArrayList<>(cached);
        }

        // 3. 缓存未命中 -> 查 DB 并回填，空权限写占位符防穿透
        List<String> dbPerms = menuService.selectPermsByRoleId(role.getId());
        if (dbPerms.isEmpty()) {
            redisTemplate.opsForSet().add(cacheKey, EMPTY_FLAG);
        } else {
            redisTemplate.opsForSet().add(cacheKey, dbPerms.toArray(new String[0]));
        }
        redisTemplate.expire(cacheKey, RedisConstants.ROLE_PERM_CACHE_TTL_HOURS, TimeUnit.HOURS);

        return dbPerms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysRole role = roleService.getRoleByUserId(userId);
        return (role != null && role.getStatus() == 1) ? List.of(role.getRoleKey()) : List.of();
    }
}
```

**6. 权限缓存维护规则（版本号机制）**

采用 **Cache-Aside + 版本号** 模式，角色权限变更时只需递增版本号（O(1)），旧缓存自然失效：

| 触发时机 | 操作 | 说明 |
| :--- | :--- | :--- |
| 首次鉴权 / 登录 | 读取 `cache:role:perms:{roleKey}:{ver}`，未命中则查库回填 | 同一角色的所有用户共享同一份缓存，缓存命中率接近 100% |
| 修改角色菜单权限 | `INCR sys:role:version:{roleKey}` | 单次 O(1) 操作，该角色下所有用户下次请求自动取新版本缓存 |
| 角色无任何权限 | 写入 `:empty:` 占位符 | 防止空权限角色频繁穿透查库 |
| 兜底 TTL | 24 小时自动过期 | 防止极端场景下缓存脏数据残留 |

> **性能优势**：相比"修改角色后遍历删除所有用户缓存 Key"，版本号机制在角色绑定成千上万用户时避免了 Redis 批量删除的性能风险，单次 `INCR` 即可完成全量失效；且角色级共享缓存将 Key 数量从"用户数"压缩到"角色数"，内存开销与缓存命中率同时达到最优。

**7. 登录防爆破限流 (`LoginRateLimiter.java`)**

采用 **双 Key（IP 维度 + IP:账号维度）Redis INCR + EXPIRE** 实现，限流逻辑前置于 BCrypt 慢哈希校验之前，防止算力被恶意耗尽：

```java
package com.digital.employee.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ip, String username) {
        if (isBlocked(RedisConstants.LOGIN_RATE_IP + ip)) {
            return false;
        }
        return !isBlocked(RedisConstants.LOGIN_RATE_IP_ACCT + ip + ":" + username);
    }

    private boolean isBlocked(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RedisConstants.LOGIN_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        return count != null && count > RedisConstants.LOGIN_RATE_MAX_ATTEMPTS;
    }
}
```

> **调用位置**：在 `AuthController.login()` 方法最前部调用 `loginRateLimiter.isAllowed(ip, username)`，返回 `false` 时直接响应 `429 TOO_MANY_REQUESTS`，**不进入密码校验流程**。BCrypt `matches()` 单次耗时约 80-120ms，若被恶意遍历将严重消耗 CPU 线程池。

**8. 全局异常细分映射 (`GlobalExceptionHandler.java`)**

将常规过期（`UNAUTHORIZED`）与被挤下线（`SESSION_REPLACED`）区分响应，便于前端针对性展示提示：

```java
package com.digital.employee.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.digital.employee.common.core.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException e) {
        if (NotLoginException.BE_REPLACED.equals(e.getType())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("SESSION_REPLACED", "您的账号已在其他终端登录，当前会话已失效"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail("UNAUTHORIZED", "登录状态已过期，请重新登录"));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail("PERMISSION_DENIED", "暂无操作权限"));
    }
}
```

**9. 密码安全规范**

```java
package com.digital.employee.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordEncoder {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordEncoder() {}

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        return ENCODER.matches(rawPassword, passwordHash);
    }
}
```

**密码策略约束**：

* 存储：统一使用 BCrypt 加密，禁止明文或 MD5/SHA1。
* 复杂度：密码长度 ≥ 8 位，需包含大写字母、小写字母、数字中的至少两类。
* 防暴力破解：登录接口采用 `IP + 账号` 维度限流，连续 5 次失败后锁定 15 分钟。

### 2.5 后端工程目录结构

采用聚合单体工程规范划分模块：

```text
digital-employee-java/                        # 父工程 POM
├── pom.xml
├── common/                                   # 基础设施下沉
│   └── src/main/java/com/digital/employee/common/
│       ├── core/                             # 统一信封 Result<T>、枚举、常量
│       ├── exception/                        # GlobalExceptionHandler 与自定义异常
│       ├── redis/                            # Redis 配置、工具类与 RedisConstants 常量
│       └── utils/                            # 加密、脱敏、上下文工具
├── system/                                   # RBAC 权限与系统核心
│   └── src/main/java/com/digital/employee/system/
│       ├── config/                           # SaTokenConfigure, CorsConfigure
│       ├── satoken/                          # StpInterfaceImpl
│       ├── domain/                           # 实体 (SysUser, SysRole, SysMenu) 与 DTO/VO
│       ├── mapper/                           # MyBatis-Plus 数据仓储
│       ├── service/                          # 用户、角色、菜单管理服务
│       └── controller/                       # AuthController, UserController 等
├── business/                                 # 核心业务模块
│   └── src/main/java/com/digital/employee/business/
│       ├── agent/                            # AI 运行时
│       └── bot/                              # Bot 配置管理
└── app/                                      # 启动与集成模块
    └── src/main/
        ├── java/com/digital/employee/
        │   └── Application.java              # 唯一 Spring Boot 启动类
        └── resources/
            ├── application.yml               # 通用配置
            ├── application-dev.yml           # 开发环境配置
            ├── application-prod.yml          # 生产环境配置
            └── db/migration/                 # PostgreSQL 脚本 (Flyway)
```

---

## 3. RBAC 数据模型（单轨 4 表模型）

将页面路由、菜单展示与后端接口权限统一收敛到 `sys_menu` 表，角色分配时只需勾选单一菜单树，避免配置脱节。业务模型采用**"用户仅绑定单角色"**约束：`sys_user` 直接内嵌 `role_id` 外键，废弃多对多关联表 `sys_user_role`，彻底消除多角色笛卡尔积授权冲突与用户级权限并集计算的复杂度。

### 3.1 DDL 设计（PostgreSQL 规范）

```sql
-- 1. 用户表（直接内嵌 role_id，单用户绑定单角色）
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20) UNIQUE,
    role_id BIGINT NOT NULL REFERENCES sys_role(id),   -- 单角色外键约束
    status SMALLINT NOT NULL DEFAULT 1,                 -- 1=正常, 0=禁用
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_user_role_id ON sys_user(role_id);
CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_phone ON sys_user(phone);

-- 2. 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_key VARCHAR(64) NOT NULL UNIQUE,       -- super_admin / admin / user
    role_name VARCHAR(64) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 菜单与权限统一表 (承载目录、路由页面、按钮与接口操作)
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(64) NOT NULL,
    menu_type CHAR(1) NOT NULL,                 -- M=目录, C=菜单页面, F=按钮与操作
    path VARCHAR(128),                          -- 前端路由 (仅 M/C)
    component VARCHAR(128),                     -- 组件路径 (仅 C)
    perms VARCHAR(128),                         -- 权限标识 (如 admin:user:manage)
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    visible SMALLINT NOT NULL DEFAULT 1,        -- 1=显示, 0=隐藏
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_menu_parent ON sys_menu(parent_id);
CREATE INDEX idx_sys_menu_perms ON sys_menu(perms);

-- 4. 角色-菜单关联表
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    menu_id BIGINT NOT NULL REFERENCES sys_menu(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_id)
);
CREATE INDEX idx_role_menu_mid ON sys_role_menu(menu_id);

-- 5. 审计日志表（等保合规，非 RBAC 核心表）
CREATE TABLE sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    ip VARCHAR(64),
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    method VARCHAR(10),
    url VARCHAR(256),
    duration_ms INT,
    status SMALLINT NOT NULL DEFAULT 1,         -- 1=成功, 0=失败
    error_msg TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_log_user ON sys_audit_log(user_id);
CREATE INDEX idx_audit_log_created ON sys_audit_log(created_at);
```

### 3.2 RBAC 数据模型 ER 关系

```mermaid
erDiagram
    SYS_USER }o--|| SYS_ROLE : "绑定"
    SYS_ROLE ||--o{ SYS_ROLE_MENU : "授权"
    SYS_MENU ||--o{ SYS_ROLE_MENU : "归属"

    SYS_USER {
        BIGSERIAL id PK
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR nickname
        VARCHAR phone UK
        BIGINT role_id FK
        SMALLINT status
        BOOLEAN must_change_password
    }

    SYS_ROLE {
        BIGSERIAL id PK
        VARCHAR role_key UK
        VARCHAR role_name
        SMALLINT status
    }

    SYS_MENU {
        BIGSERIAL id PK
        BIGINT parent_id
        VARCHAR title
        CHAR menu_type
        VARCHAR path
        VARCHAR component
        VARCHAR perms
        VARCHAR icon
        INT sort_order
        SMALLINT visible
    }
```

### 3.3 预置权限标识清单（16 核心权限码）

| 权限码 | 说明 |
| :--- | :--- |
| `admin:user:manage` | 用户管理（增删改） |
| `admin:user:readonly` | 用户查看 |
| `admin:permission:manage` | 权限管理（增删改） |
| `admin:permission:readonly` | 权限查看 |
| `admin:invite_code:manage` | 邀请码管理 |
| `admin:invite_code:readonly` | 邀请码查看 |
| `admin:menu:manage` | 菜单管理 |
| `admin:menu:readonly` | 菜单查看 |
| `admin:data_platform:dashboard` | 数据平台仪表盘 |
| `admin:data_platform:data_items` | 数据平台数据项 |
| `admin:data_platform:config` | 数据平台配置 |
| `admin:bot:manage` | Bot 管理 |
| `admin:bot:readonly` | Bot 查看 |
| `admin:agent:manage` | Agent 管理 |
| `admin:agent:readonly` | Agent 查看 |
| `admin:observability:log:view` | 可观测性日志查看 |

### 3.4 初始化 SQL 脚本

```sql
-- 初始角色
INSERT INTO sys_role (role_key, role_name, status)
VALUES
    ('super_admin', '超级管理员', 1),
    ('admin', '管理员', 1),
    ('user', '普通用户', 1);

-- 初始管理员用户 (密码: Admin@123456, BCrypt 加密)，直接绑定 super_admin 单角色
INSERT INTO sys_user (username, password_hash, nickname, role_id, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员',
        (SELECT id FROM sys_role WHERE role_key = 'super_admin'), 1);

-- 初始菜单与权限 (示例：系统管理目录 + 用户管理页面 + 用户管理权限码)
INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, perms, icon, sort_order)
VALUES
    (1, 0, '系统管理', 'M', '/system', NULL, NULL, 'setting', 1),
    (2, 1, '用户管理', 'C', '/system/user', 'system/user/UserManage', 'admin:user:readonly', 'user', 1),
    (3, 2, '用户新增', 'F', NULL, NULL, 'admin:user:manage', NULL, 1),
    (4, 2, '用户编辑', 'F', NULL, NULL, 'admin:user:manage', NULL, 2),
    (5, 2, '用户删除', 'F', NULL, NULL, 'admin:user:manage', NULL, 3);

-- super_admin 角色拥有全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT (SELECT id FROM sys_role WHERE role_key = 'super_admin'), id FROM sys_menu;
```

---

## 4. 前端开发规范（`digital-employee-web`）

严格遵循 `frontend-spec` 前端规范：使用 TS 全量类型推导、禁用 `any`、组件采用大驼峰命名、工具与目录采用短横线、样式符合 BEM 且嵌套不超 3 层。

### 4.1 前端工程目录树 (`src/`)

```text
src/
├── api/                             # 接口定义（kebab-case）
│   ├── types.ts                     # 统一信封接口与领域模型
│   ├── auth-api.ts                  # 登录、登出、/me 获取资料
│   └── system-api.ts                # 用户、角色、菜单树接口
├── assets/                          # 静态资源
├── components/                      # 公共组件（PascalCase）
│   ├── CaptchaInput.vue             # 算术验证码组件
│   └── TablePagination.vue          # 分页展示组件
├── composables/                     # 业务复用组合式函数
│   ├── use-auth.ts                  # 认证生命周期管理
│   └── use-table.ts                 # 表格查询与防抖封装
├── constants/                       # 全局常量（全大写下划线）
│   ├── access-codes.ts              # 16 个权限标识常量
│   └── app-keys.ts                  # Storage Key 与路由白名单
├── directives/                      # 自定义指令
│   └── permission.ts                # v-hasPermi 指令 + checkPermi 工具函数
├── layouts/                         # 基础骨架（PascalCase）
│   ├── MainLayout.vue               # 主容器入口
│   └── components/
│       ├── SideBar.vue              # 动态侧边栏
│       └── TopNavbar.vue            # 顶部导航操作栏
├── router/                          # 路由配置
│   ├── index.ts                     # 静态路由（/login, /404）
│   └── guard.ts                     # 全局 beforeEach 守卫与 addRoute 注入
├── store/                           # 状态管理（Pinia）
│   ├── index.ts
│   └── modules/
│       ├── user.ts                  # 用户凭据、Token、权限集合
│       └── permission.ts            # 动态菜单树解析与生成
├── styles/                          # SCSS 全局样式
│   ├── index.scss
│   └── variables.scss               # 布局与色彩变量
├── utils/                           # 通用工具函数
│   ├── request.ts                   # Axios 拦截器与 401 登出
│   └── storage.ts                   # Storage 统一安全封装
└── views/                           # 页面视图（kebab-case 目录 + PascalCase 组件）
    ├── auth/
    │   └── LoginView.vue
    ├── dashboard/
    │   └── DashboardView.vue
    └── system/
        ├── user/
        │   └── UserManage.vue
        └── role/
            └── RoleManage.vue
```

### 4.2 前端核心实现

**1. Axios 封装与被顶下线阻断 (`src/utils/request.ts`)**

```typescript
import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { IApiResponse } from '@/api/types';
import { getStorage, clearStorage } from '@/utils/storage';
import { STORAGE_KEYS } from '@/constants/app-keys';

const HTTP_STATUS = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
} as const;

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
});

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response: AxiosResponse<IApiResponse>) => {
    const { data } = response;
    if (data && data.code !== 'OK') {
      ElMessage.error(data.message || '请求失败');
      return Promise.reject(new Error(data.message));
    }
    return data;
  },
  (error) => {
    const { response } = error;
    if (response) {
      const { status, data } = response;
      if (status === HTTP_STATUS.UNAUTHORIZED) {
        if (data?.code === 'SESSION_REPLACED') {
          ElMessageBox.alert('您的账号已在其他终端登录，当前会话已失效。', '下线提示', {
            confirmButtonText: '重新登录',
            type: 'warning',
            callback: () => {
              clearStorage();
              window.location.href = '/login';
            },
          });
        } else {
          ElMessage.error(data?.message || '登录已失效，请重新登录');
          clearStorage();
          window.location.href = '/login';
        }
      } else if (status === HTTP_STATUS.FORBIDDEN) {
        ElMessage.error(data?.message || '暂无操作权限');
      } else {
        ElMessage.error(data?.message || '网络连接异常');
      }
    }
    return Promise.reject(error);
  }
);

export default request;
```

**1. 按钮级权限指令 (`src/directives/permission.ts`)**

遵循若依实现：通过 `mounted` 钩子从父容器彻底移除无权限 DOM，防止 F12 调试篡改样式显示按钮：

```typescript
import type { App, DirectiveBinding } from 'vue';
import { useUserStore } from '@/store/modules/user';

const ALL_PERMISSION = '*:*:*';

export const setupPermissionDirective = (app: App): void => {
  app.directive('hasPermi', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string[] | string>) {
      const { value } = binding;
      const allPermissions = useUserStore().permissions || [];

      if (value && (Array.isArray(value) ? value.length > 0 : !!value)) {
        const targetPerms = Array.isArray(value) ? value : [value];

        const hasPermission = allPermissions.some((perm) => {
          return perm === ALL_PERMISSION || targetPerms.includes(perm);
        });

        if (!hasPermission && el.parentNode) {
          el.parentNode.removeChild(el);
        }
      } else {
        throw new Error('v-hasPermi 必须绑定权限标识，例如：v-hasPermi="[\'admin:user:manage\']"');
      }
    },
  });
};
```

**2. 全局通用权限校验工具 (`src/utils/permission.ts`)**

> **若依关键避坑规范**：在 `el-table-column` 操作列、复杂行内循环或需要动态禁用/显示的场景中，**禁止使用 `v-hasPermi` 指令（避免 `removeChild` 导致 Vue Virtual DOM diff 错位崩溃）**，必须使用 `checkPermi` 配套 `v-if`：

```typescript
import { useUserStore } from '@/store/modules/user';

const ALL_PERMISSION = '*:*:*';

export const checkPermi = (value: string[] | string): boolean => {
  if (!value || (Array.isArray(value) && value.length === 0)) {
    return false;
  }
  const allPermissions = useUserStore().permissions || [];
  const targetPerms = Array.isArray(value) ? value : [value];

  return allPermissions.some((perm) => {
    return perm === ALL_PERMISSION || targetPerms.includes(perm);
  });
};
```

> **运行时热切局限**：指令仅在 `mounted` 阶段执行，权限变更后不会自动刷新 DOM。常规解法：权限变更后调用 `window.location.reload()` 强制刷新页面，或通过 Vue `key` 强制重建组件。

### 4.3 `/api/v1/auth/me` 接口契约

登录成功后前端调用此接口获取当前用户信息、角色与权限集合，用于动态路由生成与按钮级鉴权。

**请求**

```
GET /api/v1/auth/me
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "user": {
      "userId": 1,
      "username": "admin",
      "nickname": "超级管理员"
    },
    "roles": ["super_admin"],
    "permissions": ["*:*:*", "admin:user:readonly", "admin:user:manage"],
    "menus": [
      {
        "id": 1,
        "parentId": 0,
        "title": "系统管理",
        "menuType": "M",
        "path": "/system",
        "icon": "Setting",
        "children": [
          {
            "id": 2,
            "parentId": 1,
            "title": "用户管理",
            "menuType": "C",
            "path": "user",
            "component": "system/user/UserManage",
            "icon": "User",
            "children": []
          }
        ]
      }
    ]
  }
}
```

> **设计规则**：`menus` 仅返回当前用户可访问的 M（目录）和 C（菜单页面）树结构，供动态路由渲染；**menus 节点内部绝不携带 permissions 数组**；所有的按钮与操作权限码，统一平铺在根节点的 `permissions` 字符串列表中。

前端 `permission.ts` Store 解析 `menus` 递归生成路由。`permissions` 为根节点的扁平字符串列表（`List<String>`），供 `v-hasPermi` 指令与 `checkPermi` 函数使用。

### 4.4 动态路由解析与守卫 (`src/router/guard.ts`)

对齐若依在单页面应用首次加载与刷新时的标准路由拦截，通过 `import.meta.glob` 实现组件懒加载：

```typescript
import router from '@/router';
import { useUserStore } from '@/store/modules/user';
import { usePermissionStore } from '@/store/modules/permission';
import { getStorage } from '@/utils/storage';
import { STORAGE_KEYS } from '@/constants/app-keys';

const WHITE_LIST = ['/login', '/404'];

router.beforeEach(async (to, from, next) => {
  const token = getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN);
  const userStore = useUserStore();
  const permissionStore = usePermissionStore();

  if (token) {
    if (to.path === '/login') {
      next({ path: '/' });
    } else {
      if (userStore.roles.length === 0) {
        try {
          const { menus } = await userStore.fetchUserInfo();
          const accessRoutes = permissionStore.generateRoutes(menus);
          accessRoutes.forEach((route) => router.addRoute(route));
          next({ ...to, replace: true });
        } catch (error) {
          await userStore.logout();
          next(`/login?redirect=${to.path}`);
        }
      } else {
        next();
      }
    }
  } else {
    if (WHITE_LIST.includes(to.path)) {
      next();
    } else {
      next(`/login?redirect=${to.path}`);
    }
  }
});
```

> **`generateRoutes` 核心逻辑**：通过 `import.meta.glob('@/views/**/*.vue')` 获取全部页面组件映射，将 `menus` 树中的 `component` 字符串（如 `"system/user/UserManage"`）动态匹配为懒加载组件函数（`() => import('@/views/system/user/UserManage.vue')`）。路由 `path` 与 `meta.title` / `meta.icon` 从菜单节点直接映射，实现后端驱动的完整路由注册。

---

## 5. 项目分阶段实施与交付计划

| 阶段 | 核心目标 | 交付物 |
| :--- | :--- | :--- |
| **Phase 0** | 基础工程与环境基线就绪 | Spring Boot 4.0.3 POM 依赖调通、5 张表 DDL 执行脚本、预置初始角色与权限码、`application-dev.yml` / `application-prod.yml` 环境配置模板 |
| **Phase 1** | 后端鉴权闭环 | `SaTokenConfigure`、`CorsConfigure`、`StpInterfaceImpl`（含缓存读写）组装完成，`@SaCheckPermission` 与全局异常映射单元测试通过，BCrypt 密码工具就绪 |
| **Phase 2** | 系统管理 CRUD | 用户管理、角色菜单分配接口与 `/api/v1/auth/me`（动态菜单树 + 权限集合）完成开发并验证，权限缓存失效联动测试 |
| **Phase 3** | 前端骨架与动态路由 | Vite 初始化、Axios 拦截器（含业务错误码处理）、Pinia 模块、动态路由追加逻辑（`router/guard.ts` + `import.meta.glob`）、`v-hasPermi` 指令与 `checkPermi` 工具函数联调 |
| **Phase 4** | 系统联调与安全加固 | 单会话多标签页被踢联动测试、登录限流防暴力破解、接口压测与联调上线 |

### 环境与规范约束

| 类别 | 规范 |
| :--- | :--- |
| **环境配置** | 三套配置文件：`application-dev.yml`（本地开发）、`application-test.yml`（测试）、`application-prod.yml`（生产），敏感配置走环境变量注入 |
| **单元测试** | 鉴权链路（`StpInterfaceImpl`、异常映射）须覆盖，覆盖率 ≥ 70% |
| **集成测试** | Phase 2 完成后，对 `/api/v1/auth/me`、角色菜单分配联动做端到端验证 |
| **日志规范** | 生产环境 `sa-token.is-log: false`；业务日志使用 SLF4J + Logback，禁止 `System.out`；登录行为（成功/失败/被踢）须记录审计日志 |
| **Redis Key 规范** | 所有 Redis Key 统一设置 TTL，禁止永久 Key；会话 Key 由 Sa-Token 管理；业务缓存 Key 前缀统一由 `RedisConstants` 常量类管理，格式：`cache:role:perms:{roleKey}:{roleVersion}`（角色共享）、`sys:role:version:{roleKey}`、`login:rate:ip:{ip}`、`login:rate:ipacct:{ip}:{username}` |

---

## 6. 已知限制与产品约束

| 项目 | 说明 | 影响范围 |
| :--- | :--- | :--- |
| **被踢下线延迟感知** | Sa-Token 基于 HTTP 请求拦截检测会话状态，已打开的浏览器标签页不会实时弹窗。只有用户在被踢后发起下一次请求时，才触发 401 `SESSION_REPLACED` 弹窗提示 | 产品体验 |
| **`v-hasPermi` 不支持运行时热切** | 自定义指令仅在 `mounted` 阶段执行，权限变更后不会自动刷新 DOM。需要刷新页面或强制重建组件才生效。复杂循环场景（`el-table-column` 操作列等）须使用 `checkPermi` + `v-if` 替代指令 | 权限变更场景 |
| **MyBatis-Plus Boot 4 适配** | Boot 4 为极新版本，需确认 MyBatis-Plus 官方 `mybatis-plus-spring-boot4-starter` 对 Jakarta 命名空间的完整支持，建议锁定稳定版 | 构建与启动 |
| **CORS 白名单** | `CorsConfigure` 中的 `allowedOriginPatterns` 需在部署时根据实际域名修改，开发环境使用 `localhost:*` | 部署配置 |
