<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Layout from '@/components/Layout.vue'
import { getMaterials, uploadMaterial, deleteMaterial, updateMaterial, getMaterialDownloadUrl, getCategories, getSubcategories, createCategory, createSubcategory, updateCategory, updateSubcategory } from '@/api/material'
import type { Material, MaterialCategory, MaterialSubcategory } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const materials = ref<Material[]>([])
const categories = ref<MaterialCategory[]>([])
const currentView = ref<'categories' | 'subcategories' | 'materials'>('categories')
const selectedCategory = ref<MaterialCategory | null>(null)
const selectedSubcategory = ref<MaterialSubcategory | null>(null)

const uploadDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const editCategoryDialogVisible = ref(false)
const subcategoryDialogVisible = ref(false)
const editSubcategoryDialogVisible = ref(false)
const editMaterialDialogVisible = ref(false)
const currentEditMaterial = ref<Material | null>(null)
const currentEditSubcategory = ref<MaterialSubcategory | null>(null)
const currentEditCategory = ref<MaterialCategory | null>(null)
const generatedDownloadUrl = ref<string>('')
const currentDownloadMaterial = ref<Material | null>(null)

const uploadForm = ref({
  title: '',
  description: '',
  file: null as File | null
})
const uploadProgress = ref(0)

const categoryForm = ref({
  name: '',
  description: ''
})

const subcategoryForm = ref({
  name: '',
  description: ''
})

// 加载分类
const loadCategories = async () => {
  loading.value = true
  try {
    const res = await getCategories()
    console.log('后端返回的分类数据:', res)
    if (res.code === 200 && res.data) {
      console.log('原始分类数组:', res.data)
      // 后端返回的是 categoryId/categoryName，需要映射为前端期望的 id/name
      categories.value = await Promise.all(
        (res.data as any[])
          .filter((cat: any) => cat && cat.categoryId != null)
          .map(async (cat: any) => {
            // 加载每个分类的子分类数量
            let subcategoryCount = 0
            try {
              const subRes = await getSubcategories(cat.categoryId)
              if (subRes.code === 200 && subRes.data) {
                subcategoryCount = subRes.data.length
              }
            } catch (error) {
              console.error(`加载分类 ${cat.categoryId} 的子分类数量失败:`, error)
            }
            
            return {
              id: cat.categoryId,
              name: cat.categoryName,
              description: cat.description,
              sortOrder: cat.sortOrder,
              createTime: cat.createTime,
              subcategories: [],
              subcategoryCount  // 添加子分类数量
            }
          })
      )
      console.log('映射后的分类数组:', categories.value)
    }
  } catch (error: any) {
    console.error('加载分类失败:', error)
    ElMessage.error(error.message || '加载分类失败')
  } finally {
    loading.value = false
  }
}

// 加载资料
const loadMaterials = async () => {
  if (!selectedSubcategory.value) return
  
  loading.value = true
  try {
    const res = await getMaterials({
      subcategoryId: selectedSubcategory.value.id
    })
    if (res.code === 200 && res.data) {
      if (Array.isArray(res.data)) {
        // 映射后端字段
        materials.value = (res.data as any[]).map((mat: any) => ({
          id: mat.materialId,
          title: mat.materialName,
          description: mat.description,
          fileUrl: mat.fileUrl,
          fileSize: mat.fileSize,
          fileType: mat.fileType,
          uploaderName: mat.uploaderName,
          uploadTime: mat.uploadTime,
          downloadCount: mat.downloadCount,
          categoryId: mat.categoryId,
          subcategoryId: mat.subcategoryId
        }))
      } else {
        materials.value = res.data.list || []
      }
    }
  } catch (error: any) {
    console.error('加载资料失败:', error)
    ElMessage.error(error.message || '加载资料失败')
  } finally {
    loading.value = false
  }
}

