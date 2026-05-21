# Architecture Decision Record: Package Structure

## Decision

This project uses **Package by Feature (Vertical Slice)** instead of a global layer-based structure.

Avoid:

```text
controller/
service/
repository/
dto/
entity/
```

Use:

```text
order/
payment/
inventory/
```

Each feature owns its internal structure.

---

## Why

Traditional layer-based organization becomes hard to maintain as the project grows:

```text
service/
 ├── OrderService
 ├── PaymentService
 ├── InventoryService
 ├── UserService
 └── ProductService
```

Problems:

- Large folders with unrelated files
- High navigation cost
- Features spread across many directories
- More coupling
- Harder ownership and maintenance

Package by Feature keeps related code physically close.

---

## Project Structure

```text
src/main/java/com/fredfmelo/orderservice

├── config/
│   ├── OpenApiConfig.java
│   ├── AwsConfig.java
│   └── JacksonConfig.java
│
├── common/
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ErrorResponse.java
│   │
│   ├── mapper/
│   └── util/
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── sns/
│   ├── sqs/
│   └── client/
│
├── api/
├── model/
│
├── order/
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── mapper/
│
├── payment/
├── inventory/
│
└── OrderServiceApplication.java
```

---

## Package Responsibilities

### api/
Generated OpenAPI interfaces.

Rules:
- Generated only
- Never manually edited

Example:

```text
TestApi.java
OrderApi.java
```

---

### model/
Generated OpenAPI request/response DTOs.

Rules:
- Generated only
- No business logic

---

### order/
Contains all order-related functionality.

Example:

```text
order/
├── controller/
├── service/
├── domain/
├── repository/
└── mapper/
```

---

### infrastructure/
External integrations.

Examples:

- AWS SNS
- AWS SQS
- Database
- External APIs

Business rules must not live here.

---

### common/
Reusable shared components.

Examples:

- exception handling
- utility classes
- common mappers

Avoid turning this into a dumping folder.

---

## Controller Rule

Controllers should stay thin.

Avoid:

```java
@GetMapping
public ResponseEntity<?> create() {
    // business logic here
}
```

Prefer:

```java
@RestController
@RequiredArgsConstructor
public class TestController implements TestApi {

    private final TestService service;

    @Override
    public ResponseEntity<Void> test() {
        service.execute();
        return ResponseEntity.ok().build();
    }
}
```

Business logic belongs in services/domain.

---

## Architecture References

This structure combines ideas from:

- Package by Feature
- Vertical Slice Architecture
- Domain-Driven Design (light usage)
- Clean Architecture principles

Goal:

Keep the codebase simple, scalable and easy to navigate as the service grows.