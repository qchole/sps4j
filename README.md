# sps4j - A Simple Plugin System for Java

sps4j is a lightweight and easy-to-use plugin framework designed for Java. It aims to help developers build modular applications where features can be developed, deployed, and managed as independent plugins without recompiling the main program. sps4j provides excellent integration support, especially for Spring Boot applications.

## ✨ Features

- **Plugin Discovery and Lifecycle Management**: Automatically discovers plugins from a specified path and manages their loading and unloading lifecycle.
- **Isolated Class Loading**: Uses a combination of Parent-First and Child-First class loading mechanisms to ensure the unity of core framework classes while isolating dependencies between plugins.
- **Annotation-Driven**: Define and declare a plugin with simple annotations (`@Sps4jPlugin` and `@Sps4jPluginInterface`).
- **Spring Boot Integration**:
    - The plugin itself can be a complete Spring Boot application (by extending `SpringBoot2AppPlugin`).
    - Seamlessly integrates the plugin's web layer (e.g., Controllers) into the host application, supports Tomcat and Jetty as web server.
    - Supports accessing beans from the host application within the plugin.
- **Versioning**: Plugins can declare a compatible version range with the host application, enabling smooth upgrades.

## 📦 Modules

- `sps4j-annotation`: Defines core annotations like `@Sps4jPlugin` and `@Sps4jPluginInterface`, as well as annotation processors for compile-time processing.
- `sps4j-common`: Contains common utility classes used by the framework.
- `sps4j-core`: The core implementation of the framework, including `PluginManager`, class loaders, and plugin lifecycle management.
- `sps4j-spring-boot2`: Provides the support layer for integration with Spring Boot 2.x, including adapters and auto-configuration to run plugins as Spring Boot applications.
- `sps4j-plugin-parent`: A Maven parent project that plugin projects can inherit from to simplify dependency management.
- `sps4j-examples`: Contains example code for using sps4j.

## 🚀 Usage

### Basic Usage

#### Step 1: Define the Plugin Interface (Contract Module)

First, create a separate Maven module (e.g., `greeter-api`) to define the plugin interface. This module serves as the contract between the host application and the plugin implementations.

- **Add Maven Dependency**:
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

- **Define the Plugin Interface**:
  ```java
  import io.github.sps4j.annotation.Sps4jPluginInterface;
  import io.github.sps4j.core.Sps4jPlugin;

  @Sps4jPluginInterface("greeter")
  public interface GreeterPlugin extends Sps4jPlugin {
      String greet(String name);
  }
  ```

#### Step 2: Implement Plugins

You can provide multiple implementations for the same interface. Just ensure that the `name` in each implementation's `@Sps4jPlugin` annotation is unique. For example:

- **Implementation 1: `hello-plugin`**

  Inherit `sps4j-plugin-parent` and add the `greeter-api` dependency with `provided` scope in your Maven `pom.xml`. Then, implement the interface.
  ```java
  @Sps4jPlugin(name = "hello", version = "1.0.0", productVersionConstraint = ">=1.0")
  public class HelloPlugin implements GreeterPlugin {
      @Override
      public String greet(String name) {
          return "Hello, " + name + "!";
      }
  }
  ```

- **Implementation 2: `bye-plugin`**
    ```java
    @Sps4jPlugin(name = "bye", version = "1.0.0", productVersionConstraint = ">=1.0")
    public class ByePlugin implements GreeterPlugin {
        @Override
        public String greet(String name) {
            return "Bye, " + name + "!";
        }
    }
    ```

#### Step 3: Set up the Host Application

In the host application module, configure the `PluginManager` and use the plugins.

- **Host Application `pom.xml`**:
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

