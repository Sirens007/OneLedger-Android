# 为 OneLedger 贡献

开始前请阅读 [项目规范中心](docs/README.md) 和 [Git 协作规范](docs/GIT_WORKFLOW.md)。

最短流程：从最新 `main` 创建语义化分支；保持提交原子并使用 Conventional Commits；完成 [质量清单](docs/QUALITY_CHECKLIST.md)；推送并按 PR 模板提供证据；评审和检查通过后 Squash 合并。

本地验证：

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug validateDebugScreenshotTest
```

安全问题请不要提交包含真实账单、密钥或签名文件的公开 Issue/PR。
