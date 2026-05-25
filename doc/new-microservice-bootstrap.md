# Bootstrap a New Microservice from order-service

Use this guide to spin up another Spring Boot microservice that reuses the same foundation as **order-service**: AWS clients, typed configuration, and a shared exception/HTTP error model.

> **Naming note:** This repo uses `TechnicalException` today. The templates below use `TechException` as requested; pick one name and use it consistently across the service.

---

## What to copy vs what to replace

| Keep (generic) | Replace per service |
|----------------|---------------------|
| `config/AwsConfig.java` | `pom.xml` `artifactId`, `name`, `description` |
| `config/ServiceConfig.java` (structure; trim/add AWS props) | Root package (`com.fredfmelo.orderservice` → `com.fredfmelo.<service>`) |
| `common/exception/*` | `application.yaml` (`spring.application.name`, ARNs, table names) |
| `pom.xml` parent, AWS BOM, Lombok, web/validation deps | `openapi.yaml`, feature packages, listeners, publishers |
| OpenAPI generator plugin pattern (optional) | `*Application.java` class name |

Do **not** copy domain code (`order/`, `infrastructure/messaging/OrderEventPublisher`, etc.) unless the new service needs the same bounded context.

---

## 1. Create the project

### Option A — Copy the repo (fastest)

```bash
cp -R order-service ../inventory-service   # example name
cd ../inventory-service
rm -rf .git target
git init
```

### Option B — Spring Initializr + paste templates