- **Host Application Code**:
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

          // Use getPluginsUnwrapped to get all "greeter" type plugins
          List<GreeterPlugin> allGreeters = pluginManager.getPluginsUnwrapped(
              GreeterPlugin.class,
              Collections.emptyMap()
          );

          // Iterate and call all plugins
          System.out.println("Found " + allGreeters.size() + " greeter plugins.");
          for (GreeterPlugin plugin : allGreeters) {
              System.out.println(plugin.greet("World"));
          }
      }
  }
  ```

#### Step 4: Build and Run
1.  **Build the plugins**: Run `mvn clean package` in the `hello-plugin` and `bye-plugin` modules respectively.
2.  **Deploy the plugins**: Copy the generated `hello-plugin-1.0.0.jar` and `bye-plugin-1.0.0.jar` to a directory accessible by the host application.
3.  **Run the host application**: Execute the `Main.main` method.
    ```
    Found 2 greeter plugins.
    Hello, World!
    Bye, World!
    ```

---

### Running a Spring Boot Application inside a Plugin

By leveraging the `sps4j-spring-boot2` module, you can achieve deeper integration, allowing the plugin itself to be a Spring Boot application. Currently, Spring Boot 2.x is supported.

#### Step 1: Define the Plugin Interface (Contract Module)
This step is identical to the basic usage. You need a separate `greeter-api` module to define the `GreeterPlugin` interface.

#### Step 2: Implement the Spring Boot Plugin
The plugin can not only implement business logic but also contain its own Controllers, Services, etc.

- **Maven Dependencies**:
  Inherit `sps4j-plugin-parent` and add the `sps4j-spring-boot2` dependency.
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

- **Implement the plugin interface and add annotations** (The `@Sps4jPlugin` annotation's `tags` can include `SpringBoot2AppPlugin.TAG_SPRING_MVC`. This will start the plugin as a Spring MVC application, allowing you to expose web endpoints. Currently, Tomcat and Jetty are supported as the web server).
    ```java
    @Sps4jPlugin(
        name = "spring-hello",
        version = "1.0.0",
        productVersionConstraint = ">=1.0",
        tags = {SpringBoot2AppPlugin.TAG_SPRING_MVC} // Mark as a Web application
    )
    @SpringBootApplication
    public class SpringHelloPlugin extends SpringBoot2AppPlugin implements GreeterPlugin {
        @Override
        public String greet(String name) {
            return "Hello from Spring, " + name + "!";
        }
    }
    ```

- **Add a Controller in the plugin**:
  ```java
  @RestController
  public class PluginController {
      @GetMapping("/hello")
      public String handle() {
          return "This response comes from a controller inside the plugin!";
      }
  }
  ```

- **Plugin's Sevlet Context Path**:
  By default, Sps4j will add plugin type and name as prefix of servlet context path in the form of `/{pluginType}/{PluginName}`, for example the following servlet context path in plugin's `application.yaml`.
  ```yaml
  server:
    servlet:
      context-path: /my-plugin
  ```
  The full path will be `/greeter/spring-hello/my-plugin`. You can disable this feature by add following configuration to the plugin's config file `application.yml`
  ```yaml
  sps4j:
    spring-mvc:
      add-servlet-context-prefix: false

  ```

#### Step 3: Set up the Spring Boot Host Application
- Add the `sps4j-spring-boot2` and `greeter-api` dependencies to your Maven `pom.xml`.
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
  ```
- Add a main class.
  ```java
  @SpringBootApplication
  public class Main {
      public static void main(String[] args) {
         SpringApplication.run(Main.class, args);
      }
  }
  ```
- Create the `PluginManager`.
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
- Load the plugin in the host application.
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

#### Step 4: Build and Run
1.  **Build the plugin**: Run `mvn clean package` in the plugin project.
2.  **Deploy the plugin**: Copy the plugin JAR to a directory accessible by the host application.
3.  **Run the host application**: Start the Spring Boot host application.

#### Step 5: Access the Plugin's Web Endpoint
Since the plugin has a context path, all its endpoints are now under `/greeter/spring-hello/my-plugin`.

Open a browser or use curl to access `http://localhost:8080/greeter/spring-hello/my-plugin/hello`.

You will get the response:
```
This response comes from a controller inside the plugin!
```
This confirms that the plugin's web layer has been successfully integrated into the host application's service, running in its own namespace to avoid routing conflicts. Meanwhile, you can still get an instance of `GreeterPlugin` via `PluginManager` in the host application and call its methods.

#### Get Spring beans of host application inside plugin

You can get Spring beans inside the plugin via the `HostApplicationContextHolder` which holds the `ApplicationContext` of the host spring application.

```java
  HostApplicationContextHolder.getHostAppContext().getBean(beanClass);
```


## 📖 Examples

A complete, runnable example can be found in the `sps4j-examples/spring-boot2-example` directory. This example includes a host application (`host-application`) and a plugin application (`plugin-app`), demonstrating all the steps described above.

## 🛠️ Building from Source

1.  Ensure you have Java Development Kit (JDK) 11 or higher installed.
2.  Clone the repository: `git clone https://github.com/qchole/sps4j.git`
2.  Navigate to the project root: `cd sps4j`
3.  Build with Maven: `mvn clean package`
