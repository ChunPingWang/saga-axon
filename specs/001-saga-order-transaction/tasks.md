# 任務清單：Saga 訂單交易協調

**輸入**: 設計文件來自 `/specs/001-saga-order-transaction/`
**前置條件**: plan.md（必要）, spec.md（必要）, research.md, data-model.md, contracts/

**憲法合規**:
- II. 測試標準：80% 覆蓋率；測試先於實作
- III. BDD：驗收測試必須對應 spec.md 中的 Given-When-Then 情境
- IV. DDD：領域層必須無框架相依
- VI. 基礎設施隔離：框架程式碼僅限於基礎設施層
- IX. 文件語言：使用者文件必須以繁體中文撰寫

**測試**: 依據憲法，業務邏輯必須達 80% 測試覆蓋率。

**組織方式**: 任務依使用者故事分組，以支援獨立實作與測試。

## 格式: `[ID] [P?] [Story?] 描述與檔案路徑`

- **[P]**: 可平行執行（不同檔案、無相依）
- **[Story]**: 所屬使用者故事（US1, US2, US3）
- 包含精確的檔案路徑

## 路徑慣例

本專案為 Monorepo 微服務架構：
- `shared-kernel/src/main/java/com/example/shared/` - 共用核心
- `sales-service/src/main/java/com/example/sales/` - 銷售服務
- `payment-service/src/main/java/com/example/payment/` - 支付服務
- `inventory-service/src/main/java/com/example/inventory/` - 庫存服務

---

## Phase 1: Setup（專案初始化）

**目的**: 建立 Gradle 多模組專案結構與基礎設定

- [x] T001 建立 Gradle 多模組專案根目錄 `settings.gradle.kts`，包含 shared-kernel, sales-service, payment-service, inventory-service
- [x] T002 [P] 設定 shared-kernel 模組 `shared-kernel/build.gradle.kts`，定義 Java 17 + Spring Boot 3 + Axon 相依
- [x] T003 [P] 設定 sales-service 模組 `sales-service/build.gradle.kts`，相依 shared-kernel
- [x] T004 [P] 設定 payment-service 模組 `payment-service/build.gradle.kts`，相依 shared-kernel
- [x] T005 [P] 設定 inventory-service 模組 `inventory-service/build.gradle.kts`，相依 shared-kernel
- [x] T006 [P] 建立 sales-service 主程式 `sales-service/src/main/java/com/example/sales/SalesServiceApplication.java`
- [x] T007 [P] 建立 payment-service 主程式 `payment-service/src/main/java/com/example/payment/PaymentServiceApplication.java`
- [x] T008 [P] 建立 inventory-service 主程式 `inventory-service/src/main/java/com/example/inventory/InventoryServiceApplication.java`
- [x] T009 [P] 設定 sales-service 應用程式組態 `sales-service/src/main/resources/application.yml`（含 Axon Server 連線）
- [x] T010 [P] 設定 payment-service 應用程式組態 `payment-service/src/main/resources/application.yml`
- [x] T011 [P] 設定 inventory-service 應用程式組態 `inventory-service/src/main/resources/application.yml`
- [x] T012 建立 Docker Compose 檔案 `docker-compose.yml`，包含 Axon Server

---

## Phase 2: Foundational（基礎建設 - 阻塞所有使用者故事）

**目的**: 建立跨服務共用的領域事件、命令、值物件，以及各服務的基礎聚合根

**⚠️ 關鍵**: 此階段必須完成後才能開始任何使用者故事

### Shared Kernel（共用核心）

