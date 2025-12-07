# 實作計畫：Saga 訂單交易協調

**分支**: `001-saga-order-transaction` | **日期**: 2025-12-07 | **規格**: [spec.md](./spec.md)
**輸入**: 功能規格來自 `/specs/001-saga-order-transaction/spec.md`

<!--
  語言要求（憲法第 IX 條）：本計畫全文必須以繁體中文撰寫。
  僅技術識別碼（API 名稱、程式碼片段）可保留英文。
-->

## 摘要

實作一個基於 Axon Framework 的 Saga 訂單交易協調系統，採用 Choreography-based Saga 模式協調三個微服務（銷售、支付、庫存）之間的分散式交易。系統使用事件驅動架構，透過 Axon Server 進行非同步訊息傳遞，確保跨服務的最終一致性。

**核心功能**:
- 訂單建立與 Saga 協調流程
- 支付額度預留/確認/補償
- 庫存預留/扣減/釋放
- 15 秒超時自動取消機制
- 補償失敗重試（最多 3 次）

## 技術背景

**語言/版本**: Java JDK 17
**主要相依**: Spring Boot 3, Axon Framework
**儲存**: H2（開發環境）, Axon Server（事件儲存）
**測試**: JUnit 5, Mockito
**建置工具**: Gradle
**目標平台**: JVM / Linux Server
**專案類型**: Monorepo 微服務架構（三個 Spring Boot 應用程式）
**效能目標**: 100 TPS, <3s 初始回應, <30s 完整流程
**限制條件**: <200ms p95（API 回應）, 15 秒超時檢測精確度 ±1 秒
**規模範圍**: MVP 單一商品、單一金額

## 憲法檢查

*閘門：必須在 Phase 0 研究前通過。Phase 1 設計後需重新檢查。*

| 原則 | 閘門條件 | 狀態 |
|------|----------|------|
| I. 程式碼品質 | 循環複雜度 <10；公開 API 有型別定義 | ✅ 通過 |
| II. 測試標準 | 計畫 80% 覆蓋率；定義契約/整合測試 | ✅ 通過 |
| III. BDD | 所有使用者故事有 Given-When-Then 情境 | ✅ 通過（規格已定義） |
| IV. DDD | 通用語言已定義；限界上下文已識別 | ✅ 通過（規格已定義） |
| V. SOLID | 計畫依賴注入；抽象介面已定義 | ✅ 通過 |
| VI. 基礎設施隔離 | 框架程式碼僅限於基礎設施層 | ✅ 通過 |
| VII. UX 一致性 | 遵循設計系統模式；考慮無障礙性 | ✅ 通過（API 優先，Mock WebSocket） |
| VIII. 效能 | 回應時間 <200ms p95；查詢時間 <100ms | ✅ 通過 |
| IX. 文件語言 | 本計畫以繁體中文撰寫 | ✅ 通過 |

## 專案結構

### 文件（本功能）

```text
specs/001-saga-order-transaction/
├── plan.md              # 本檔案
├── spec.md              # 功能規格
├── research.md          # Phase 0 研究輸出
├── data-model.md        # Phase 1 資料模型
├── quickstart.md        # Phase 1 快速入門
├── contracts/           # Phase 1 API 契約
│   ├── sales-api.yaml
│   ├── payment-events.yaml
│   └── inventory-events.yaml
└── tasks.md             # Phase 2 任務清單（/speckit.tasks 產出）
```

### 原始碼（儲存庫根目錄）

```text
sales-service/
├── src/main/java/com/example/sales/
│   ├── domain/              # 領域層
│   │   ├── aggregate/       # Order 聚合根
│   │   ├── event/           # 領域事件
│   │   ├── command/         # 命令
│   │   └── saga/            # OrderSaga 協調器
│   ├── application/         # 應用層
│   │   ├── service/         # 應用服務
│   │   └── dto/             # 資料傳輸物件
│   ├── infrastructure/      # 基礎設施層
│   │   ├── persistence/     # 儲存庫實作
│   │   ├── messaging/       # Axon 設定
│   │   └── web/             # REST Controller
│   └── SalesServiceApplication.java
└── src/test/java/
    ├── contract/            # 契約測試
    ├── integration/         # 整合測試
    └── unit/                # 單元測試

payment-service/
├── src/main/java/com/example/payment/
│   ├── domain/
│   │   ├── aggregate/       # PaymentReservation 聚合根
│   │   ├── event/
│   │   └── command/
│   ├── application/
│   ├── infrastructure/
│   └── PaymentServiceApplication.java
└── src/test/java/

inventory-service/
├── src/main/java/com/example/inventory/
│   ├── domain/
│   │   ├── aggregate/       # InventoryReservation 聚合根
│   │   ├── event/
│   │   └── command/
│   ├── application/
│   ├── infrastructure/
│   └── InventoryServiceApplication.java
└── src/test/java/

shared-kernel/
├── src/main/java/com/example/shared/
│   ├── event/               # 共用領域事件
│   ├── command/             # 共用命令
│   └── valueobject/         # 共用值物件
└── src/test/java/
```

**結構決策**: 採用 Monorepo 微服務架構，三個獨立的 Spring Boot 應用程式透過 Axon Framework 進行事件驅動通訊。共用核心（shared-kernel）包含跨服務共用的事件與命令定義。

## 複雜度追蹤

> **僅在憲法檢查有違規需要理由時填寫**

| 違規 | 為何需要 | 拒絕更簡單替代方案的原因 |
|------|----------|--------------------------|
| 無 | N/A | N/A |
