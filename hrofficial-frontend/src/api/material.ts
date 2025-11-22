import http from '@/utils/http'
import type {
  Material,
  MaterialCategory,
  MaterialSubcategory,
  PageResponse
} from '@/types'

// 获取资料列表
export const getMaterials = (params?: {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  subcategoryId?: number
  keyword?: string
}) => {
  const query = new URLSearchParams(params as any).toString()
  return http.get<PageResponse<Material>>(`/api/materials${query ? '?' + query : ''}`)
}

// 上传资料
export const uploadMaterial = async (data: {
  title: string
  description?: string
  categoryId: number
  subcategoryId?: number
  file: File
}, onProgress?: (percent: number) => void) => {
  const formData = new FormData()
  formData.append('file', data.file)
  formData.append('categoryId', data.categoryId.toString())
  if (data.subcategoryId) {
    formData.append('subcategoryId', data.subcategoryId.toString())
  }
  formData.append('materialName', data.title)
  if (data.description) {
    formData.append('description', data.description)
  }

  const token = localStorage.getItem('token')
  const headers: any = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  return new Promise<any>((resolve, reject) => {
    const xhr = new XMLHttpRequest()

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

    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    xhr.open('POST', `${baseURL}/api/materials/upload`)
    Object.entries(headers).forEach(([key, value]) => {
      xhr.setRequestHeader(key, value as string)
    })
    xhr.send(formData)
  })
}

// 删除资料
export const deleteMaterial = (id: number) => {
  return http.delete(`/api/materials/${id}`)
}

// 更新资料信息
export const updateMaterial = (id: number, data: {
  title: string
  description?: string
  categoryId: number
  subcategoryId?: number
}) => {
  return http.put<Material>(`/api/materials/${id}`, {
    materialName: data.title,
    description: data.description,
    categoryId: data.categoryId,
    subcategoryId: data.subcategoryId
  })
}

// 获取下载预签名URL
export const getMaterialDownloadUrl = (id: number) => {
  return http.get<string>(`/api/materials/download-url/${id}`)
}

// 更新分类
export const updateCategory = (id: number, data: {
  name: string
  description?: string
}) => {
  return http.put(`/api/materials/category/${id}`, {
    categoryName: data.name,
    sortOrder: 0
  })
}

// 更新子分类
export const updateSubcategory = (id: number, data: {
  name: string
  description?: string
}) => {
  return http.put(`/api/materials/subcategory/${id}`, {
    subcategoryName: data.name,
    sortOrder: 0
  })
}

// 获取分类列表
export const getCategories = () => {
  return http.get<MaterialCategory[]>('/api/materials/categories')
}

// 获取分类下的子分类
export const getSubcategories = (categoryId: number) => {
  return http.get<MaterialSubcategory[]>(`/api/materials/category/${categoryId}/subcategories`)
}

// 创建分类
export const createCategory = (data: { name: string; description?: string }) => {
  return http.post<MaterialCategory>('/api/materials/category', {
    categoryName: data.name,  // 后端期望 categoryName
    sortOrder: 0
  })
}

// 创建子分类
export const createSubcategory = (categoryId: number, data: { name: string; description?: string }) => {
  return http.post<MaterialSubcategory>('/api/materials/subcategory', {
    categoryId,
    subcategoryName: data.name,  // 后端期望 subcategoryName
    sortOrder: 0
  })
}