- [x] T013 [P] 建立值物件 Money `shared-kernel/src/main/java/com/example/shared/valueobject/Money.java`
- [x] T014 [P] 建立值物件 OrderItem `shared-kernel/src/main/java/com/example/shared/valueobject/OrderItem.java`
- [x] T015 [P] 建立列舉 OrderStatus `shared-kernel/src/main/java/com/example/shared/valueobject/OrderStatus.java`
- [x] T016 [P] 建立列舉 ReservationStatus `shared-kernel/src/main/java/com/example/shared/valueobject/ReservationStatus.java`
- [x] T017 [P] 建立命令 CreateOrderCommand `shared-kernel/src/main/java/com/example/shared/command/CreateOrderCommand.java`
- [x] T018 [P] 建立命令 ConfirmOrderCommand `shared-kernel/src/main/java/com/example/shared/command/ConfirmOrderCommand.java`
- [x] T019 [P] 建立命令 CancelOrderCommand `shared-kernel/src/main/java/com/example/shared/command/CancelOrderCommand.java`
- [x] T020 [P] 建立命令 ReservePaymentCommand `shared-kernel/src/main/java/com/example/shared/command/ReservePaymentCommand.java`
- [x] T021 [P] 建立命令 ConfirmPaymentCommand `shared-kernel/src/main/java/com/example/shared/command/ConfirmPaymentCommand.java`
- [x] T022 [P] 建立命令 ReleasePaymentCommand `shared-kernel/src/main/java/com/example/shared/command/ReleasePaymentCommand.java`
- [x] T023 [P] 建立命令 ReserveInventoryCommand `shared-kernel/src/main/java/com/example/shared/command/ReserveInventoryCommand.java`
- [x] T024 [P] 建立命令 ConfirmInventoryCommand `shared-kernel/src/main/java/com/example/shared/command/ConfirmInventoryCommand.java`
- [x] T025 [P] 建立命令 ReleaseInventoryCommand `shared-kernel/src/main/java/com/example/shared/command/ReleaseInventoryCommand.java`
- [x] T026 [P] 建立事件 OrderCreatedEvent `shared-kernel/src/main/java/com/example/shared/event/OrderCreatedEvent.java`
- [x] T027 [P] 建立事件 OrderConfirmedEvent `shared-kernel/src/main/java/com/example/shared/event/OrderConfirmedEvent.java`
- [x] T028 [P] 建立事件 OrderCancelledEvent `shared-kernel/src/main/java/com/example/shared/event/OrderCancelledEvent.java`
- [x] T029 [P] 建立事件 PaymentReservedEvent `shared-kernel/src/main/java/com/example/shared/event/PaymentReservedEvent.java`
- [x] T030 [P] 建立事件 PaymentReservationFailedEvent `shared-kernel/src/main/java/com/example/shared/event/PaymentReservationFailedEvent.java`
- [x] T031 [P] 建立事件 PaymentConfirmedEvent `shared-kernel/src/main/java/com/example/shared/event/PaymentConfirmedEvent.java`
- [x] T032 [P] 建立事件 PaymentReleasedEvent `shared-kernel/src/main/java/com/example/shared/event/PaymentReleasedEvent.java`
- [x] T033 [P] 建立事件 InventoryReservedEvent `shared-kernel/src/main/java/com/example/shared/event/InventoryReservedEvent.java`
- [x] T034 [P] 建立事件 InventoryReservationFailedEvent `shared-kernel/src/main/java/com/example/shared/event/InventoryReservationFailedEvent.java`
- [x] T035 [P] 建立事件 InventoryConfirmedEvent `shared-kernel/src/main/java/com/example/shared/event/InventoryConfirmedEvent.java`
- [x] T036 [P] 建立事件 InventoryReleasedEvent `shared-kernel/src/main/java/com/example/shared/event/InventoryReleasedEvent.java`

### 基礎聚合根

