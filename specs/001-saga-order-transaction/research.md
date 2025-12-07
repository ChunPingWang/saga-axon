# 研究文件：Saga 訂單交易協調

**功能分支**: `001-saga-order-transaction`
**建立日期**: 2025-12-07

## 研究主題

### 1. Axon Framework Saga 實作模式

**決策**: 採用 Axon Framework 的 Saga 機制實作 Choreography-based Saga 模式

**理由**:
- Axon Framework 原生支援 Saga 生命週期管理（`@Saga`, `@StartSaga`, `@EndSaga`）
- 內建超時機制（`@DeadlineManager`）可直接實作 15 秒超時需求
- 與 Spring Boot 3 無縫整合
- 事件溯源（Event Sourcing）確保交易可追蹤性與稽核需求

**考慮的替代方案**:
- **Orchestration-based Saga（使用中央協調器）**: 較容易理解但增加單點故障風險，不符合微服務解耦原則
- **手動實作補償邏輯**: 複雜度高，容易出錯，需要大量樣板程式碼
- **使用其他框架（Eventuate Tram）**: 社群支援較少，與 Spring Boot 3 整合不如 Axon 成熟

### 2. 事件驅動通訊架構

**決策**: 使用 Axon Server 作為事件儲存與訊息代理

**理由**:
- Axon Server 提供開箱即用的事件儲存、命令路由、查詢分發
- 支援事件重播，便於除錯與系統恢復
- 內建事件追蹤與指標監控
- 免費版本足夠 MVP 需求

**考慮的替代方案**:
- **Apache Kafka**: 需要額外設定與維護，學習曲線較高
- **RabbitMQ**: 不原生支援事件溯源
- **嵌入式 H2 + Axon 無伺服器模式**: 開發測試可用，但生產環境不建議

### 3. 超時處理機制

**決策**: 使用 Axon 的 `DeadlineManager` 實作 15 秒超時

**理由**:
- `DeadlineManager` 與 Saga 生命週期整合，超時自動觸發處理器
- 支援排程與取消，適合處理競態條件
- 精確度可達毫秒級，滿足 ±1 秒需求

**實作方式**:
```java
// 在 Saga 中設定 deadline
deadlineManager.schedule(
    Duration.ofSeconds(15),
    "payment-timeout",
    new PaymentTimeoutPayload(orderId)
);

// 收到成功回應後取消 deadline
deadlineManager.cancelAllWithinScope("payment-timeout");
```

**考慮的替代方案**:
- **Spring `@Scheduled`**: 無法與 Saga 狀態整合，難以管理個別交易超時
- **外部排程器（Quartz）**: 過度複雜，增加維運負擔

### 4. 補償重試策略

**決策**: 使用 Spring Retry 實作固定 3 次重試

**理由**:
- Spring Retry 與 Spring Boot 3 原生整合
- 支援指數退避，但 MVP 使用固定間隔即可
- 可透過 `@Retryable` 註解宣告式設定

**實作方式**:
```java
@Retryable(
    value = {CompensationFailedException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public void sendCompensation(String orderId) {
    // 發送補償命令
}

@Recover
public void handleCompensationFailure(CompensationFailedException e, String orderId) {
    // 記錄失敗並標記待人工處理
    sagaRepository.markAsManualIntervention(orderId);
}
```

### 5. 資料一致性保證

**決策**: 採用最終一致性模型，搭配冪等性設計

**理由**:
- 分散式系統無法同時滿足強一致性與高可用性（CAP 定理）
- 電商場景可接受短暫不一致（秒級），最終一致性足夠
- 冪等性確保重試安全

**實作要點**:
- 所有命令處理器必須實作冪等性檢查
- 使用業務 ID（orderId）而非技術 ID 作為識別
- 事件儲存提供天然的稽核軌跡

### 6. 測試策略

**決策**: 採用 Axon Test Fixtures 進行 Saga 測試

**理由**:
- `SagaTestFixture` 專門設計用於測試 Saga 行為
- 支援 Given-When-Then 風格，符合 BDD 原則
- 可模擬事件序列與驗證預期命令

**測試範例**:
```java
fixture.givenAggregate(orderId)
    .published(new OrderCreatedEvent(orderId, productId, amount))
    .whenPublishingA(new PaymentReservedEvent(orderId, paymentId))
    .expectDispatchedCommands(new ReserveInventoryCommand(orderId, productId, 1));
```

## 技術決策摘要

| 主題 | 決策 | 信心度 |
|------|------|--------|
| Saga 框架 | Axon Framework Saga | 高 |
| 訊息代理 | Axon Server | 高 |
| 超時機制 | DeadlineManager | 高 |
| 重試策略 | Spring Retry (3 次) | 高 |
| 一致性模型 | 最終一致性 + 冪等性 | 高 |
| 測試框架 | Axon Test Fixtures + JUnit 5 | 高 |

## 待進一步研究（Phase 1）

- Mock WebSocket 通知機制的具體實作方式
- H2 與 Axon Server 在整合測試中的設定
- Gradle 多模組專案設定最佳實踐
