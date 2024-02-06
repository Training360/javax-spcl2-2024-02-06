# Spring Cloud

* Gyakori minták megvalósítása elosztott környezetben
  * Konfigurációkezelés
  * Service discovery
  * Routing
  * Load balancing
  * Circuit breakers
  * Rövidéletű microservice-ek (taskok)
  * Contract testing
* Kubernetes és felhőszolgáltatók támogatása
  * AWS: Spring Cloud for Amazon Web Services - community project
  * Azure
    * Dokumentáció - https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/developer-guide-overview
    * Forráskód - https://github.com/microsoft/spring-cloud-azure
  * GCP: Spring Cloud GCP is no longer part of the Spring Cloud release train.
    * https://github.com/GoogleCloudPlatform/spring-cloud-gcp
* 2015-ben jött ki
* Netflix eszközök köré egy wrapper
  * Eureka - service discovery
  * Hystrix - circuit breaker  
  * Zuul - API gateway
  * Ribbon - client-side load balancer
* Azóta bővítve, és a régi eszközök helyett érdemes újabbakat használni
  * Hystrix -> Resilience4j
  * Zuul -> Spring Cloud Gateway
  * Ribbon -> Spring Cloud LoadBalancer
  * Egyedüli megmaradó komponens az Eureka
* Spring Initializr támogatás
* Erősen függ a Spring Boot verziótól
  * Spring Boot 3.0.x, 3.1.x -> 2022.0.x aka Kilburn (2022.0.3-tól)
  * Spring Boot 3.2.x -> 2023.0.x aka Leyton
  * 2020.0 (aka Ilford) és az előttiek már nem támogatottak

# Spring Cloud Config

## Spring Cloud Config Server elindítása

* `config-server-demo`
* `spring-cloud-config-server` függőség

```java
@EnableConfigServer
```

* `application.properties`

```properties
server.port=8888
spring.cloud.config.server.git.uri=file:///C:\\trainings\\config
spring.cloud.config.server.git.default-label=master
```


* Git repo: `C:\training\config\config-client-demo.properties`

```properties
demo.prefix = Hello
logging.level.training=debug
```

Ellenőrzés URL-en: `http://localhost:8888/config-client-demo/default`

## Spring Cloud Config Client elindítása

* `config-client-demo`
* `spring-boot-starter-web`, `spring-cloud-config-client`, `lombok`


```java
@Data
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private String prefix;
}
```

```java
@RestController
@AllArgsConstructor
@EnableConfigurationProperties(DemoProperties.class)
@Slf4j
public class HelloController {

    private DemoProperties demoProperties;

    @GetMapping("/api/hello")
    public Message hello() {
        log.debug("Hello called");
        return new Message(demoProperties.getPrefix() + name);
    }
}
```

* `application.properties`

```properties
spring.config.import=configserver:
spring.application.name=config-client-demo
```

## Kódolt értékek

```shell
keytool -genkeypair -alias config-server-key -keyalg RSA -keysize 4096 -sigalg SHA512withRSA -dname "CN=Config Server,OU=Spring Cloud,O=Training" -keypass changeit -keystore config-server.jks -storepass changeit
```

* `pom.xml`


```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-rsa</artifactId>
</dependency>
```

* `application.properties`

```properties
encrypt.keyStore.location=file:config-server.jks
encrypt.keyStore.password=changeit
encrypt.keyStore.alias=config-server-key
encrypt.keyStore.secret=changeit
```

```http
###
POST http://localhost:8888/encrypt

HelloEncoded
```

* `config-client-demo.properties`

```
demo.prefix = {cipher}AgC1eiSxapB6BOni6Y7t6mL+u...
```

# Spring Cloud Bus

## Kafka indítása

Apache Kafka is an open-source distributed event streaming platform.

`docker-compose.yaml`

```yaml
services:
  kafka:
    image: docker.io/bitnami/kafka:3.4.1
    ports:
      - "9092:9092"
    environment:
      - KAFKA_ENABLE_KRAFT=yes
      - KAFKA_CFG_PROCESS_ROLES=broker,controller
      - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER 
      - KAFKA_CFG_LISTENERS=EXTERNAL://:9092,CLIENT://:9093,CONTROLLER://:9094
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=EXTERNAL:PLAINTEXT,CLIENT:PLAINTEXT,CONTROLLER:PLAINTEXT
      - KAFKA_CFG_ADVERTISED_LISTENERS=EXTERNAL://127.0.0.1:9092,CLIENT://kafka:9093
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME=CLIENT
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9094
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_BROKER_ID=1
      - ALLOW_PLAINTEXT_LISTENER=yes
  kafdrop:
    image: obsidiandynamics/kafdrop:4.0.1
    ports:
      - "9000:9000"
    environment:
      KAFKA_BROKERCONNECT: "kafka:9093"
      JVM_OPTS: "-Xms32M -Xmx64M"
      SERVER_SERVLET_CONTEXTPATH: "/"
    depends_on:
      - "kafka"
```

* Kafdrop: `http://localhost:9000`

## Konfiguráció újratöltése futásidőben

server:

`spring-cloud-config-monitor`, `spring-cloud-starter-bus-kafka` függőség

client:

* `spring-cloud-starter-bus-kafka` függőség