- [x] T037 建立 Order 聚合根基礎結構 `sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`（含 @Aggregate、欄位、建構子）
- [x] T038 建立 PaymentReservation 聚合根基礎結構 `payment-service/src/main/java/com/example/payment/domain/aggregate/PaymentReservation.java`
- [x] T039 建立 InventoryReservation 聚合根基礎結構 `inventory-service/src/main/java/com/example/inventory/domain/aggregate/InventoryReservation.java`
- [x] T040 [P] 建立 CustomerCredit 實體 `payment-service/src/main/java/com/example/payment/domain/entity/CustomerCredit.java`
- [x] T041 [P] 建立 Product 實體 `inventory-service/src/main/java/com/example/inventory/domain/entity/Product.java`

### 基礎設施

- [x] T042 [P] 建立 CustomerCredit 儲存庫介面 `payment-service/src/main/java/com/example/payment/domain/repository/CustomerCreditRepository.java`
- [x] T043 [P] 建立 Product 儲存庫介面 `inventory-service/src/main/java/com/example/inventory/domain/repository/ProductRepository.java`
- [x] T044 [P] 實作 CustomerCredit JPA 儲存庫 `payment-service/src/main/java/com/example/payment/infrastructure/persistence/JpaCustomerCreditRepository.java`
- [x] T045 [P] 實作 Product JPA 儲存庫 `inventory-service/src/main/java/com/example/inventory/infrastructure/persistence/JpaProductRepository.java`
- [x] T046 建立測試資料初始化 `sales-service/src/main/java/com/example/sales/infrastructure/DataInitializer.java`（含範例顧客、商品）

**Checkpoint**: 基礎建設完成 - 可開始使用者故事實作

---

## Phase 3: User Story 1 - 成功訂購商品 (Priority: P1) 🎯 MVP

**目標**: 實作完整的成功訂購流程，包含 Saga 協調、支付預留、庫存預留、確認交易

**獨立測試**: 發送訂購請求後，驗證訂單狀態為「已確認」，支付已扣款，庫存已減少

### Tests for User Story 1

- [ ] T047 [P] [US1] 建立 Order 聚合單元測試 `sales-service/src/test/java/com/example/sales/domain/aggregate/OrderTest.java`
- [ ] T048 [P] [US1] 建立 PaymentReservation 聚合單元測試 `payment-service/src/test/java/com/example/payment/domain/aggregate/PaymentReservationTest.java`
- [ ] T049 [P] [US1] 建立 InventoryReservation 聚合單元測試 `inventory-service/src/test/java/com/example/inventory/domain/aggregate/InventoryReservationTest.java`
- [ ] T050 [P] [US1] 建立 OrderSaga 單元測試（成功路徑）`sales-service/src/test/java/com/example/sales/domain/saga/OrderSagaSuccessTest.java`
- [ ] T051 [P] [US1] 建立訂單 API 契約測試 `sales-service/src/test/java/com/example/sales/contract/OrderApiContractTest.java`
- [ ] T052 [US1] 建立整合測試（成功訂購完整流程）`sales-service/src/test/java/com/example/sales/integration/SuccessfulOrderIntegrationTest.java`

### Implementation for User Story 1

#### Sales Service

