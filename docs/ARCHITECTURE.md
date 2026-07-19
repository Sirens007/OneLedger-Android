# OneLedger v0.1 架构与产品框架

## 1. 产品结构

底部导航固定为四个明确目的地：

1. **账本**：回答“最近花了什么、本月还可花多少”，并下钻到预算轨道和收支日历，承载快速记账入口。
2. **资产**：回答“我现在有多少钱、欠多少钱”，管理现金、银行卡、信用账户。
3. **存钱**：回答“目标还差多少”，支持定额、灵活、52 周、365 天计划。
4. **统计**：回答“钱去了哪里、趋势是否异常”，提供周/月/年/范围聚合。

第一版不做账号、云同步、OCR、AI、报销流和多币种换算。数据结构保留稳定 ID、创建/更新时间和软删除字段，为以后同步留接口，但不让远期功能拖慢本地 MVP。

## 2. 本地优先数据流

```text
Compose Screen
    ↓ 用户事件 / StateFlow
ViewModel
    ↓
LedgerRepository（接口）
    ↓
OfflineLedgerRepository
    ↓
Room DAO → SQLite
```

- UI 只消费不可变 `UiState`，不直接访问 DAO。
- DAO 暴露 `Flow`，数据库更新后四个页面自动重算。
- 账单新增、更新、软删除与恢复统一经 Repository；更新保留稳定 ID/创建时间，删除只写入 `deletedAt`，撤销清除该字段。
- 金额统一用最小货币单位 `Long` 保存，例如 `¥12.34 = 1234`，严禁 `Double`。
- 时间首版保存 epoch milliseconds；展示层再按本地时区分组。
- 系统自动云备份默认关闭；未来如增加加密备份，必须由用户主动开启并明确选择目的地。

## 3. 核心数据表

| 表 | 用途 | 关键字段 |
| --- | --- | --- |
| `ledger_books` | 多账本边界 | `id`, `name`, `currencyCode`, `isDefault` |
| `accounts` | 现金、储蓄卡、信用账户 | `type`, `openingBalanceMinor`, `includeInNetWorth` |
| `categories` | 收入/支出分类 | `transactionType`, `iconKey`, `colorHex`, `sortOrder` |
| `transactions` | 收入、支出、转账 | `amountMinor`, `accountId`, `toAccountId`, `categoryId`, `occurredAt` |
| `budgets` | 总预算或分类预算 | `periodStart`, `periodEnd`, `limitMinor`, `categoryId` |
| `savings_plans` | 存钱目标 | `method`, `targetMinor`, `savedMinor`, `startAt`, `endAt` |

`transactions` 同时预留 `relatedTransactionId`、`reimbursementState`、`deletedAt`，以后可以扩展退款/报销/同步，而不用破坏基础账单模型。

## 4. 工程边界

```text
com.oneledger.app
├── data/local        # Room entity、DAO、Database
├── data/repository   # 数据源实现、初始化数据
├── domain/model      # 跨层稳定模型
├── ui/components     # OneLedger 复用组件
├── ui/screens        # 主页面、预算详情、收支日历
├── ui/theme          # 品牌颜色、排版、圆角、主题
└── util              # 金额与日期等纯函数
```

现阶段采用单 app 模块、按功能分包。只有出现独立团队、显著构建瓶颈或可复用 SDK 时，才拆 `core:database`、`core:designsystem` 与 `feature:*`，避免个人项目被模块配置吞掉迭代速度。

## 5. 视觉与交互原则

- 使用 OneLedger 自己的“票据轨道”语言：固定品牌页眉、蓝绿轨道、资金脉冲和双轨日历；参考图只定义功能，不复用页面组合。
- 8dp 基准网格，页面水平边距 16dp；卡片以 12–18dp 圆角为主；紧凑控件的视觉尺寸可以更小，但可点击区域保持易触达。
- 金额使用等宽数字特性可读风格；标题收紧字距，正文保持系统字体。
- 高频切页不做整屏滑动；只移动底栏选中容器，约 180ms、无弹跳。
- 按下立即缩放到 0.97；快速记账 Sheet 从 `+` 的空间来源出现，保存时触发一次轻触觉反馈。
- 所有颜色都同时提供浅/深主题语义，次要文字仍满足可读对比度；图标必须有语义描述。

## 6. 迭代路线

### v0.1 本地 MVP

- CRUD、分类、账户、月预算、基础统计、导出 JSON/CSV。

### v0.2 完整个人财务

- 转账成对记账、退款/报销、周期账单、搜索筛选、数据库迁移测试、应用锁。

### v1.0 可发布版

- 加密备份、可选云同步、多设备冲突合并、无障碍与大屏适配、性能基准。

### v2.0 智能增强

- OCR/自然语言记账只作为可选输入层；解析结果必须先预览再入账，不让 AI 直接修改账本。
