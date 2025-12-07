# 資料模型：Saga 訂單交易協調

**功能分支**: `001-saga-order-transaction`
**建立日期**: 2025-12-07

## 聚合根（Aggregates）

### 1. Order（訂單聚合）- Sales Context

**識別碼**: `orderId` (UUID)

| 欄位 | 型別 | 說明 | 驗證規則 |
|------|------|------|----------|
| orderId | UUID | 訂單唯一識別碼 | 必填，不可變 |
| customerId | String | 顧客識別碼 | 必填 |
| productId | String | 商品識別碼 | 必填，MVP 固定為 "IPHONE17" |
| quantity | Integer | 數量 | 必填，MVP 固定為 1 |
| amount | BigDecimal | 訂單金額 | 必填，MVP 固定為 35000，必須 > 0 |
| status | OrderStatus | 訂單狀態 | 必填 |
| createdAt | Instant | 建立時間 | 系統自動產生 |
| updatedAt | Instant | 更新時間 | 系統自動維護 |

**狀態機（OrderStatus）**:
```
PENDING → PROCESSING → CONFIRMED
                    ↘ CANCELLED
                    ↘ CANCELLED_TIMEOUT
```

**不變量（Invariants）**:
- 金額必須為正數
- 狀態轉換必須合法（不可從 CONFIRMED 轉為 CANCELLED）
- 一旦 CONFIRMED 或 CANCELLED 即為終態

---

### 2. PaymentReservation（支付預留聚合）- Payment Context

**識別碼**: `reservationId` (UUID)

| 欄位 | 型別 | 說明 | 驗證規則 |
|------|------|------|----------|
| reservationId | UUID | 預留唯一識別碼 | 必填，不可變 |
| orderId | UUID | 關聯訂單編號 | 必填 |
| customerId | String | 顧客識別碼 | 必填 |
| amount | BigDecimal | 預留金額 | 必填，必須 > 0 |
| status | ReservationStatus | 預留狀態 | 必填 |
| expiresAt | Instant | 過期時間 | 系統自動設定（建立後 15 秒） |
| createdAt | Instant | 建立時間 | 系統自動產生 |

**狀態機（ReservationStatus）**:
```
RESERVED → CONFIRMED
        ↘ RELEASED
        ↘ EXPIRED
```

**不變量（Invariants）**:
- 預留金額不得超過顧客可用信用額度
- 一旦 CONFIRMED 或 RELEASED 即為終態
- EXPIRED 由超時機制自動觸發

---

### 3. InventoryReservation（庫存預留聚合）- Inventory Context

**識別碼**: `reservationId` (UUID)

| 欄位 | 型別 | 說明 | 驗證規則 |
|------|------|------|----------|
| reservationId | UUID | 預留唯一識別碼 | 必填，不可變 |
| orderId | UUID | 關聯訂單編號 | 必填 |
| productId | String | 商品識別碼 | 必填 |
| quantity | Integer | 預留數量 | 必填，必須 > 0 |
| status | ReservationStatus | 預留狀態 | 必填 |
| createdAt | Instant | 建立時間 | 系統自動產生 |

**狀態機（ReservationStatus）**:
```
RESERVED → CONFIRMED
        ↘ RELEASED
```

**不變量（Invariants）**:
- 預留數量不得超過可用庫存
- 一旦 CONFIRMED 或 RELEASED 即為終態

---

### 4. Product（商品實體）- Inventory Context

**識別碼**: `productId` (String)

| 欄位 | 型別 | 說明 | 驗證規則 |
|------|------|------|----------|
| productId | String | 商品唯一識別碼 | 必填，不可變 |
| name | String | 商品名稱 | 必填 |
| price | BigDecimal | 單價 | 必填，必須 > 0 |
| availableStock | Integer | 可用庫存 | 必須 >= 0 |
| reservedStock | Integer | 已預留庫存 | 必須 >= 0 |

**不變量（Invariants）**:
- availableStock + reservedStock = 總庫存
- 預留操作必須：availableStock >= requestedQuantity

---

### 5. CustomerCredit（顧客信用實體）- Payment Context

**識別碼**: `customerId` (String)

| 欄位 | 型別 | 說明 | 驗證規則 |
|------|------|------|----------|
| customerId | String | 顧客唯一識別碼 | 必填，不可變 |
| creditLimit | BigDecimal | 信用額度上限 | 必填，必須 > 0 |
| availableCredit | BigDecimal | 可用額度 | 必須 >= 0 |
| reservedCredit | BigDecimal | 已預留額度 | 必須 >= 0 |

**不變量（Invariants）**:
- availableCredit + reservedCredit <= creditLimit
- 預留操作必須：availableCredit >= requestedAmount