- [ ] T053 [US1] 實作 Order 聚合命令處理器（CreateOrderCommand）`sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`
- [ ] T054 [US1] 實作 Order 聚合事件處理器（OrderCreatedEvent → 更新狀態）`sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`
- [ ] T055 [US1] 建立 OrderSaga 基礎結構 `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`（含 @Saga、狀態欄位）
- [ ] T056 [US1] 實作 OrderSaga 啟動邏輯（@StartSaga on OrderCreatedEvent）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T057 [US1] 實作 OrderSaga 處理 PaymentReservedEvent `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T058 [US1] 實作 OrderSaga 處理 InventoryReservedEvent `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T059 [US1] 實作 OrderSaga 兩者皆成功時發送確認命令邏輯 `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T060 [US1] 實作 Order 聚合確認命令處理器（ConfirmOrderCommand）`sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`
- [ ] T061 [US1] 建立 OrderApplicationService `sales-service/src/main/java/com/example/sales/application/service/OrderApplicationService.java`
- [ ] T062 [US1] 建立 CreateOrderRequest DTO `sales-service/src/main/java/com/example/sales/application/dto/CreateOrderRequest.java`
- [ ] T063 [US1] 建立 OrderResponse DTO `sales-service/src/main/java/com/example/sales/application/dto/OrderResponse.java`
- [ ] T064 [US1] 建立 OrderController REST 端點 `sales-service/src/main/java/com/example/sales/infrastructure/web/OrderController.java`
- [ ] T065 [US1] 建立 Order 查詢模型 `sales-service/src/main/java/com/example/sales/infrastructure/query/OrderQueryModel.java`
- [ ] T066 [US1] 建立 Order 查詢處理器 `sales-service/src/main/java/com/example/sales/infrastructure/query/OrderQueryHandler.java`

#### Payment Service

- [ ] T067 [US1] 實作 PaymentReservation 聚合命令處理器（ReservePaymentCommand）`payment-service/src/main/java/com/example/payment/domain/aggregate/PaymentReservation.java`
- [ ] T068 [US1] 實作 PaymentReservation 聚合確認命令處理器（ConfirmPaymentCommand）`payment-service/src/main/java/com/example/payment/domain/aggregate/PaymentReservation.java`
- [ ] T069 [US1] 建立 CustomerCreditService 處理額度檢查 `payment-service/src/main/java/com/example/payment/application/service/CustomerCreditService.java`

#### Inventory Service

- [ ] T070 [US1] 實作 InventoryReservation 聚合命令處理器（ReserveInventoryCommand）`inventory-service/src/main/java/com/example/inventory/domain/aggregate/InventoryReservation.java`
- [ ] T071 [US1] 實作 InventoryReservation 聚合確認命令處理器（ConfirmInventoryCommand）`inventory-service/src/main/java/com/example/inventory/domain/aggregate/InventoryReservation.java`
- [ ] T072 [US1] 建立 ProductStockService 處理庫存檢查 `inventory-service/src/main/java/com/example/inventory/application/service/ProductStockService.java`

**Checkpoint**: US1 完成 - 成功訂購流程可獨立測試驗證

---

## Phase 4: User Story 2 - 失敗時取消交易 (Priority: P2)

**目標**: 實作補償機制，當支付或庫存失敗時自動取消交易並釋放已預留資源

**獨立測試**: 模擬支付/庫存失敗，驗證訂單狀態為「已取消」且預留資源已釋放

### Tests for User Story 2

- [ ] T073 [P] [US2] 建立 OrderSaga 單元測試（支付失敗路徑）`sales-service/src/test/java/com/example/sales/domain/saga/OrderSagaPaymentFailureTest.java`
- [ ] T074 [P] [US2] 建立 OrderSaga 單元測試（庫存失敗路徑）`sales-service/src/test/java/com/example/sales/domain/saga/OrderSagaInventoryFailureTest.java`
- [ ] T075 [P] [US2] 建立 PaymentReservation 釋放單元測試 `payment-service/src/test/java/com/example/payment/domain/aggregate/PaymentReservationReleaseTest.java`
- [ ] T076 [P] [US2] 建立 InventoryReservation 釋放單元測試 `inventory-service/src/test/java/com/example/inventory/domain/aggregate/InventoryReservationReleaseTest.java`
- [ ] T077 [US2] 建立整合測試（支付失敗補償流程）`sales-service/src/test/java/com/example/sales/integration/PaymentFailureCompensationTest.java`
- [ ] T078 [US2] 建立整合測試（庫存失敗補償流程）`sales-service/src/test/java/com/example/sales/integration/InventoryFailureCompensationTest.java`

### Implementation for User Story 2

#### Sales Service

- [ ] T079 [US2] 實作 OrderSaga 處理 PaymentReservationFailedEvent `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T080 [US2] 實作 OrderSaga 處理 InventoryReservationFailedEvent `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T081 [US2] 實作 OrderSaga 補償邏輯（發送 ReleasePaymentCommand）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T082 [US2] 實作 OrderSaga 補償邏輯（發送 ReleaseInventoryCommand）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T083 [US2] 實作 Order 聚合取消命令處理器（CancelOrderCommand）`sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`
- [ ] T084 [US2] 實作 OrderSaga 結束邏輯（@EndSaga on compensation complete）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`

