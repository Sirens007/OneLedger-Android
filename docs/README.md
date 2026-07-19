# OneLedger 项目规范中心

> 状态：生效｜规范版本：1.1｜最后更新：2026-07-20

这里是 OneLedger 的单一事实来源（Single Source of Truth）。需求、设计、开发和评审出现分歧时，按以下优先级处理：用户数据安全与正确性 > 本目录规范 > 已确认的需求稿 > 临时口头约定 > 参考产品截图。

## 阅读顺序

1. [项目执行总则](PROJECT_PLAYBOOK.md)：每次迭代如何开始、验收和收尾。
2. [产品与整体框架](PRODUCT_FRAMEWORK.md)：做什么、不做什么、页面和数据如何组织。
3. [数据完整性基线](DATA_INTEGRITY.md)：账本隔离、安全写入、数据库迁移和聚合规则。
4. [视觉设计系统](DESIGN_SYSTEM.md)：颜色、排版、间距、组件和无障碍标准。
5. [动效与文案规范](MOTION_AND_CONTENT.md)：页面过渡、反馈节奏和用户可见文字。
6. [Git 协作规范](GIT_WORKFLOW.md)：分支、提交、PR、版本和回滚规则。
7. [质量检查清单](QUALITY_CHECKLIST.md)：功能完成前逐项勾选。
8. [技术架构明细](ARCHITECTURE.md)：当前数据模型、工程边界和版本路线。

## 使用方式

- 新功能开始前：建立需求卡，填写目标、范围、数据影响、界面状态和验收条件。
- 开发中：只使用设计令牌和复用组件；需求改变时先更新文档，再改代码。
- 提交前：执行质量清单和 Gradle 校验，不用“看起来没问题”代替验证。
- 合并后：更新变更日志或路线状态；只有经确认的视觉变化才能更新截图基准。

规范参考了企业设计系统的治理方法，但不复制任何产品的页面。腾讯 TDesign 强调通过颜色、尺寸、圆角、阴影和字体令牌保持多端一致；OneLedger 采用同样的令牌治理思想，并形成自己的“票据轨道”视觉语言。参考：[TDesign Design Token](https://tdesign.tencent.com/starter/docs/vue/design-token)、[Material 3](https://m3.material.io/)。