// 点击大分类
const handleCategoryClick = async (category: MaterialCategory) => {
  selectedCategory.value = category
  selectedSubcategory.value = null
  currentView.value = 'subcategories'
  
  // 加载子分类
  await loadSubcategories(category.id)
}

// 加载子分类
const loadSubcategories = async (categoryId: number) => {
  loading.value = true
  try {
    const res = await getSubcategories(categoryId)
    console.log('子分类数据:', res)
    if (res.code === 200 && res.data) {
      // 映射后端字段
      const subcategories = (res.data as any[]).map((sub: any) => ({
        id: sub.subcategoryId,
        name: sub.subcategoryName,
        description: sub.description,
        sortOrder: sub.sortOrder,
        createTime: sub.createTime
      }))
      
      // 更新当前分类的子分类
      if (selectedCategory.value) {
        selectedCategory.value.subcategories = subcategories
      }
    }
  } catch (error: any) {
    console.error('加载子分类失败:', error)
    ElMessage.error(error.message || '加载子分类失败')
  } finally {
    loading.value = false
  }
}

// 点击小分类
const handleSubcategoryClick = (subcategory: MaterialSubcategory) => {
  selectedSubcategory.value = subcategory
  currentView.value = 'materials'
  loadMaterials()
}

// 返回大分类
const backToCategories = () => {
  selectedCategory.value = null
  selectedSubcategory.value = null
  currentView.value = 'categories'
}

// 返回小分类
const backToSubcategories = () => {
  selectedSubcategory.value = null
  materials.value = []
  currentView.value = 'subcategories'
}

// 创建大分类
const handleCreateCategory = () => {
  categoryForm.value = { name: '', description: '' }
  categoryDialogVisible.value = true
}