#### Payment Service

- [ ] T085 [US2] 實作 PaymentReservation 聚合釋放命令處理器（ReleasePaymentCommand）`payment-service/src/main/java/com/example/payment/domain/aggregate/PaymentReservation.java`
- [ ] T086 [US2] 實作 CustomerCredit 額度回復邏輯 `payment-service/src/main/java/com/example/payment/application/service/CustomerCreditService.java`

#### Inventory Service

- [ ] T087 [US2] 實作 InventoryReservation 聚合釋放命令處理器（ReleaseInventoryCommand）`inventory-service/src/main/java/com/example/inventory/domain/aggregate/InventoryReservation.java`
- [ ] T088 [US2] 實作 Product 庫存回復邏輯 `inventory-service/src/main/java/com/example/inventory/application/service/ProductStockService.java`

**Checkpoint**: US1 + US2 完成 - 成功與失敗路徑皆可獨立測試

---

## Phase 5: User Story 3 - 超時自動取消 (Priority: P3)

**目標**: 實作 15 秒超時機制，當服務未及時回應時自動取消交易

**獨立測試**: 模擬服務延遲超過 15 秒，驗證系統自動取消訂單

### Tests for User Story 3

- [ ] T089 [P] [US3] 建立 OrderSaga 超時單元測試 `sales-service/src/test/java/com/example/sales/domain/saga/OrderSagaTimeoutTest.java`
- [ ] T090 [US3] 建立整合測試（超時取消流程）`sales-service/src/test/java/com/example/sales/integration/TimeoutCancellationTest.java`

### Implementation for User Story 3

- [ ] T091 [US3] 實作 OrderSaga DeadlineManager 設定（15 秒超時）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T092 [US3] 實作 OrderSaga 超時處理器（@DeadlineHandler）`sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T093 [US3] 實作 OrderSaga 成功時取消 Deadline 邏輯 `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T094 [US3] 建立 SagaTimedOut 事件處理 `sales-service/src/main/java/com/example/sales/domain/saga/OrderSaga.java`
- [ ] T095 [US3] 實作 Order 狀態更新為 CANCELLED_TIMEOUT `sales-service/src/main/java/com/example/sales/domain/aggregate/Order.java`

**Checkpoint**: 所有使用者故事完成 - 完整 Saga 流程可驗證

---

## Phase 6: User Story 4 - 補償重試機制 (Priority: P4)

**目標**: 實作補償失敗時的重試機制（最多 3 次）

**獨立測試**: 模擬補償失敗，驗證重試機制與最終標記

### Tests for User Story 4

- [ ] T096 [P] [US4] 建立補償重試單元測試 `sales-service/src/test/java/com/example/sales/domain/saga/CompensationRetryTest.java`

### Implementation for User Story 4

- [ ] T097 [US4] 設定 Spring Retry 在 `sales-service/build.gradle.kts`
- [ ] T098 [US4] 建立 CompensationService 實作重試邏輯 `sales-service/src/main/java/com/example/sales/application/service/CompensationService.java`
- [ ] T099 [US4] 實作重試失敗後標記待人工處理 `sales-service/src/main/java/com/example/sales/application/service/CompensationService.java`
- [ ] T100 [US4] 建立 ManualInterventionRequired 事件 `shared-kernel/src/main/java/com/example/shared/event/ManualInterventionRequiredEvent.java`

**Checkpoint**: 補償重試機制完成

---

## Phase 7: Polish & Cross-Cutting Concerns

