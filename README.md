# sps4j - Simple Plugin System for Java

sps4j 是一个为Java设计的轻量级、简单易用的插件化框架。它旨在帮助开发者构建模块化的应用程序，使得功能可以作为独立的插件进行开发、部署和管理，而无需重新编译主程序。sps4j尤其对Spring Boot应用提供了良好的集成支持。

## ✨ 特性

- **插件发现与生命周期管理**: 自动从指定路径发现插件，并管理其加载和卸载生命周期。
- **隔离的类加载机制**: 采用父类优先（Parent-First）和子类优先（Child-First）相结合的类加载机制，既保证框架核心类的统一，又实现了插件间依赖的隔离。
- **注解驱动**: 通过简单的注解（`@Sps4jPlugin` 和 `@Sps4jPluginInterface`）即可定义和声明一个插件。
- **Spring Boot集成**:
    - 插件本身可以是一个完整的Spring Boot应用（通过继承`SpringBoot2AppPlugin`）。
    - 支持将插件的Web层（如Controller）无缝集成到主应用的Tomcat实例中。
    - 支持在普通插件中访问主应用的bean。
- **版本控制**: 插件可以声明其与主应用兼容的版本范围，实现平滑升级。

## 📦 模块介绍

- `sps4j-annotation`: 定义了`@Sps4jPlugin`和`@Sps4jPluginInterface`等核心注解，以及用于编译时处理注解的处理器。
- `sps4j-common`: 包含了框架使用的一些通用类。
- `sps4j-core`: 框架的核心实现，包括`PluginManager`、类加载器以及插件生命周期管理等。
- `sps4j-spring-boot2`: 提供了与Spring Boot 2.x集成的支持层，包含了让插件作为Spring Boot应用运行的适配器和自动配置。
- `sps4j-plugin-parent`: 一个Maven父项目，插件项目可以继承它来简化依赖管理。
- `sps4j-examples`: 包含了使用sps4j的示例代码。

## 🚀 使用方法

### 基本使用方法

#### 步骤 1: 定义插件接口（契约模块）

首先，创建一个独立的Maven模块（例如 `greeter-api`）来定义插件接口。这个模块是主应用和插件实现之间的契约。

- **添加maven依赖**:
  ```xml
  <dependencies>
      <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>sps4j-core</artifactId>
          <version>${sps4j.version}</version>
          <scope>provided</scope>
      </dependency>
  </dependencies>
  ```

- **定义插件接口**:
  ```java
  import io.github.sps4j.annotation.Sps4jPluginInterface;
  import io.github.sps4j.core.Sps4jPlugin;

  @Sps4jPluginInterface("greeter")
  public interface GreeterPlugin extends Sps4jPlugin {
      String greet(String name);
  }
  ```

#### 实现插件

你可以为同一个接口提供多个实现。只需确保每个实现的 `@Sps4jPlugin` 注解中的 `name` 是唯一的。示例如下：

- **实现一: `hello-plugin`**

  maven依赖继承`sps4j-plugin-parent`并添加`greeter-api`依赖，scope为`provided`。 然后实现接口插件接口。
  ```java
  @Sps4jPlugin(name = "hello", version = "1.0.0", productVersionConstraint = ">=1.0")
  public class HelloPlugin implements GreeterPlugin {
      @Override
      public String greet(String name) {
          return "Hello, " + name + "!";
      }
  }
  ```

- **实现二: `bye-plugin`**
    ```java
    @Sps4jPlugin(name = "bye", version = "1.0.0", productVersionConstraint = ">=1.0")
    public class ByePlugin implements GreeterPlugin {
        @Override
        public String greet(String name) {
            return "Bye, " + name + "!";
        }
    }
    ```

#### 步骤 3: 搭建主应用

在主应用模块中，配置`PluginManager`并使用插件。

- **主应用 `pom.xml`**:
  ```xml
  <dependencies>
      <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>sps4j-core</artifactId>
          <version>${sps4j.version}</version>
      </dependency>
      <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>greeter-api</artifactId>
          <version>1.0.0</version>
      </dependency>
  </dependencies>
  ```

- **主应用**:
  ```java
  public class Main {
      public static void main(String[] args) throws Exception {
          ProductPluginLoadService productService = () -> Version.parse("1.0.0");
          File pluginDir = new File("plugins");
          pluginDir.mkdirs();
          
          PluginManager pluginManager = new DefaultPluginManager(
              pluginDir.toURI().toURL().toString(),
              productService,
              new DefaultPluginLoader()
          );

          // 使用 getPluginsUnwrapped 获取所有 "greeter" 类型的插件
          List<GreeterPlugin> allGreeters = pluginManager.getPluginsUnwrapped(
              GreeterPlugin.class,
              Collections.emptyMap()
          );

          // 遍历并调用所有插件
          System.out.println("Found " + allGreeters.size() + " greeter plugins.");
          for (GreeterPlugin plugin : allGreeters) {
              System.out.println(plugin.greet("World"));
          }
      }
  }
  ```

#### 步骤 4: 构建和运行
1.  **构建插件**: 分别在 `hello-plugin` 和 `bye-plugin` 模块中运行 `mvn clean package`。
2.  **部署插件**: 将生成的 `hello-plugin-1.0.0.jar` 和 `bye-plugin-1.0.0.jar` 复制到主应用可访问的目录。
3.  **运行主应用**: 执行 `Main.main` 方法。
    ```
    Found 2 greeter plugins.
    Hello, World!
    Bye, World!
    ```