const handleSaveCategory = async () => {
  if (!categoryForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  loading.value = true
  try {
    const res = await createCategory({
      name: categoryForm.value.name,
      description: categoryForm.value.description
    })

    if (res.code === 200) {
      ElMessage.success('创建成功')
      categoryDialogVisible.value = false
      loadCategories()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error: any) {
    console.error('创建分类失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 创建小分类
const handleCreateSubcategory = () => {
  subcategoryForm.value = { name: '', description: '' }
  subcategoryDialogVisible.value = true
}

const handleSaveSubcategory = async () => {
  if (!subcategoryForm.value.name) {
    ElMessage.warning('请输入子分类名称')
    return
  }
  if (!selectedCategory.value) {
    ElMessage.error('请先选择大分类')
    return
  }

  loading.value = true
  try {
    const res = await createSubcategory(selectedCategory.value.id, {
      name: subcategoryForm.value.name,
      description: subcategoryForm.value.description
    })

    if (res.code === 200) {
      ElMessage.success('创建成功')
      subcategoryDialogVisible.value = false
      // 重新加载当前分类的子分类
      await loadSubcategories(selectedCategory.value.id)
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error: any) {
    console.error('创建子分类失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 文件选择
const handleFileChange = (file: any) => {
  uploadForm.value.file = file.raw
  if (!uploadForm.value.title) {
    uploadForm.value.title = file.raw.name
  }
  return false
}

// 上传资料
const handleUpload = async () => {
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入资料标题')
    return
  }
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!selectedSubcategory.value) {
    ElMessage.error('请先选择小分类')
    return
  }

  loading.value = true
  uploadProgress.value = 0

  try {
    const res = await uploadMaterial(
      {
        ...uploadForm.value,
        categoryId: selectedCategory.value!.id,
        subcategoryId: selectedSubcategory.value.id,
        file: uploadForm.value.file!
      } as any,
      (percent) => {
        uploadProgress.value = percent
      }
    )

    if (res.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      resetUploadForm()
      loadMaterials()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传资料失败:', error)
    ElMessage.error(error.message || '上传失败')
  } finally {
    loading.value = false
  }
}

// 重置上传表单
const resetUploadForm = () => {
  uploadForm.value = {
    title: '',
    description: '',
    file: null
  }
  uploadProgress.value = 0
}

// 删除资料
const handleDeleteMaterial = async (material: Material) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资料“${material.title}”吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const res = await deleteMaterial(material.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadMaterials()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除资料失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 编辑资料
const handleEditMaterial = (material: Material) => {
  currentEditMaterial.value = material
  uploadForm.value = {
    title: material.title,
    description: material.description || '',
    file: null
  }
  editMaterialDialogVisible.value = true
}

// 保存资料修改
const handleSaveEditMaterial = async () => {
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入资料标题')
    return
  }
  if (!currentEditMaterial.value || !selectedCategory.value || !selectedSubcategory.value) {
    ElMessage.error('缺少必要信息')
    return
  }

  loading.value = true
  try {
    const res = await updateMaterial(currentEditMaterial.value.id, {
      title: uploadForm.value.title,
      description: uploadForm.value.description,
      categoryId: selectedCategory.value.id,
      subcategoryId: selectedSubcategory.value.id
    })

    if (res.code === 200) {
      ElMessage.success('修改成功')
      editMaterialDialogVisible.value = false
      loadMaterials()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (error: any) {
    console.error('修改资料失败:', error)
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}

// 生成下载链接
const handleGenerateDownloadLink = async (material: Material) => {
  try {
    loading.value = true
    const res = await getMaterialDownloadUrl(material.id)
    if (res.code === 200 && res.data) {
      generatedDownloadUrl.value = res.data
      currentDownloadMaterial.value = material
      ElMessage.success('下载链接已生成，链接1小时内有效')
    } else {
      ElMessage.error(res.message || '生成下载链接失败')
    }
  } catch (error: any) {
    console.error('生成下载链接失败:', error)
    ElMessage.error(error.message || '生成下载链接失败')
  } finally {
    loading.value = false
  }
}

// 下载文件
const handleDownloadFile = () => {
  if (generatedDownloadUrl.value) {
    window.open(generatedDownloadUrl.value, '_blank')
  }
}

// 复制链接
const handleCopyLink = async () => {
  if (generatedDownloadUrl.value) {
    try {
      await navigator.clipboard.writeText(generatedDownloadUrl.value)
      ElMessage.success('链接已复制到剪贴板')
    } catch (error) {
      ElMessage.error('复制失败，请手动复制')
    }
  }
}

// 删除分类
const handleDeleteCategory = async (category: MaterialCategory) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类“${category.name}”吗？删除后该分类下的所有子分类和资料都将被删除！`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    // TODO: 调用后端删除分类接口
    ElMessage.info('后端删除分类接口待实现')
    // const res = await deleteCategory(category.id)
    // if (res.code === 200) {
    //   ElMessage.success('删除成功')
    //   loadCategories()
    // }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除分类失败:', error)
    }
  } finally {
    loading.value = false
  }
}

// 编辑分类
const handleEditCategory = (category: MaterialCategory) => {
  currentEditCategory.value = category
  categoryForm.value = {
    name: category.name,
    description: category.description || ''
  }
  editCategoryDialogVisible.value = true
}

// 保存分类编辑
const handleSaveEditCategory = async () => {
  if (!categoryForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (!currentEditCategory.value) {
    ElMessage.error('缺少必要信息')
    return
  }

  loading.value = true
  try {
    const res = await updateCategory(currentEditCategory.value.id, {
      name: categoryForm.value.name,
      description: categoryForm.value.description
    })

    if (res.code === 200) {
      ElMessage.success('修改成功')
      editCategoryDialogVisible.value = false
      loadCategories()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (error: any) {
    console.error('修改分类失败:', error)
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}

// 删除子分类
const handleDeleteSubcategory = async (subcategory: MaterialSubcategory) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除子分类“${subcategory.name}”吗？删除后该子分类下的所有资料都将被删除！`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    // TODO: 调用后端删除子分类接口
    ElMessage.info('后端删除子分类接口待实现')
    // const res = await deleteSubcategory(subcategory.id)
    // if (res.code === 200) {
    //   ElMessage.success('删除成功')
    //   if (selectedCategory.value) {
    //     await loadSubcategories(selectedCategory.value.id)
    //   }
    // }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除子分类失败:', error)
    }
  } finally {
    loading.value = false
  }
}

// 编辑子分类
const handleEditSubcategory = (subcategory: MaterialSubcategory) => {
  currentEditSubcategory.value = subcategory
  subcategoryForm.value = {
    name: subcategory.name,
    description: subcategory.description || ''
  }
  editSubcategoryDialogVisible.value = true
}

// 保存子分类编辑
const handleSaveEditSubcategory = async () => {
  if (!subcategoryForm.value.name) {
    ElMessage.warning('请输入子分类名称')
    return
  }
  if (!currentEditSubcategory.value) {
    ElMessage.error('缺少必要信息')
    return
  }

  loading.value = true
  try {
    const res = await updateSubcategory(currentEditSubcategory.value.id, {
      name: subcategoryForm.value.name,
      description: subcategoryForm.value.description
    })
    
    if (res.code === 200) {
      ElMessage.success('修改成功')
      editSubcategoryDialogVisible.value = false
      if (selectedCategory.value) {
        await loadSubcategories(selectedCategory.value.id)
      }
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (error: any) {
    console.error('修改子分类失败:', error)
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 获取当前分类的小分类
const currentSubcategories = computed(() => {
  if (!selectedCategory.value) return []
  const subcategories = selectedCategory.value.subcategories || []
  return subcategories.filter(sub => sub && sub.id != null && sub.name)
})

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <Layout>
    <div class="materials-container">
      <!-- 面包屑导航 -->
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item>
          <a @click="backToCategories" style="cursor: pointer">资料管理</a>
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="selectedCategory">
          <a @click="backToSubcategories" style="cursor: pointer">{{ selectedCategory.name }}</a>
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="selectedSubcategory">
          {{ selectedSubcategory.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 大分类视图 -->
      <div v-if="currentView === 'categories'" v-loading="loading">
        <el-card class="header-card">
          <div class="header-content">
            <h2>资料分类</h2>
            <el-button 
              v-if="userStore.isMinister" 
              type="primary" 
              @click="handleCreateCategory"
            >
              <el-icon><FolderAdd /></el-icon>
              新建分类
            </el-button>
          </div>
        </el-card>

        <div class="categories-grid">
          <el-card
            v-for="category in categories"
            :key="category.id"
            class="category-card hover-card"
          >
            <div class="category-content" @click="handleCategoryClick(category)">
              <el-icon :size="48" color="var(--color-primary)">
                <Folder />
              </el-icon>
              <h3>{{ category.name }}</h3>
              <el-tag>{{ category.subcategoryCount || 0 }} 个子分类</el-tag>
            </div>
            <div v-if="userStore.isMinister" class="card-actions" @click.stop>
              <el-button type="warning" size="small" @click="handleEditCategory(category)">
                编辑
              </el-button>
              <el-button type="danger" size="small" @click="handleDeleteCategory(category)">
                删除分类
              </el-button>
            </div>
          </el-card>
        </div>

        <el-empty v-if="!loading && categories.length === 0" description="暂无分类" />
      </div>

      <!-- 小分类视图 -->
      <div v-if="currentView === 'subcategories'" v-loading="loading">
        <el-card class="header-card">
          <div class="header-content">
            <h2>{{ selectedCategory?.name }} - 子分类</h2>
            <el-button 
              v-if="userStore.isMinister" 
              type="primary" 
              @click="handleCreateSubcategory"
            >
              <el-icon><FolderAdd /></el-icon>
              新建子分类
            </el-button>
          </div>
        </el-card>

        <div class="categories-grid">
          <el-card
            v-for="subcategory in currentSubcategories"
            :key="subcategory.id"
            class="category-card hover-card"
          >
            <div class="category-content" @click="handleSubcategoryClick(subcategory)">
              <el-icon :size="48" color="var(--color-secondary)">
                <Document />
              </el-icon>
              <h3>{{ subcategory.name }}</h3>
            </div>
            <div v-if="userStore.isMinister" class="card-actions" @click.stop>
              <el-button type="warning" size="small" @click="handleEditSubcategory(subcategory)">
                编辑
              </el-button>
              <el-button type="danger" size="small" @click="handleDeleteSubcategory(subcategory)">
                删除子分类
              </el-button>
            </div>
          </el-card>
        </div>

        <el-empty v-if="!loading && currentSubcategories.length === 0" description="暂无子分类" />
      </div>

      <!-- 资料列表视图 -->
      <div v-if="currentView === 'materials'" v-loading="loading">
        <el-card class="header-card">
          <div class="header-content">
            <h2>{{ selectedSubcategory?.name }} - 资料</h2>
            <el-button 
              v-if="userStore.isMember" 
              type="primary" 
              @click="uploadDialogVisible = true"
            >
              <el-icon><Upload /></el-icon>
              上传资料
            </el-button>
          </div>
        </el-card>

        <div class="materials-grid">
          <el-card
            v-for="material in materials"
            :key="material.id"
            class="material-card"
          >
            <div class="material-header">
              <el-icon :size="48" color="var(--color-primary)">
                <Document />
              </el-icon>
            </div>
            <div class="material-body">
              <h3 class="material-title">{{ material.title }}</h3>
              <p class="material-description">{{ material.description || '无描述' }}</p>
              <div class="material-info">
                <span>上传者：{{ material.uploaderName }}</span>
                <span>大小：{{ formatFileSize(material.fileSize) }}</span>
              </div>
              <div class="material-info">
                <span>下载次数：{{ material.downloadCount }}</span>
              </div>
            </div>
            <div class="material-footer">
              <el-button type="primary" size="small" @click="handleGenerateDownloadLink(material)">
                <el-icon><Download /></el-icon>
                生成下载链接
              </el-button>
              <el-button
                v-if="userStore.isMinister"
                type="warning"
                size="small"
                @click="handleEditMaterial(material)"
              >
                编辑
              </el-button>
              <el-button
                v-if="userStore.isMinister"
                type="danger"
                size="small"
                @click="handleDeleteMaterial(material)"
              >
                删除
              </el-button>
            </div>
            
            <!-- 下载链接显示区域 -->
            <div 
              v-if="generatedDownloadUrl && currentDownloadMaterial?.id === material.id" 
              class="download-link-section"
            >
              <div class="download-link-label">下载链接（有效期1小时）：</div>
              <div class="download-link-content">
                <el-input 
                  :model-value="generatedDownloadUrl" 
                  readonly 
                  size="small"
                  class="download-link-input"
                />
                <el-button 
                  type="success" 
                  size="small" 
                  @click="handleDownloadFile"
                  class="download-action-btn"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleCopyLink"
                  class="download-action-btn"
                >
                  <el-icon><DocumentCopy /></el-icon>
                  复制
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <el-empty v-if="!loading && materials.length === 0" description="暂无资料" />
      </div>

      <!-- 上传资料对话框 -->
      <el-dialog
        v-model="uploadDialogVisible"
        title="上传资料"
        width="600px"
        @close="resetUploadForm"
      >
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="资料标题" required>
            <el-input v-model="uploadForm.title" placeholder="请输入资料标题" />
          </el-form-item>
          <el-form-item label="资料描述">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入资料描述"
            />
          </el-form-item>
          <el-form-item label="选择文件" required>
            <el-upload
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
              drag
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或<em>点击上传</em>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item v-if="uploadProgress > 0">
            <el-progress :percentage="uploadProgress" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpload" :loading="loading">
            上传
          </el-button>
        </template>
      </el-dialog>

      <!-- 创建大分类对话框 -->
      <el-dialog
        v-model="categoryDialogVisible"
        title="创建分类"
        width="500px"
      >
        <el-form :model="categoryForm" label-width="100px">
          <el-form-item label="分类名称" required>
            <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCategory" :loading="loading">
            确定
          </el-button>
        </template>
      </el-dialog>

      <!-- 创建小分类对话框 -->
      <el-dialog
        v-model="subcategoryDialogVisible"
        title="创建子分类"
        width="500px"
      >
        <el-form :model="subcategoryForm" label-width="100px">
          <el-form-item label="子分类名称" required>
            <el-input v-model="subcategoryForm.name" placeholder="请输入子分类名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="subcategoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveSubcategory" :loading="loading">
            确定
          </el-button>
        </template>
      </el-dialog>

      <!-- 编辑资料对话框 -->
      <el-dialog
        v-model="editMaterialDialogVisible"
        title="编辑资料信息"
        width="600px"
      >
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="资料标题" required>
            <el-input v-model="uploadForm.title" placeholder="请输入资料标题" />
          </el-form-item>
          <el-form-item label="资料描述">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入资料描述"
            />
          </el-form-item>
          <el-form-item>
            <el-alert
              title="注意：修改仅限标题和描述，不可更改文件"
              type="info"
              :closable="false"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editMaterialDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveEditMaterial" :loading="loading">
            保存
          </el-button>
        </template>
      </el-dialog>

      <!-- 编辑子分类对话框 -->
      <el-dialog
        v-model="editSubcategoryDialogVisible"
        title="编辑子分类"
        width="500px"
      >
        <el-form :model="subcategoryForm" label-width="100px">
          <el-form-item label="子分类名称" required>
            <el-input v-model="subcategoryForm.name" placeholder="请输入子分类名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editSubcategoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveEditSubcategory" :loading="loading">
            保存
          </el-button>
        </template>
      </el-dialog>

      <!-- 编辑分类对话框 -->
      <el-dialog
        v-model="editCategoryDialogVisible"
        title="编辑分类"
        width="500px"
      >
        <el-form :model="categoryForm" label-width="100px">
          <el-form-item label="分类名称" required>
            <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editCategoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveEditCategory" :loading="loading">
            保存
          </el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.materials-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
}

.breadcrumb {
  margin-bottom: var(--spacing-xl);
  font-size: 16px;
}

.header-card {
  margin-bottom: var(--spacing-lg);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.category-card {
  transition: all 0.3s;
  position: relative;
  display: flex;
  flex-direction: column;
}

.category-content {
  cursor: pointer;
  flex: 1;
}

.card-actions {
  padding: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-xs);
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.category-content {
  text-align: center;
  padding: var(--spacing-xl);
}

.category-content h3 {
  margin: var(--spacing-md) 0;
  font-size: 18px;
  font-weight: 600;
}

.materials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
}

.material-card {
  display: flex;
  flex-direction: column;
}

.material-header {
  text-align: center;
  padding: var(--spacing-lg) 0;
  border-bottom: 1px solid var(--color-border);
}

.material-body {
  flex: 1;
  padding: var(--spacing-md);
}

.material-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-primary);
}

.material-description {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-bottom: var(--spacing-md);
  min-height: 40px;
}

.material-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-light);
  margin-bottom: var(--spacing-xs);
}

.material-footer {
  padding: var(--spacing-md);
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: var(--spacing-sm);
}

.download-link-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light, #f5f7fa);
  border-top: 1px solid var(--color-border);
}

.download-link-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
  font-weight: 500;
}

.download-link-content {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

.download-link-input {
  flex: 1;
}

.download-action-btn {
  flex-shrink: 0;
}

.hover-card {
  transition: all 0.3s ease;
}

.hover-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
