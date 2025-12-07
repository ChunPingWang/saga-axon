# 快速入門：Saga 訂單交易協調

**功能分支**: `001-saga-order-transaction`
**建立日期**: 2025-12-07

## 前置需求

- Java JDK 17+
- Gradle 8.x
- Docker（用於 Axon Server）

## 環境設定

### 1. 啟動 Axon Server

```bash
# 使用 Docker 啟動 Axon Server
docker run -d --name axon-server \
  -p 8024:8024 -p 8124:8124 \
  axoniq/axonserver:latest

# 驗證 Axon Server 已啟動
curl http://localhost:8024/actuator/health
```

Axon Server 管理介面：http://localhost:8024

### 2. 建置專案

```bash
# 在儲存庫根目錄
./gradlew clean build

# 僅建置不執行測試
./gradlew build -x test
```

### 3. 啟動微服務

```bash
# 終端機 1：啟動 Sales Service
./gradlew :sales-service:bootRun

# 終端機 2：啟動 Payment Service
./gradlew :payment-service:bootRun

# 終端機 3：啟動 Inventory Service
./gradlew :inventory-service:bootRun
```

## 基本操作

### 建立訂單

```bash
# 發送訂購請求
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "IPHONE17"
  }'
```

預期回應（202 Accepted）：
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "訂單已受理，正在處理中",
  "createdAt": "2025-12-07T10:30:00Z"
}
```

### 查詢訂單狀態

```bash
curl http://localhost:8080/api/v1/orders/{orderId}
```

預期回應：
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "productId": "IPHONE17",
  "quantity": 1,
  "amount": 35000,
  "status": "CONFIRMED",
  "statusMessage": "訂單已確認",
  "createdAt": "2025-12-07T10:30:00Z",
  "updatedAt": "2025-12-07T10:30:05Z"
}
```

### 查詢訂單歷史

```bash
curl http://localhost:8080/api/v1/orders/{orderId}/history
```

## 測試情境

### 情境 1：成功訂購（Happy Path）

**前置條件**:
- 顧客 CUST-001 有 50,000 元信用額度
- iPhone 17 庫存 > 0

**執行**:
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST-001", "productId": "IPHONE17"}'
```

**預期結果**: 訂單狀態最終為 `CONFIRMED`

### 情境 2：信用額度不足

**前置條件**:
- 顧客 CUST-002 信用額度 < 35,000 元

**執行**:
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST-002", "productId": "IPHONE17"}'
```

**預期結果**: 訂單狀態為 `CANCELLED`，原因為「信用額度不足」

### 情境 3：庫存不足

**前置條件**:
- iPhone 17 庫存 = 0

**執行**:
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST-001", "productId": "IPHONE17"}'
```

**預期結果**: 訂單狀態為 `CANCELLED`，原因為「商品已售完」，支付預留已釋放

### 情境 4：超時取消

**前置條件**:
- 模擬支付服務延遲 > 15 秒

**預期結果**: 訂單狀態為 `CANCELLED_TIMEOUT`

## 執行測試

```bash
# 執行所有測試
./gradlew test

# 執行特定模組測試
./gradlew :sales-service:test

# 執行整合測試
./gradlew integrationTest

# 產生測試報告
./gradlew jacocoTestReport
```

## 監控與除錯

### Axon Server Dashboard

訪問 http://localhost:8024 查看：
- 已註冊的命令處理器
- 事件儲存狀態
- 查詢處理器
- 命令/事件流量

### 日誌查看

```bash
# Sales Service 日誌
tail -f sales-service/logs/application.log

# 過濾 Saga 相關日誌
grep -i saga sales-service/logs/application.log
```

### H2 Console

開發環境可透過 H2 Console 查看資料庫：
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:salesdb`

## 常見問題

### Q: Axon Server 連線失敗

```
確認 Axon Server 已啟動：
docker ps | grep axon-server

檢查連線設定（application.yml）：
axon:
  axonserver:
    servers: localhost:8124
```

### Q: 訂單一直在 PROCESSING 狀態

```
1. 檢查 Payment 和 Inventory 服務是否正常運行
2. 查看 Axon Server Dashboard 確認事件是否正確路由
3. 檢查各服務日誌是否有錯誤
```

### Q: 測試無法啟動

```
確保測試環境使用嵌入式 Axon Server：
./gradlew test -Dspring.profiles.active=test
```

## 下一步

1. 執行 `/speckit.tasks` 產生任務清單
2. 依據任務清單進行實作
3. 實作完成後執行整合測試驗證