---

### 在插件中启动spring-boot应用

利用`sps4j-spring-boot2`模块，可以实现更深度的集成，让插件自身成为一个spring-boot应用，当前支持spring-boot 2.x版本。

#### 步骤 1: 定义插件接口（契约模块）
这一步与一般插件完全相同。你需要一个独立的 `greeter-api` 模块来定义`GreeterPlugin`接口。

#### 步骤 2: 实现Spring Boot插件
插件不仅可以实现业务逻辑，还可以包含自己的Controller、Service等。

- **maven依赖**:
  继承`sps4j-plugin-parent`，并添加`sps4j-spring-boot2` 依赖
  ```xml
  <parent>
      <groupId>io.github.qchole</groupId>
      <artifactId>sps4j-plugin-parent</artifactId>
      <version>${sps4j.version}</version>
  </parent>
  <dependencies>
      <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>greeter-api</artifactId>
          <version>1.0.0</version>
          <scope>provided</scope>
      </dependency>
      <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>sps4j-spring-boot2</artifactId>
          <scope>provided</scope>
      </dependency>
  </dependencies>
  ```

- **实现插件接口并标记插件注解，springboot应用注解**（插件注解`tags`可以添加`SpringBoot2AppPlugin.TAG_SPRING_MVC`，此时插件将会作为spring-mvc应用启动，可以在插件中暴露web端点。目前仅支持tomcat作为webserver）
    ```java
    @Sps4jPlugin(
        name = "spring-hello",
        version = "1.0.0",
        productVersionConstraint = ">=1.0",
        tags = {SpringBoot2AppPlugin.TAG_SPRING_MVC} // 标记为Web应用
    )
    @SpringBootApplication
    public class SpringHelloPlugin extends SpringBoot2AppPlugin implements GreeterPlugin {
        @Override
        public String greet(String name) {
            return "Hello from Spring, " + name + "!";
        }
    }
    ```

- **插件中添加一个controller**:
  ```java
  @RestController
  public class PluginController {
      @GetMapping("/hello")
      public String handle() {
          return "This response comes from a controller inside the plugin!";
      }
  }
  ```

- **插件应用配置文件`application.yml`**:
  为了避免与主应用或其他插件的端点冲突，建议为每个Web插件设置独立的上下文路径（Context Path）。
  ```yaml
  server:
    servlet:
      context-path: /my-plugin
  ```

#### 步骤 3: 搭建Spring Boot主应用
- maven依赖添加`sps4j-spring-boot2`，api模块
  ```xml
  <dependencies>
        <dependency>
            <groupId>io.github.qchole</groupId>
            <artifactId>sps4j-spring-boot2</artifactId>
            <version>${sps4j.version}</version>
        </dependency>
        <dependency>
          <groupId>io.github.qchole</groupId>
          <artifactId>greeter-api</artifactId>
          <version>1.0.0</version>
      </dependency>
    </dependencies>

- 添加启动类
  ```java
  @SpringBootApplication
  public class Main {
      public static void main(String[] args) {
         SpringApplication.run(Main.class, args);
      }
  }
  ```
- 创建`PlugManager`
  ```java
    @Configuration
    public class PluginConfig {
        @Autowired
        private ResourceLoader resourceLoader;

        @Bean
        public ProductPluginLoadService productPluginLoadService() {
            return () -> Version.parse("1.0.0");
        }

        @Bean
        public PluginManager loader(ProductPluginLoadService productPluginLoadService) throws IOException {
            Resource resource = resourceLoader.getResource("classpath:plugin");
            return new DefaultPluginManager(resource.getURL().toString(),
                productPluginLoadService, new SpringAppSupportPluginLoader());
        }
  }
  ```
- 在主应用中加载插件
  ```java
  @RestController
  public class HostController {
      @Autowired
      private PluginManager pluginManager;
      @GetMapping("/load")
      public String load() {
          pluginManager.getPluginUnwrapped(GreeterPlugin.class,
                  PluginArtifact.builder()
                  .type("greeter").name("spring-hello").build(),
                  Collections.emptyMap());
          return "load ok ";
      }
  }

  ```

#### 步骤 4: 构建和运行
1.  **构建插件**: 在插件项目中运行 `mvn clean package`。
2.  **部署插件**: 将插件jar包复制到主应用可访问的目录下。
3.  **运行主应用**: 启动Spring Boot主应用。

#### 步骤 5: 访问插件的Web端点
由于插件设置了上下文路径，现在它的所有端点都在`/my-plugin`下。

打开浏览器或使用curl，访问 `http://localhost:8080/my-plugin/hello`。

将会得到响应:
```
This response comes from a controller inside the plugin!
```
这证明了插件的Web层已经成功运行在主应用的服务中，并且拥有自己独立的命名空间，避免了路由冲突。同时，你依然可以在主应用中通过`PluginManager`获取`GreeterPlugin`的实例并调用其方法。


## 📖 示例

一个完整的、可运行的示例可以在 `sps4j-examples/spring-boot2-example` 目录下找到。该示例包含了一个基础的主应用（`host-application`）和一个插件应用（`plugin-app`），完整地演示了上述所有步骤。

## 🛠️ 从源码构建

1.  克隆本项目: `git clone https://github.com/qchole/sps4j.git`
2.  进入项目根目录: `cd sps4j`
3.  使用Maven进行构建: `mvn clean package`
