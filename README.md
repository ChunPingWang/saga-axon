# Saga 訂單交易系統

基於 **Axon Framework** 實作的分散式交易協調系統，採用 **Saga 模式** 處理跨服務的訂單、付款、庫存交易。

## 專案概述

本專案展示如何使用 Axon Framework 實作 Choreography-based Saga 模式，協調三個微服務之間的分散式交易：

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sales Service                             │
│  ┌─────────┐    ┌───────────┐    ┌─────────────────────────┐   │
│  │  Order  │───▶│ OrderSaga │───▶│ CommandGateway (Axon)   │   │
│  │Aggregate│    │           │    └─────────────────────────┘   │
│  └─────────┘    └───────────┘              │                    │
└────────────────────────────────────────────┼────────────────────┘
                                             │
                    ┌────────────────────────┼────────────────────┐
                    ▼                        ▼                    │
┌─────────────────────────────┐  ┌─────────────────────────────┐ │
│     Payment Service         │  │     Inventory Service       │ │
│  ┌───────────────────────┐  │  │  ┌───────────────────────┐  │ │
│  │  PaymentReservation   │  │  │  │ InventoryReservation  │  │ │
│  │      Aggregate        │  │  │  │      Aggregate        │  │ │
│  └───────────────────────┘  │  │  └───────────────────────┘  │ │
└─────────────────────────────┘  └─────────────────────────────┘ │
                                                                  │
                         Axon Server (Event Store & Message Broker)
```

## 技術架構

### 核心技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 程式語言 |
| Spring Boot | 3.x | 應用框架 |
| Axon Framework | 4.9.3 | CQRS/ES 與 Saga |
| Axon Server | latest | 事件儲存與訊息路由 |
| H2 Database | - | 開發用資料庫 |
| Gradle | 8.5 | 建置工具 |

### 模組結構

```
saga-axon/
├── shared-kernel/          # 共用核心（命令、事件、值物件）
├── sales-service/          # 銷售服務（訂單、Saga 協調器）
├── payment-service/        # 付款服務（信用額度管理）
├── inventory-service/      # 庫存服務（商品庫存管理）
├── docker-compose.yml      # Axon Server 容器設定
└── specs/                  # 規格文件
```

## 快速開始

### 1. 啟動 Axon Server

```bash
docker-compose up -d
```

Axon Server Dashboard: http://localhost:8024

### 2. 編譯專案

```bash
./gradlew build
```

### 3. 啟動服務

分別在三個終端機執行：

```bash
# 終端機 1 - Sales Service
./gradlew :sales-service:bootRun

# 終端機 2 - Payment Service
./gradlew :payment-service:bootRun

# 終端機 3 - Inventory Service
./gradlew :inventory-service:bootRun
```

### 4. 測試 API

```bash
# 建立訂單
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "IPHONE17",
    "quantity": 1
  }'
```

## 用戶故事與實作

### US1: 成功訂單流程 ✅

當客戶下單時，系統會：
1. 建立訂單（PENDING 狀態）
2. 平行發送付款預留與庫存預留命令
3. 兩者都成功後確認訂單（CONFIRMED 狀態）

```
OrderCreated ──┬──▶ ReservePayment ──▶ PaymentReserved ──┐
               │                                          │
               └──▶ ReserveInventory ──▶ InventoryReserved┴──▶ ConfirmOrder
```

### US2: 失敗補償機制 ✅

當任一預留失敗時：
1. 釋放已成功的預留
2. 取消訂單（CANCELLED 狀態）

```
PaymentReserved + InventoryFailed ──▶ ReleasePayment ──▶ CancelOrder
```

### US3: 逾時取消 ✅

15 秒內未完成交易自動取消：

```
OrderCreated ──▶ [15秒逾時] ──▶ 釋放所有預留 ──▶ CancelOrder (CANCELLED_TIMEOUT)
```

### US4: 補償重試機制 ✅

補償命令失敗時自動重試（最多 3 次，間隔 500ms）

---

## Axon Framework 教學

### 什麼是 Axon Framework？

Axon Framework 是一個用於建構事件驅動微服務的 Java 框架，提供：

- **CQRS**（Command Query Responsibility Segregation）：命令與查詢分離
- **Event Sourcing**：以事件作為狀態的唯一真相來源
- **Saga**：跨服務的分散式交易協調

### 核心概念

#### 1. 聚合根（Aggregate）

聚合根是 DDD 中的核心概念，負責處理命令並發布事件：

```java
@Aggregate
public class Order {

