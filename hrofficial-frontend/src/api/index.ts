/**
 * API 模块统一导出
 *
 * 提供所有后端接口的前端调用封装
 * @module api
 */

// 认证授权
export * from './auth'

// 用户管理
export * from './user'

// 活动管理
export * from './activity'

// 往届活动
export * from './pastActivity'

// 资料管理
export * from './material'

// AI 对话
export * from './ai'

// RAG 知识库
export * from './rag'

// OSS 存储
export * from './oss'

// 性能监控
export * from './performance'
