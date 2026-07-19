# OneLedger Git 协作规范

## 1. 基本策略

- `main` 始终保持可构建、可演示；禁止直接提交和强制推送。
- 每项工作从最新 `main` 建短生命周期分支，通过 PR 合并。
- 一个 PR 只解决一个清晰问题；功能、数据库迁移和大规模格式化不得混在一起。
- 提交前查看 `git diff` 和 `git status`，不提交密钥、构建产物、IDE 本地配置或真实账单。

## 2. 分支命名

```text
feature/budget-category-edit
fix/calendar-month-boundary
refactor/money-formatter
docs/design-system
test/room-migration-2-3
chore/gradle-versions
```

只用小写英文、数字和连字符；不使用姓名、`new`、`test2` 等无语义名称。

## 3. 提交格式

采用 [Conventional Commits 1.0](https://www.conventionalcommits.org/en/v1.0.0/)：

```text
<type>(<scope>): <简短说明>
```

| type | 用途 |
| --- | --- |
| `feat` | 用户可见的新能力 |
| `fix` | 缺陷修复 |
| `refactor` | 行为不变的代码重构 |
| `perf` | 性能改进 |
| `test` | 测试新增或修正 |
| `docs` | 仅文档 |
| `style` | 不改变行为的格式调整 |
| `build` / `ci` | 构建或持续集成 |
| `chore` | 其他维护 |

常用 scope：`ledger`、`budget`、`calendar`、`assets`、`savings`、`stats`、`database`、`design`、`docs`。

示例：

```text
feat(budget): 支持编辑分类预算
fix(calendar): 修正跨月日期归属
test(database): 覆盖版本 2 到 3 的迁移
docs(design): 补充动效与文案规范
```

破坏性变化在正文写 `BREAKING CHANGE:`。标题使用祈使语义、控制在约 50 个字符，不写句号，不使用“update files”。

## 4. 原子提交

每个提交应能独立解释并尽量独立构建。先提交纯重命名，再提交行为修改；生成的截图与对应 UI 修改放在同一提交；不要把无关文件“顺手修了”带进 PR。

## 5. PR 要求

PR 必须包含：用户结果、改动范围、测试证据、UI 前后截图（如适用）、数据库/隐私影响、已知限制。合并条件：

- 至少一次有效评审；作者不能用自我批准替代评审。
- `assembleDebug`、单元测试、Lint 和相关截图测试通过。
- 有数据库变更时迁移测试和回滚说明齐全。
- 用户可见变化已更新文档/发布说明。
- 评论已解决或记录为明确的后续 Issue。

默认使用 Squash merge，使 `main` 每个提交对应一个完整变更；Squash 标题保持 Conventional Commit 格式。

## 6. 版本与发布

采用语义化版本 `MAJOR.MINOR.PATCH`：不兼容的数据/行为变更升 MAJOR，新能力升 MINOR，兼容修复升 PATCH。发布标签格式 `v0.1.0`，标签只打在通过发布检查的 `main` 提交上。

发布分支只用于稳定期修复；紧急修复使用 `hotfix/<issue>`，修复后仍走 PR。禁止用修改历史掩盖已发布错误；用新提交修复并记录影响。

## 7. 安全与回滚

- `.keystore`、签名密码、API 密钥、导出的真实财务数据不得进入 Git。
- 发现密钥进入历史后，先吊销/轮换，再清理历史；仅删除当前文件不算完成。
- 代码回滚优先 `git revert`，避免对共享分支 `reset --hard` 或 force push。
- 数据库迁移无法简单回滚时，先保护数据，再发布修复；不得用 destructive migration 消除崩溃。

## 8. 本地提交前命令

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug validateDebugScreenshotTest compileDebugAndroidTestKotlin
git status --short
git diff --check
```

涉及 Room schema、Migration 或 DAO 行为时，必须另在允许 USB 安装测试 APK 的设备上执行 `.\gradlew.bat connectedDebugAndroidTest`。

界面变化只有得到确认后才运行 `updateDebugScreenshotTest`，不得为了让 CI 变绿直接覆盖基准图。