    @AggregateIdentifier
    private UUID orderId;
    private OrderStatus status;

    // 命令處理器 - 建構子用於建立聚合
    @CommandHandler
    public Order(CreateOrderCommand command) {
        // 驗證業務規則
        // 發布事件（不直接修改狀態）
        AggregateLifecycle.apply(new OrderCreatedEvent(
            command.orderId(),
            command.customerId(),
            command.productId(),
            command.quantity(),
            command.amount()
        ));
    }

    // 事件溯源處理器 - 根據事件更新狀態
    @EventSourcingHandler
    public void on(OrderCreatedEvent event) {
        this.orderId = event.orderId();
        this.status = OrderStatus.PENDING;
    }

    // 命令處理器 - 實例方法用於修改聚合
    @CommandHandler
    public void handle(ConfirmOrderCommand command) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("無法確認非待處理狀態的訂單");
        }
        AggregateLifecycle.apply(new OrderConfirmedEvent(orderId));
    }

    @EventSourcingHandler
    public void on(OrderConfirmedEvent event) {
        this.status = OrderStatus.CONFIRMED;
    }
}
```

#### 2. 命令（Command）

命令代表改變系統狀態的意圖：

```java
public record CreateOrderCommand(
    @TargetAggregateIdentifier  // 指定目標聚合的 ID
    UUID orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal amount
) {
    public CreateOrderCommand {
        // 驗證命令參數
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID 不可為空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("數量必須大於 0");
        }
    }
}
```

#### 3. 事件（Event）

事件代表已發生的事實，是不可變的：

```java
public record OrderCreatedEvent(
    UUID orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal amount,
    Instant timestamp
) {
    public OrderCreatedEvent(UUID orderId, String customerId,
                             String productId, int quantity, BigDecimal amount) {
        this(orderId, customerId, productId, quantity, amount, Instant.now());
    }
}
```

#### 4. Saga 協調器

Saga 負責協調跨聚合/服務的長時間交易：

```java
@Saga
public class OrderSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    private UUID orderId;
    private UUID paymentReservationId;
    private UUID inventoryReservationId;
    private StepStatus paymentStatus = StepStatus.PENDING;
    private StepStatus inventoryStatus = StepStatus.PENDING;

    // 啟動 Saga
    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderCreatedEvent event) {
        this.orderId = event.orderId();

        // 設定 15 秒逾時
        deadlineManager.schedule(Duration.ofSeconds(15), "order-timeout");

        // 發送預留命令（平行執行）
        this.paymentReservationId = UUID.randomUUID();
        this.inventoryReservationId = UUID.randomUUID();

        commandGateway.send(new ReservePaymentCommand(
            paymentReservationId, orderId, event.customerId(), event.amount()
        ));

        commandGateway.send(new ReserveInventoryCommand(
            inventoryReservationId, orderId, event.productId(), event.quantity()
        ));
    }

    // 處理付款預留成功
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentReservedEvent event) {
        this.paymentStatus = StepStatus.SUCCESS;
        checkCompletion();
    }

    // 處理庫存預留成功
    @SagaEventHandler(associationProperty = "orderId")
    public void on(InventoryReservedEvent event) {
        this.inventoryStatus = StepStatus.SUCCESS;
        checkCompletion();
    }

    // 處理付款預留失敗 - 觸發補償
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentReservationFailedEvent event) {
        this.paymentStatus = StepStatus.FAILED;
        compensate("付款失敗: " + event.reason());
    }

    // 檢查是否兩個預留都成功
    private void checkCompletion() {
        if (paymentStatus == StepStatus.SUCCESS &&
            inventoryStatus == StepStatus.SUCCESS) {

            // 取消逾時
            deadlineManager.cancelAllWithinScope("order-timeout");

            // 確認預留並完成訂單
            commandGateway.send(new ConfirmPaymentCommand(paymentReservationId, orderId));
            commandGateway.send(new ConfirmInventoryCommand(inventoryReservationId, orderId));
            commandGateway.send(new ConfirmOrderCommand(orderId));
        }
    }

    // 補償邏輯
    private void compensate(String reason) {
        deadlineManager.cancelAllWithinScope("order-timeout");

        // 釋放已成功的預留
        if (paymentStatus == StepStatus.SUCCESS) {
            commandGateway.send(new ReleasePaymentCommand(
                paymentReservationId, orderId, reason
            ));
        }
        if (inventoryStatus == StepStatus.SUCCESS) {
            commandGateway.send(new ReleaseInventoryCommand(
                inventoryReservationId, orderId, reason
            ));
        }
    }

    // 逾時處理
    @DeadlineHandler(deadlineName = "order-timeout")
    public void onTimeout() {
        compensate("交易逾時");
        commandGateway.send(CancelOrderCommand.forTimeout(orderId));
    }

    // 結束 Saga
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderConfirmedEvent event) {
        // Saga 成功結束
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderCancelledEvent event) {
        // Saga 因取消結束
    }
}
```

### 測試 Axon 元件

#### 聚合根測試

使用 `AggregateTestFixture`：

```java
@DisplayName("Order Aggregate")
class OrderTest {