- Java **21**, Spring Boot **3.5.x**
- Dependencies: **Web**, **Validation**, **Lombok**
- Add AWS SDK and OpenAPI plugin manually (see [Maven dependencies](#5-maven-dependencies)).

---

## 2. Rename identifiers

Replace placeholders everywhere:

| Placeholder | Example (inventory-service) |
|-------------|-----------------------------|
| `{basePackage}` | `com.fredfmelo.inventoryservice` |
| `{serviceName}` | `inventory-service` |
| `{ServiceName}` | `InventoryService` |
| `{tableName}` | `INVENTORY` |

**Files to update:**

1. Move/rename package folders under `src/main/java/`.
2. Rename `OrderServiceApplication.java` → `{ServiceName}Application.java`.
3. Update `pom.xml`: `artifactId`, `<name>`, `<description>`.
4. Update `application.yaml`: `spring.application.name`.
5. Update OpenAPI `apiPackage` / `modelPackage` in `pom.xml` if you use the generator.

**Compile check:**

```bash
mvn -q compile
```

---

## 3. Target package layout (shared + features)

Keep cross-cutting code outside feature slices:

```text
src/main/java/{basePackage}/
├── {ServiceName}Application.java
├── config/
│   ├── AwsConfig.java
│   └── ServiceConfig.java
├── common/
│   └── exception/
│       ├── BusinessException.java
│       ├── TechException.java
│       ├── ErrorResponse.java
│       └── GlobalExceptionHandler.java
└── <feature>/          # e.g. inventory/, payment/
    ├── controller/
    ├── service/
    ├── domain/
    └── ...
```

Feature-specific AWS wiring (SNS publishers, SQS listeners) stays under `<feature>/` or `infrastructure/`, not in `config/`, unless every service shares the same bean.

---

## 4. Generic classes (templates)

Replace `{basePackage}` with your root package (e.g. `com.fredfmelo.inventoryservice`).

### 4.1 `config/AwsConfig.java`

Register only the AWS clients the service needs. Remove unused `@Bean` methods (e.g. drop SNS if the service only reads SQS).

```java
package {basePackage}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    private static final Region REGION = Region.US_EAST_1;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(REGION)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(REGION)
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(REGION)
                .build();
    }
}
```

For **LocalStack** or explicit credentials, extend this class with endpoint overrides; order-service uses default credential chain + `us-east-1`.

---

### 4.2 `config/ServiceConfig.java`

Binds `aws.*` from `application.yaml`. Add nested classes only for resources this service uses.

```java
package {basePackage}.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws")
public class ServiceConfig {

    private DynamoDb dynamodb;
    private Sns sns;
    private Sqs sqs;

    @Getter
    @Setter
    public static class DynamoDb {
        private String tableName;
    }

    @Getter
    @Setter
    public static class Sns {
        /** Example: arn:aws:sns:us-east-1:ACCOUNT:my-topic */
        private String topicArn;
    }

    @Getter
    @Setter
    public static class Sqs {
        private String queueName;
    }
}
```

Rename fields to match your domain (order-service uses `orderTopicArn`, `orderStateQueue`, etc.).

---

### 4.3 `common/exception/ErrorResponse.java`

Shared API error body (used by `GlobalExceptionHandler`).

```java
package {basePackage}.common.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String message
) {
}
```

---

### 4.4 `common/exception/BusinessException.java`

Expected domain/rule failures → typically **4xx** (default `422 UNPROCESSABLE_ENTITY`).

```java
package {basePackage}.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

public class BusinessException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public BusinessException(String message, @NonNull HttpStatus status) {
        super(message);
        this.status = status;
    }

    @NonNull
    public HttpStatus getStatus() {
        return status;
    }
}
```

**Usage in services:**

```java
throw new BusinessException("Customer ID is required");
throw new BusinessException("Order not found", HttpStatus.NOT_FOUND);
```

---

### 4.5 `common/exception/TechException.java`

Infrastructure / integration failures (DB, SNS, serialization) → typically **5xx** (default `500 INTERNAL_SERVER_ERROR`).

```java
package {basePackage}.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

public class TechException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public TechException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public TechException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public TechException(String message, @NonNull HttpStatus status) {
        super(message);
        this.status = status;
    }

    public TechException(String message, Throwable cause, @NonNull HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    @NonNull
    public HttpStatus getStatus() {
        return status;
    }
}
```

**Usage** (wrap low-level errors instead of raw `RuntimeException`):

```java
} catch (JsonProcessingException | SdkException ex) {
    throw new TechException("Failed to publish event", ex);
}
```

---

### 4.6 `common/exception/GlobalExceptionHandler.java`

Maps exceptions to `ErrorResponse` + HTTP status.

```java
package {basePackage}.common.exception;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                ex.getStatus().value(),
                ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(TechException.class)
    public ResponseEntity<ErrorResponse> handleTechnical(TechException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                ex.getStatus().value(),
                ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                500,
                "Internal server error");

        return ResponseEntity
                .internalServerError()
                .body(response);
    }
}
```

`@RestControllerAdvice` is picked up automatically if it lives under the same base package as `{ServiceName}Application`.

---

## 5. Maven dependencies

Minimum set aligned with order-service (adjust if you drop DynamoDB, SNS, or SQS):

```xml
<properties>
    <java.version>21</java.version>
    <aws.sdk.version>2.32.12</aws.sdk.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>${aws.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>sns</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>sqs</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <!-- Optional: SQS listener support -->
    <dependency>
        <groupId>io.awspring.cloud</groupId>
        <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        <version>3.3.1</version>
    </dependency>
</dependencies>
```

Copy the **OpenAPI Generator** plugin block from order-service `pom.xml` if the new service is API-first.

---

## 6. `application.yaml` template

```yaml
spring:
  application:
    name: {serviceName}

aws:
  dynamodb:
    table-name: {tableName}
  sns:
    topic-arn: arn:aws:sns:us-east-1:ACCOUNT:your-topic
  sqs:
    queue-name: your-queue-name
```

Spring maps kebab-case YAML to camelCase Java (`table-name` → `tableName`, `topic-arn` → `topicArn`).

---

## 7. Application entrypoint

```java
package {basePackage};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {ServiceName}Application {

    public static void main(String[] args) {
        SpringApplication.run({ServiceName}Application.class, args);
    }
}
```

---

## 8. Checklist before first feature

- [ ] Generic classes under `config/` and `common/exception/` compile
- [ ] `mvn compile` passes
- [ ] `application.yaml` has correct AWS resource names/ARNs
- [ ] Unused beans removed from `AwsConfig` (no orphan clients)
- [ ] `ServiceConfig` fields match YAML keys
- [ ] Controllers throw `BusinessException` / `TechException`, not generic `IllegalArgumentException`, unless you add more handlers
- [ ] OpenAPI packages updated in `pom.xml` (if applicable)
- [ ] `.vscode/launch.json` main class updated for local debug

---

## 9. Reference in order-service

| Template in this doc | Current file in order-service |
|----------------------|-------------------------------|
| `AwsConfig.java` | `src/main/java/com/fredfmelo/orderservice/config/AwsConfig.java` |
| `ServiceConfig.java` | `src/main/java/com/fredfmelo/orderservice/config/ServiceConfig.java` |
| `BusinessException.java` | `src/main/java/com/fredfmelo/orderservice/common/exception/BusinessException.java` |
| `TechException.java` | `TechnicalException.java` (same idea; consider renaming) |
| `GlobalExceptionHandler.java` | `src/main/java/com/fredfmelo/orderservice/common/exception/GlobalExceptionHandler.java` |
| `ErrorResponse.java` | `src/main/java/com/fredfmelo/orderservice/common/exception/ErrorResponse.java` |

For package-by-feature conventions when adding domain code, see [architecture_package_by_feature_documentation.md](./architecture_package_by_feature_documentation.md).