## Spring Cloud Bus event kezelése

server:

`lombok` függőség

```java
public class ClearCachesEvent extends RemoteApplicationEvent {

  public ClearCachesEvent(Object source, String originService, Destination destination) {
    super(source, originService, destination);
  }

}
```

```java
@RestController
@AllArgsConstructor
public class ClearCacheController {

    private ApplicationEventPublisher applicationEventPublisher;

    private BusProperties busProperties;

    private Destination.Factory factory;

    @DeleteMapping("/api/caches")
    public void clear() {
        publisher.publishEvent(new ClearCachesEvent(this, busProperties.getId(), factory.getDestination("config-client-demo")));
    }
}
```

client:

`ClearCachesEvent`

* application

```java
@RemoteApplicationEventScan(basePackageClasses = ClearCachesEvent.class)
```

```java
@Component
@Slf4j
public class ClearCachesEventListener {

    @EventListener
    public void handleEvent(ClearCachesEvent event) {
        log.info("Event handled: {}", event);
    }
}
```

```http
DELETE http://localhost:8888/api/caches
```

## HashiCorp Vault Backend

* Tárolni: tokenek, jelszavak, tanúsítványok, kulcsok
* Webes felület, CLI, HTTP API

```
docker run -d --cap-add=IPC_LOCK -e VAULT_DEV_ROOT_TOKEN_ID=myroot -p 8200:8200 --name=vault hashicorp/vault
```

* `IPC_LOCK` - érzékeny adatokat ne swappelje a diskre
* http://localhost:8200
* Token bejelentkezés, `myroot`

```shell
export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='myroot'
vault kv put secret/config-client-demo demo.prefix=HelloFromVault
```

```properties
#spring.cloud.config.server.git.uri=file:///C:\\trainings\\javax-spcl2\\config

spring.profiles.active=vault
spring.cloud.config.server.vault.kv-version=2
spring.cloud.config.server.vault.authentication=TOKEN
spring.cloud.config.server.vault.token=myroot
```

* Server indítása után a `config-server-demo.http` fájlba

```http
###
GET http://localhost:8888/config-client-demo/default
```

```http
###
GET http://localhost:8080/api/hello
```

## Frissítés HashiCorp Vault Backenddel

```shell
vault kv put secret/config-client-demo demo.prefix=HelloFromVault2
```

* Hook a `/monitor` címen, ha van `org.springframework.cloud:spring-cloud-config-monitor` és `org.springframework.cloud:spring-cloud-starter-bus-kafka`
 függőség.
  * `Content-Type` `application/x-www-form-urlencoded`
  * `path` értéke az alkalmazás neve

```http
###
POST http://localhost:8888/monitor
Content-Type: application/x-www-form-urlencoded

path=config-client-demo
```

* Kafdrop
* Client

# Spring Cloud Circuit Breaker

* Absztrakciós réteg, támogatott implementációk:
  * Resilience4J
  * Spring Retry
* Támogatott mechanizmusok:
  * CircuitBreaker
  * Bulkhead
* Resilience4J
  * CircuitBreaker
  * Bulkhead
  * RateLimiter
  * Retry
  * TimeLimiter

## Backend alkalmazás előkészítése

```java
@Component
@Slf4j
public class ChaosFilter extends OncePerRequestFilter {

    private int delay = 0;

    private int faultPercent = 0;

    private final Random random = new Random();

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var newFaultPercent = request.getParameter("faultPercent");
        if (newFaultPercent != null) {
            faultPercent = Integer.parseInt(newFaultPercent);
        }

        var newDelay = request.getParameter("delay");
        if (newDelay != null) {
            delay = Integer.parseInt(newDelay);
        }

        var randomThreshold = random.nextInt(100) + 1;
        if (faultPercent < randomThreshold) {
            log.info("We got lucky, no error occurred, {} < {}",
                    faultPercent, randomThreshold);
        } else {
            log.info("Bad luck, an error occurred, {} >= {}",
                    faultPercent, randomThreshold);
            throw new RuntimeException("Something went wrong...");
        }

        if (delay > 0) {
            log.info("Sleeping, {}", delay);
            Thread.sleep(delay);
        }

        filterChain.doFilter(request, response);
    }
}
```

## Resilience4j bevezetése, circuit breaker

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```java
@CircuitBreaker(name = "clientCircuitBreaker")
```


```yaml
resilience4j:
  circuitbreaker:
    instances:
      clientCircuitBreaker:
        slidingWindowSize: 3
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: count_based
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
```

* Actuator

## Failover

## TimeLimiter

## Retry

```java
@Retry(name = "clientRetry")
```


```yaml
resilience4j:
  retry:
    instances:
      clientRetry:
        max-attempts: 3
```

## Bulkhead

```java
@Bulkhead(name="clientBulkhead")
```


```yaml
resilience4j:
  bulkhead:
    instances:
      clientBulkhead:
        max-concurrent-calls: 5
```

* JMeter

## RateLimiter

```java
@RateLimiter(name="clientRateLimiter")
```


```yaml
resilience4j:
  ratelimiter:
    instances:
      clientRateLimiter:
        limit-for-period: 5
        limit-refresh-period: 1s
        allow-health-indicator-to-fail: true
        timeout-duration: 0s
```

* JMeter
