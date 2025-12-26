import type { ApiResponse } from '@/types'

// 开发环境使用代理，生产环境使用实际地址
const API_BASE_URL = import.meta.env.PROD 
  ? (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  : '' // 开发环境使用相对路径，通过Vite代理转发

// 请求拦截器
class HttpClient {
  private baseURL: string

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  private getToken(): string | null {
    return localStorage.getItem('token')
  }

  private getHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json'
    }

    const token = this.getToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    return headers
  }

  async request<T>(
    url: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const fullUrl = `${this.baseURL}${url}`
    const headers = this.getHeaders()

    // 为长时间请求添加超时控制（AI生成接口）
    const isAIRequest = url.includes('/api/ai/')
    const controller = new AbortController()
    const timeoutId = isAIRequest 
      ? setTimeout(() => controller.abort(), 180000) // AI请求3分钟超时
      : setTimeout(() => controller.abort(), 30000)  // 普通请求30秒超时

    try {
      const response = await fetch(fullUrl, {
        ...options,
        headers: {
          ...headers,
          ...options.headers
        },
        signal: controller.signal
      })

      if (!response.ok) {
        if (response.status === 401) {
          // Token过期或未登录，清除token并提示登录
          const wasLoggedIn = !!localStorage.getItem('token')
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          
          if (wasLoggedIn) {
            // 如果之前是登录状态，token过期后自动退出
            window.location.href = '/login?expired=1'
            throw new Error('登录已过期，请重新登录')
          } else {
            // 如果未登录，提示需要登录
            throw new Error('请先登录')
          }
        }
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const data = await response.json()
      return data
    } catch (error: any) {
      if (error.name === 'AbortError') {
        console.error('请求超时:', url)
        throw new Error('请求超时，请重试或使用流式输出')
      }
      console.error('请求失败:', error)
      throw error
    } finally {
      clearTimeout(timeoutId)
    }
  }

  async get<T>(url: string): Promise<ApiResponse<T>> {
    return this.request<T>(url, { method: 'GET' })
  }

  async post<T>(url: string, body?: any): Promise<ApiResponse<T>> {
    return this.request<T>(url, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined
    })
  }

  async put<T>(url: string, body?: any): Promise<ApiResponse<T>> {
    return this.request<T>(url, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined
    })
  }

  async delete<T>(url: string): Promise<ApiResponse<T>> {
    return this.request<T>(url, { method: 'DELETE' })
  }

  // 上传文件
  async upload<T>(url: string, file: File, onProgress?: (percent: number) => void): Promise<ApiResponse<T>> {
    const formData = new FormData()
    formData.append('file', file)

    const token = this.getToken()
    const headers: HeadersInit = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()

      // 监听上传进度
      if (onProgress) {
        xhr.upload.addEventListener('progress', (e) => {
          if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100)
            onProgress(percent)
          }
        })
      }

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          const data = JSON.parse(xhr.responseText)
          resolve(data)
        } else {
          reject(new Error(`上传失败: ${xhr.statusText}`))
        }
      })

      xhr.addEventListener('error', () => {
        reject(new Error('网络错误'))
      })

      xhr.open('POST', `${this.baseURL}${url}`)
      Object.entries(headers).forEach(([key, value]) => {
        xhr.setRequestHeader(key, value as string)
      })
      xhr.send(formData)
    })
  }

  // 流式请求
  async stream(url: string, body?: any, onChunk?: (chunk: string) => void): Promise<string> {
    const fullUrl = `${this.baseURL}${url}`
    const headers = this.getHeaders()

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 300000) // 5分钟超时

    try {
      const response = await fetch(fullUrl, {
        method: 'POST',
        headers,
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法读取响应流')
      }

      const decoder = new TextDecoder('utf-8')
      let fullContent = ''
      let buffer = '' // 用于存储未完成的SSE消息

      try {
        while (true) {
          const { value, done } = await reader.read()

          if (done) {
            console.log('流式读取完成')
            break
          }

          if (value) {
            const text = decoder.decode(value, { stream: true })
            buffer += text
            
            // 解析SSE格式：data: 开头的行
            const lines = buffer.split('\n')
            buffer = lines[lines.length - 1] || '' // 保留最后一个可能不完整的行
            
            for (let i = 0; i < lines.length - 1; i++) {
              const line = lines[i]?.trim() || ''
              
              // 跳过空行和注释
              if (!line || line.startsWith(':')) {
                continue
              }
              
              // 解析 data: 格式
              if (line.startsWith('data:')) {
                const content = line.substring(5) // 去掉 "data:" 前缀，保留空白符
                if (content) {
                  fullContent += content
                  if (onChunk) {
                    onChunk(content)
                  }
                }
              }
            }
          }
        }

        // 处理buffer中剩余的数据
        if (buffer.trim()) {
          const line = buffer.trim()
          if (line.startsWith('data:')) {
            const content = line.substring(5) // 保留空白符
            if (content) {
              fullContent += content
              if (onChunk) {
                onChunk(content)
              }
            }
          }
        }

        // 解码最后的数据
        const finalChunk = decoder.decode()
        if (finalChunk) {
          const line = finalChunk.trim()
          if (line.startsWith('data:')) {
            const content = line.substring(5) // 保留空白符
            if (content) {
              fullContent += content
              if (onChunk) {
                onChunk(content)
              }
            }
          }
        }
      } catch (readError) {
        console.error('读取流时发生错误:', readError)
        // 如果已经读取了部分内容，不抛出错误
        if (fullContent.length === 0) {
          throw readError
        }
      } finally {
        reader.releaseLock()
      }

      return fullContent
    } finally {
      clearTimeout(timeoutId)
    }
  }
}

export const http = new HttpClient(API_BASE_URL)
export default http
