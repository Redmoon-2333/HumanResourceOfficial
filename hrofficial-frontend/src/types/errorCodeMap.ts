export type ErrorLevel = 'error' | 'warning' | 'info'

export type ErrorCategory =
  | 'user'
  | 'token'
  | 'permission'
  | 'request'
  | 'system'
  | 'activity'
  | 'material'
  | 'task'
  | 'message'
  | 'role'
  | 'general'

export interface ErrorCodeEntry {
  message: string
  level: ErrorLevel
  category: ErrorCategory
}

export const ErrorCodeMap: Record<number, ErrorCodeEntry> = {
  // 用户相关错误 1000-1999
  1001: { message: '用户不存在', level: 'warning', category: 'user' },
  1002: { message: '用户名已存在', level: 'warning', category: 'user' },
  1003: { message: '用户名或密码错误', level: 'warning', category: 'user' },
  1004: { message: '激活码无效或已过期', level: 'warning', category: 'user' },
  1005: { message: '两次输入的密码不一致', level: 'warning', category: 'user' },

  // Token相关错误 2000-2999
  2001: { message: '令牌无效或已过期', level: 'warning', category: 'token' },
  2002: { message: '需要提供有效的令牌', level: 'warning', category: 'token' },
  2003: { message: '缺少认证令牌', level: 'warning', category: 'token' },
  2004: { message: '令牌已过期', level: 'warning', category: 'token' },
  2005: { message: '令牌格式错误', level: 'warning', category: 'token' },

  // 权限相关错误 3000-3999
  3001: { message: '权限不足', level: 'warning', category: 'permission' },
  3002: { message: '只有部长或副部长才能执行此操作', level: 'warning', category: 'permission' },

  // 请求参数相关错误 4000-4999
  4001: { message: '请求参数不正确', level: 'warning', category: 'request' },
  4002: { message: '数据验证失败', level: 'warning', category: 'request' },

  // 冲突相关错误 4100-4199
  4101: { message: '资源冲突', level: 'warning', category: 'request' },

  // 系统错误 5000-5999
  5000: { message: '系统内部错误', level: 'error', category: 'system' },

  // 活动相关错误 6000-6999
  6001: { message: '活动不存在', level: 'warning', category: 'activity' },
  6002: { message: '活动已存在', level: 'warning', category: 'activity' },
  6003: { message: '活动创建失败', level: 'error', category: 'activity' },
  6004: { message: '活动更新失败', level: 'error', category: 'activity' },
  6005: { message: '活动删除失败', level: 'error', category: 'activity' },
  6006: { message: '活动列表获取失败', level: 'error', category: 'activity' },
  6007: { message: '活动详情获取失败', level: 'error', category: 'activity' },

  // 活动图片相关错误 6100-6199
  6101: { message: '活动图片保存失败', level: 'error', category: 'activity' },
  6102: { message: '活动图片删除失败', level: 'error', category: 'activity' },
  6103: { message: '活动图片列表获取失败', level: 'error', category: 'activity' },
  6104: { message: '活动图片不存在', level: 'warning', category: 'activity' },
  6105: { message: '活动图片更新失败', level: 'error', category: 'activity' },

  // 往届活动相关错误 7000-7999
  7001: { message: '资源不存在', level: 'warning', category: 'general' },
  7002: { message: '操作失败', level: 'error', category: 'general' },

  // 内部资料相关错误 8000-8999
  8001: { message: '文件上传失败', level: 'error', category: 'material' },
  8002: { message: '资料不存在', level: 'warning', category: 'material' },
  8003: { message: '资料上传失败', level: 'error', category: 'material' },
  8004: { message: '资料查询失败', level: 'error', category: 'material' },
  8005: { message: '资料更新失败', level: 'error', category: 'material' },
  8006: { message: '资料删除失败', level: 'error', category: 'material' },
  8007: { message: '资料下载失败', level: 'error', category: 'material' },
  8008: { message: '分类创建失败', level: 'error', category: 'material' },
  8009: { message: '分类查询失败', level: 'error', category: 'material' },
  8010: { message: '子分类创建失败', level: 'error', category: 'material' },
  8011: { message: '子分类查询失败', level: 'error', category: 'material' },

  // 任务相关错误 9001-9099
  9001: { message: '任务不存在', level: 'warning', category: 'task' },
  9002: { message: '任务指派记录不存在', level: 'warning', category: 'task' },
  9003: { message: '任务已完成', level: 'warning', category: 'task' },
  9004: { message: '非任务创建者，无权操作', level: 'warning', category: 'task' },
  9005: { message: '该字段不可修改', level: 'warning', category: 'task' },
  9010: { message: '催促冷却中', level: 'info', category: 'task' },
  9011: { message: '被指派人届别不匹配', level: 'warning', category: 'task' },
  9012: { message: '该部员已被指派此任务', level: 'warning', category: 'task' },

  // 站内信相关错误 9101-9199
  9101: { message: '站内信不存在', level: 'warning', category: 'message' },
  9102: { message: '无权操作该站内信', level: 'warning', category: 'message' },

  // 角色管理相关错误 9201-9299
  9201: { message: '非法角色值', level: 'warning', category: 'role' },
  9202: { message: '该届已存在部长', level: 'warning', category: 'role' },
  9203: { message: '副部长无权任命部长', level: 'warning', category: 'role' },
  9204: { message: '角色历史解析失败', level: 'error', category: 'role' },
  9205: { message: '同届必须有至少1位部长，无法自我降级', level: 'warning', category: 'role' },
  9206: { message: '学号已存在', level: 'warning', category: 'role' },
  9207: { message: '学号格式不正确', level: 'warning', category: 'role' }
}

export function isTokenErrorCode(code: number): boolean {
  return code >= 2001 && code <= 2999
}

export function isAuthErrorCode(code: number): boolean {
  return code === 1003 || isTokenErrorCode(code)
}

export function isPermissionErrorCode(code: number): boolean {
  return code >= 3001 && code <= 3999
}