---

## 值物件（Value Objects）

### Money

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        currency = Objects.requireNonNullElse(currency, "TWD");
    }
}
```

### OrderItem

```java
public record OrderItem(String productId, int quantity, Money unitPrice) {
    public Money totalPrice() {
        return new Money(
            unitPrice.amount().multiply(BigDecimal.valueOf(quantity)),
            unitPrice.currency()
        );
    }
}
```

---

## 領域事件（Domain Events）

### Sales Context 發出

| 事件 | 觸發時機 | 關鍵欄位 |
|------|----------|----------|
| OrderCreatedEvent | 訂單建立 | orderId, customerId, productId, amount |
| OrderConfirmedEvent | 訂單確認成功 | orderId |
| OrderCancelledEvent | 訂單取消 | orderId, reason |

### Payment Context 發出

| 事件 | 觸發時機 | 關鍵欄位 |
|------|----------|----------|
| PaymentReservedEvent | 支付預留成功 | orderId, reservationId, amount |
| PaymentReservationFailedEvent | 支付預留失敗 | orderId, reason |
| PaymentConfirmedEvent | 支付確認成功 | orderId, reservationId |
| PaymentReleasedEvent | 支付預留釋放 | orderId, reservationId |

### Inventory Context 發出

| 事件 | 觸發時機 | 關鍵欄位 |
|------|----------|----------|
| InventoryReservedEvent | 庫存預留成功 | orderId, reservationId, productId, quantity |
| InventoryReservationFailedEvent | 庫存預留失敗 | orderId, productId, reason |
| InventoryConfirmedEvent | 庫存扣減確認 | orderId, reservationId |
| InventoryReleasedEvent | 庫存預留釋放 | orderId, reservationId |

---

## 命令（Commands）

### Sales Context 處理

| 命令 | 處理者 | 說明 |
|------|--------|------|
| CreateOrderCommand | Order Aggregate | 建立新訂單 |
| ConfirmOrderCommand | Order Aggregate | 確認訂單（內部） |
| CancelOrderCommand | Order Aggregate | 取消訂單 |

### Payment Context 處理

| 命令 | 處理者 | 說明 |
|------|--------|------|
| ReservePaymentCommand | PaymentReservation Aggregate | 預留信用額度 |
| ConfirmPaymentCommand | PaymentReservation Aggregate | 確認扣款 |
| ReleasePaymentCommand | PaymentReservation Aggregate | 釋放預留額度 |

### Inventory Context 處理

| 命令 | 處理者 | 說明 |
|------|--------|------|
| ReserveInventoryCommand | InventoryReservation Aggregate | 預留庫存 |
| ConfirmInventoryCommand | InventoryReservation Aggregate | 確認扣減庫存 |
| ReleaseInventoryCommand | InventoryReservation Aggregate | 釋放預留庫存 |

---

## Saga 狀態追蹤

### OrderSaga 狀態

| 欄位 | 型別 | 說明 |
|------|------|------|
| orderId | UUID | 關聯訂單 |
| paymentReservationId | UUID | 支付預留 ID（可為 null） |
| inventoryReservationId | UUID | 庫存預留 ID（可為 null） |
| paymentStatus | StepStatus | 支付步驟狀態 |
| inventoryStatus | StepStatus | 庫存步驟狀態 |
| deadlineId | String | 超時排程 ID |

**StepStatus 列舉**:
- `PENDING`: 等待中
- `SUCCESS`: 成功
- `FAILED`: 失敗
- `COMPENSATED`: 已補償

---

## 實體關係圖

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sales Context                             │
│  ┌─────────────┐                                                │
│  │   Order     │──────────────────────────────────────────────┐ │
│  │  Aggregate  │                                              │ │
│  └─────────────┘                                              │ │
│         │                                                      │ │
│         │ publishes OrderCreatedEvent                          │ │
│         ▼                                                      │ │
│  ┌─────────────┐                                              │ │
│  │ OrderSaga   │ (coordinates)                                │ │
│  └─────────────┘                                              │ │
└─────────────────────────────────────────────────────────────────┘
          │                              │
          │ ReservePaymentCommand        │ ReserveInventoryCommand
          ▼                              ▼
┌──────────────────────┐    ┌──────────────────────┐
│   Payment Context    │    │  Inventory Context   │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │PaymentReserva- │  │    │  │InventoryReser- │  │
│  │tion Aggregate  │  │    │  │vation Aggregate│  │
│  └────────────────┘  │    │  └────────────────┘  │
│         │            │    │         │            │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │CustomerCredit  │  │    │  │    Product     │  │
│  └────────────────┘  │    │  └────────────────┘  │
└──────────────────────┘    └──────────────────────┘
```
