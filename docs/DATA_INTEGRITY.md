# OneLedger 数据完整性基线

本文记录数据库 v2 起必须保持的约束，后续功能不得绕过这些边界。

## 账本隔离

- Repository 维护唯一的 `activeBookId`，账户、分类、账单、预算、存钱计划和账户余额 Flow 都由它切换。
- DAO 的读取、详情查询、软删除和恢复必须显式接收 `bookId`。
- 新增或编辑账单前，Repository 必须验证来源账户、转入账户和分类属于当前账本。
- UI 不直接访问 DAO，也不能自行拼接默认账本 ID。

## 安全写入

- 账本、账户、分类、预算和存钱计划使用 Room `@Upsert`。
- 禁止使用 `INSERT OR REPLACE` 更新带外键的父记录；SQLite 的 REPLACE 可能先删除旧行，进而触发级联删除。
- 账单保留稳定 ID；删除继续采用 `deletedAt` 软删除。

## 数据库 v2

- `transactions.toAccountId` 和 `transactions.categoryId` 增加外键。
- `budgets.categoryId` 增加外键。
- 预算增加 `scopeKey`；总预算使用 `__TOTAL__`，分类预算使用分类 ID。
- `(bookId, scopeKey, periodStart, periodEnd)` 唯一，保证同一账本同一周期只有一个相同作用域预算。
- v1→v2 只能通过 `MIGRATION_1_2` 升级，禁止破坏式重建。

## 聚合与计算

- 账户余额由 DAO 一次聚合收入、支出和转账，不再由每个账户遍历全部账单。
- 净资产只统计 `includeInNetWorth = true` 的账户。
- “剩余日均”按所选月份真实剩余天数计算并包含当天。
- 月/年统计的日均和年份必须使用页面传入的 `nowMillis`；“全部”按首笔记录到当前日的实际天数计算。

## 回归门禁

- `DatabaseMigrationTest` 使用真实 v1 schema 验证迁移、数据保留和外键完整性。
- `LedgerDaoTest` 验证跨账本隔离、余额聚合和父记录 Upsert 不删除子记录。
- 日期与净资产规则由 JVM 单元测试覆盖。
- 每次数据库变更必须提交新的 Room schema JSON，并通过单元测试、设备测试、截图回归、Lint 和 Debug 构建。