    private FixtureConfiguration<Order> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Order.class);
    }

    @Test
    @DisplayName("應該建立訂單並發布 OrderCreatedEvent")
    void shouldCreateOrder() {
        UUID orderId = UUID.randomUUID();

        fixture.givenNoPriorActivity()
            .when(new CreateOrderCommand(orderId, "CUST-001", "PROD-001", 1, new BigDecimal("1000")))
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(OrderCreatedEvent.class))
            ))
            .expectState(order -> {
                assertEquals(orderId, order.getOrderId());
                assertEquals(OrderStatus.PENDING, order.getStatus());
            });
    }
}
```

#### Saga 測試

使用 `SagaTestFixture`：

```java
@DisplayName("OrderSaga")
class OrderSagaTest {

    private SagaTestFixture<OrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderSaga.class);
    }

    @Test
    @DisplayName("建立訂單時應該發送預留命令")
    void shouldSendReservationCommands() {
        UUID orderId = UUID.randomUUID();

        fixture.givenNoPriorActivity()
            .whenPublishingA(new OrderCreatedEvent(orderId, "CUST-001", "PROD-001", 1, new BigDecimal("1000")))
            .expectActiveSagas(1)
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ReservePaymentCommand.class)),
                messageWithPayload(instanceOf(ReserveInventoryCommand.class))
            ));
    }

    @Test
    @DisplayName("逾時時應該取消訂單")
    void shouldCancelOrderOnTimeout() {
        UUID orderId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(orderId, "CUST-001", "PROD-001", 1, new BigDecimal("1000")))
            .whenTimeElapses(Duration.ofSeconds(15))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }
}
```

### Axon 設定

```yaml
# application.yml
axon:
  axonserver:
    servers: localhost:8124    # Axon Server 位址
  serializer:
    general: jackson          # 使用 Jackson 序列化
    events: jackson
    messages: jackson

spring:
  application:
    name: sales-service
```

### 依賴注入聚合根中的外部服務

```java
@Aggregate
public class PaymentReservation {

    @CommandHandler
    public PaymentReservation(ReservePaymentCommand command,
                               CustomerCreditRepository creditRepository) {
        // Axon 會自動注入 Spring Bean
        CustomerCredit credit = creditRepository.findByCustomerId(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        if (!credit.hasAvailableCredit(command.amount())) {
            AggregateLifecycle.apply(new PaymentReservationFailedEvent(
                command.orderId(),
                "INSUFFICIENT_CREDIT",
                "信用額度不足"
            ));
            return;
        }

        credit.reserveCredit(command.amount());
        creditRepository.save(credit);

        AggregateLifecycle.apply(new PaymentReservedEvent(
            command.orderId(),
            command.reservationId(),
            command.customerId(),
            command.amount()
        ));
    }
}
```

---

## 測試覆蓋

本專案包含 **42 個測試方法**，涵蓋：

| 測試類別 | 數量 | 說明 |
|---------|------|------|
| OrderTest | 12 | 訂單聚合根測試 |
| PaymentReservationTest | 5 | 付款預留聚合根測試 |
| InventoryReservationTest | 5 | 庫存預留聚合根測試 |
| OrderSagaSuccessTest | 4 | Saga 成功路徑測試 |
| OrderSagaCompensationTest | 9 | Saga 補償路徑測試 |
| OrderSagaTimeoutTest | 6 | Saga 逾時測試 |
| RetryableCommandGatewayTest | 2 | 重試機制測試 |

執行測試：

```bash
./gradlew test
```

產生覆蓋率報告：

```bash
./gradlew jacocoTestReport
```

---

## 參考資源

- [Axon Framework 官方文件](https://docs.axoniq.io/reference-guide/)
- [Axon Framework GitHub](https://github.com/AxonFramework/AxonFramework)
- [Saga 模式介紹](https://microservices.io/patterns/data/saga.html)
- [CQRS 模式](https://martinfowler.com/bliki/CQRS.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

---

## 授權

MIT License