**目的**: 完善文件、效能最佳化、安全強化

- [ ] T101 [P] 建立 Mock WebSocket 通知服務 `sales-service/src/main/java/com/example/sales/infrastructure/notification/MockWebSocketNotifier.java`
- [ ] T102 [P] 實作訂單歷史查詢端點 `sales-service/src/main/java/com/example/sales/infrastructure/web/OrderController.java`
- [ ] T103 [P] 新增結構化日誌記錄 across all services
- [ ] T104 [P] 設定 Jacoco 測試覆蓋率報告 in root `build.gradle.kts`
- [ ] T105 執行 quickstart.md 驗證流程
- [ ] T106 產生測試覆蓋率報告並確認達 80%

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 無相依 - 可立即開始
- **Phase 2 (Foundational)**: 相依 Phase 1 - **阻塞所有使用者故事**
- **Phase 3-6 (User Stories)**: 相依 Phase 2
  - US1 → US2 → US3 → US4（建議順序）
  - US2-4 可在 US1 完成後平行進行
- **Phase 7 (Polish)**: 相依所有使用者故事完成

### User Story Dependencies

- **US1 (P1)**: Phase 2 完成後可開始 - 無其他故事相依
- **US2 (P2)**: Phase 2 完成後可開始 - 與 US1 共用基礎設施但獨立測試
- **US3 (P3)**: Phase 2 完成後可開始 - 與 US1/US2 共用基礎設施但獨立測試
- **US4 (P4)**: Phase 2 完成後可開始 - 增強 US2 功能

### Within Each User Story

- 測試必須先撰寫並確認失敗
- 聚合根優先於服務
- 服務優先於端點
- 核心實作優先於整合

### Parallel Opportunities

- Phase 1: T002-T011 可平行執行
- Phase 2: T013-T036（共用核心）可全部平行執行
- Phase 2: T040-T045（儲存庫）可平行執行
- Phase 3: T047-T051（US1 測試）可平行執行
- Phase 4: T073-T076（US2 測試）可平行執行

---

## Parallel Example: Phase 2 Shared Kernel

```bash
# 可同時執行所有值物件與列舉建立：
Task: "建立 Money 值物件 in shared-kernel/..."
Task: "建立 OrderItem 值物件 in shared-kernel/..."
Task: "建立 OrderStatus 列舉 in shared-kernel/..."
Task: "建立 ReservationStatus 列舉 in shared-kernel/..."

# 可同時執行所有命令建立：
Task: "建立 CreateOrderCommand in shared-kernel/..."
Task: "建立 ReservePaymentCommand in shared-kernel/..."
Task: "建立 ReserveInventoryCommand in shared-kernel/..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（關鍵 - 阻塞所有故事）
3. 完成 Phase 3: User Story 1
4. **停止並驗證**: 獨立測試 US1
5. 可部署/展示 MVP

### Incremental Delivery

1. Setup + Foundational → 基礎就緒
2. US1 → 獨立測試 → 部署（MVP!）
3. US2 → 獨立測試 → 部署（補償機制）
4. US3 → 獨立測試 → 部署（超時處理）
5. US4 → 獨立測試 → 部署（重試機制）
6. Polish → 最終交付

### Parallel Team Strategy

多人開發時：

1. 團隊共同完成 Setup + Foundational
2. Foundational 完成後：
   - 開發者 A: User Story 1
   - 開發者 B: User Story 2
   - 開發者 C: User Story 3
3. 各故事獨立完成與測試

---

## Notes

- [P] 任務 = 不同檔案，無相依
- [Story] 標籤對應 spec.md 中的使用者故事
- 每個使用者故事應可獨立完成與測試
- 實作前確認測試失敗
- 每個任務或邏輯群組後進行 commit
- 在任何 checkpoint 停止以獨立驗證故事
- 避免：模糊任務、檔案衝突、破壞獨立性的跨故事相依
