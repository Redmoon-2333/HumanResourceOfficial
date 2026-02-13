<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted } from 'vue'
import {
  getCategories,
  getSubcategories,
  getMaterialsByCategory,
  getMaterialsBySubcategory,
  getMaterialById,
  searchMaterials,
  uploadMaterial,
  updateMaterial,
  deleteMaterial,
  getDownloadUrl,
  createCategory,
  updateCategory,
  createSubcategory,
  updateSubcategory
} from '@/api/materials'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder,
  Document,
  Download,
  ArrowRight,
  Grid,
  List,
  Search,
  DocumentCopy,
  DocumentChecked,
  Picture,
  Box,
  Clock,
  Star,
  Tickets,
  Collection,
  Files,
  Plus,
  Edit,
  Delete,
  Setting,
  Upload,
  Close
} from '@element-plus/icons-vue'
import type { CategoryResponse, SubcategoryResponse, MaterialResponse, MaterialRequest, CategoryRequest, SubcategoryRequest } from '@/types'
import { usePermission } from '@/composables/usePermission'

interface Category extends CategoryResponse {
  icon?: string
  color?: string
  bg?: string
}

interface Subcategory extends SubcategoryResponse {
  icon?: any
  color?: string
}

interface Material extends MaterialResponse {}

const { isMinister, isLoggedIn } = usePermission()

const categories = ref<Category[]>([])
const subcategories = ref<Subcategory[]>([])
const materials = ref<Material[]>([])
const currentCategory = ref<number | null>(null)
const currentSubcategory = ref<number | null>(null)
const loading = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const searchQuery = ref('')

const showUploadDialog = ref(false)
const showEditDialog = ref(false)
const showCategoryDialog = ref(false)
const showSubcategoryDialog = ref(false)
const uploadProgress = ref(0)
const uploading = ref(false)

const uploadForm = ref({
  file: null as File | null,
  materialName: '',
  description: '',
  categoryId: null as number | null,
  subcategoryId: null as number | null
})

const editForm = ref<MaterialRequest>({
  materialName: '',
  description: '',
  categoryId: undefined,
  subcategoryId: undefined
})

const categoryForm = ref<CategoryRequest>({
  categoryName: '',
  sortOrder: 0
})

const subcategoryForm = ref<SubcategoryRequest>({
  categoryId: 0,
  subcategoryName: '',
  sortOrder: 0
})

const editingMaterialId = ref<number | null>(null)
const editingCategoryId = ref<number | null>(null)
const editingSubcategoryId = ref<number | null>(null)
const isEditMode = ref(false)

const fileTypeIcons: Record<string, any> = {
  'pdf': Tickets,
  'doc': DocumentCopy,
  'docx': DocumentCopy,
  'xls': DocumentChecked,
  'xlsx': DocumentChecked,
  'ppt': DocumentCopy,
  'pptx': DocumentCopy,
  'jpg': Picture,
  'jpeg': Picture,
  'png': Picture,
  'gif': Picture,
  'zip': Box,
  'rar': Box,
  '7z': Box,
  'default': Document
}

const fileTypeColors: Record<string, string> = {
  'pdf': '#EF4444',
  'doc': '#3B82F6',
  'docx': '#3B82F6',
  'xls': '#10B981',
  'xlsx': '#10B981',
  'ppt': '#F59E0B',
  'pptx': '#F59E0B',
  'jpg': '#8B5CF6',
  'jpeg': '#8B5CF6',
  'png': '#8B5CF6',
  'zip': '#6B7280'
}

const categoryColors = [
  { bg: 'linear-gradient(135deg, #FF6B4A, #E35532)', icon: '#FF6B4A' },
  { bg: 'linear-gradient(135deg, #F59E0B, #D97706)', icon: '#F59E0B' },
  { bg: 'linear-gradient(135deg, #10B981, #059669)', icon: '#10B981' },
  { bg: 'linear-gradient(135deg, #3B82F6, #2563EB)', icon: '#3B82F6' },
  { bg: 'linear-gradient(135deg, #8B5CF6, #7C3AED)', icon: '#8B5CF6' },
  { bg: 'linear-gradient(135deg, #EC4899, #DB2777)', icon: '#EC4899' }
]

const fetchCategories = async () => {
  try {
    const response = await getCategories()
    if (response.code === 200) {
      const data = (response.data || []) as CategoryResponse[]
      categories.value = data.map((cat, index) => ({
        ...cat,
        ...categoryColors[index % categoryColors.length]
      }))
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取分类失败')
  }
}

const fetchSubcategories = async (categoryId: number) => {
  try {
    const response = await getSubcategories(categoryId)
    if (response.code === 200) {
      const data = (response.data || []) as SubcategoryResponse[]
      subcategories.value = data.map((sub, index) => ({
        ...sub,
        icon: Files,
        color: categoryColors[index % categoryColors.length]?.icon || '#FF6B4A'
      }))
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取子分类失败')
  }
}

const fetchMaterials = async () => {
  loading.value = true
  try {
    let response
    if (currentSubcategory.value) {
      response = await getMaterialsBySubcategory(currentSubcategory.value)
    } else if (currentCategory.value) {
      response = await getMaterialsByCategory(currentCategory.value)
    } else {
      response = await getMaterialsByCategory()
    }
    
    if (response.code === 200) {
      materials.value = (response.data || []) as Material[]
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取资料失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    await fetchMaterials()
    return
  }
  
  loading.value = true
  try {
    const response = await searchMaterials(searchQuery.value.trim())
    if (response.code === 200) {
      materials.value = (response.data || []) as Material[]
    }
  } catch (error: any) {
    ElMessage.error(error.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

const handleCategoryClick = async (categoryId?: number) => {
  currentCategory.value = categoryId ?? null
  currentSubcategory.value = null
  
  if (categoryId) {
    await fetchSubcategories(categoryId)
  } else {
    subcategories.value = []
  }
  
  await fetchMaterials()
}

const handleSubcategoryClick = async (subcategoryId: number) => {
  currentSubcategory.value = subcategoryId
  await fetchMaterials()
}

const handleDownload = async (material: Material) => {
  try {
    const response = await getDownloadUrl(material.materialId)
    if (response.code === 200) {
      window.open(response.data as string, '_blank')
      ElMessage.success('开始下载')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '下载失败')
  }
}

const formatFileSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const getFileIcon = (fileType: string) => {
  const type = fileType?.toLowerCase().replace(/^\./, '') || 'default'
  return fileTypeIcons[type] || fileTypeIcons['default']
}

const getFileColor = (fileType: string) => {
  const type = fileType?.toLowerCase().replace(/^\./, '') || 'default'
  return fileTypeColors[type] || '#9CA3AF'
}

const filteredMaterials = computed(() => {
  if (!searchQuery.value) return materials.value
  const query = searchQuery.value.toLowerCase()
  return materials.value.filter(m => 
    m.materialName.toLowerCase().includes(query)
  )
})

const totalMaterials = computed(() => 
  materials.value.length
)

const totalDownloads = computed(() =>
  materials.value.reduce((sum, m) => sum + (m.downloadCount || 0), 0)
)

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    uploadForm.value.file = file
    if (!uploadForm.value.materialName) {
      uploadForm.value.materialName = file.name.replace(/\.[^/.]+$/, '')
    }
  }
}

const handleUploadSubmit = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!uploadForm.value.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (!uploadForm.value.subcategoryId) {
    ElMessage.warning('请选择子分类')
    return
  }
  if (!uploadForm.value.materialName.trim()) {
    ElMessage.warning('请输入资料名称')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  
  try {
    const response = await uploadMaterial(
      uploadForm.value.file,
      uploadForm.value.categoryId,
      uploadForm.value.subcategoryId,
      uploadForm.value.materialName.trim(),
      uploadForm.value.description.trim() || undefined,
      (percent) => {
        uploadProgress.value = percent
      }
    )
    
    if (response.code === 200) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      resetUploadForm()
      await fetchMaterials()
    } else {
      ElMessage.error(response.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const resetUploadForm = () => {
  uploadForm.value = {
    file: null,
    materialName: '',
    description: '',
    categoryId: null,
    subcategoryId: null
  }
  uploadProgress.value = 0
}

const openEditDialog = async (material: Material) => {
  editingMaterialId.value = material.materialId
  editForm.value = {
    materialName: material.materialName,
    description: material.description || '',
    categoryId: material.categoryId,
    subcategoryId: material.subcategoryId
  }
  if (material.categoryId) {
    await fetchSubcategories(material.categoryId)
  }
  showEditDialog.value = true
}

const handleEditSubmit = async () => {
  if (!editingMaterialId.value) return
  
  try {
    const response = await updateMaterial(editingMaterialId.value, editForm.value)
    if (response.code === 200) {
      ElMessage.success('更新成功')
      showEditDialog.value = false
      await fetchMaterials()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  }
}

const handleDelete = async (material: Material) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资料「${material.materialName}」吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await deleteMaterial(material.materialId)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      await fetchMaterials()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const openCategoryDialog = (category?: Category) => {
  if (category) {
    isEditMode.value = true
    editingCategoryId.value = category.categoryId
    categoryForm.value = {
      categoryName: category.categoryName,
      sortOrder: category.sortOrder
    }
  } else {
    isEditMode.value = false
    editingCategoryId.value = null
    categoryForm.value = {
      categoryName: '',
      sortOrder: 0
    }
  }
  showCategoryDialog.value = true
}

const handleCategorySubmit = async () => {
  if (!categoryForm.value.categoryName.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  
  try {
    let response
    if (isEditMode.value && editingCategoryId.value) {
      response = await updateCategory(editingCategoryId.value, categoryForm.value)
    } else {
      response = await createCategory(categoryForm.value)
    }
    
    if (response.code === 200) {
      ElMessage.success(isEditMode.value ? '更新成功' : '创建成功')
      showCategoryDialog.value = false
      await fetchCategories()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const openSubcategoryDialog = (categoryId: number, subcategory?: Subcategory) => {
  if (subcategory) {
    isEditMode.value = true
    editingSubcategoryId.value = subcategory.subcategoryId
    subcategoryForm.value = {
      categoryId: categoryId,
      subcategoryName: subcategory.subcategoryName,
      sortOrder: subcategory.sortOrder
    }
  } else {
    isEditMode.value = false
    editingSubcategoryId.value = null
    subcategoryForm.value = {
      categoryId: categoryId,
      subcategoryName: '',
      sortOrder: 0
    }
  }
  showSubcategoryDialog.value = true
}

const handleSubcategorySubmit = async () => {
  if (!subcategoryForm.value.subcategoryName.trim()) {
    ElMessage.warning('请输入子分类名称')
    return
  }
  
  try {
    let response
    if (isEditMode.value && editingSubcategoryId.value) {
      response = await updateSubcategory(editingSubcategoryId.value, subcategoryForm.value)
    } else {
      response = await createSubcategory(subcategoryForm.value)
    }
    
    if (response.code === 200) {
      ElMessage.success(isEditMode.value ? '更新成功' : '创建成功')
      showSubcategoryDialog.value = false
      if (subcategoryForm.value.categoryId) {
        await fetchSubcategories(subcategoryForm.value.categoryId)
      }
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const onCategoryChange = async () => {
  uploadForm.value.subcategoryId = null
  if (uploadForm.value.categoryId) {
    await fetchSubcategories(uploadForm.value.categoryId)
  }
}

onMounted(() => {
  fetchCategories()
  fetchMaterials()
})
</script>

<template>
  <Layout>
    <div class="materials-page">
      <!-- Hero区域 -->
      <div class="hero-section">
        <div class="hero-background">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="floating-shapes">
            <div class="shape shape-1">
              <el-icon :size="40" color="rgba(255, 107, 74, 0.3)"><Folder /></el-icon>
            </div>
            <div class="shape shape-2">
              <el-icon :size="32" color="rgba(245, 158, 11, 0.3)"><Document /></el-icon>
            </div>
            <div class="shape shape-3">
              <el-icon :size="36" color="rgba(16, 185, 129, 0.3)"><Collection /></el-icon>
            </div>
          </div>
        </div>

        <div class="hero-content">
          <div class="hero-badge">
            <el-icon :size="16"><Folder /></el-icon>
            <span>资料中心</span>
          </div>
          <!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
          <h1 class="hero-title" style="color: #1C1917;">
            共享知识
            <span style="color: #000000;">共创价值</span>
          </h1>
          <!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
          <p class="hero-subtitle" style="color: #78716C;">
            汇集人力资源中心各类文档资料，助力成员成长与协作
          </p>

          <!-- 统计卡片 -->
          <div class="stats-row">
            <div class="stat-item">
              <div class="stat-icon" style="background: linear-gradient(135deg, #FF6B4A, #E35532);">
                <el-icon :size="20" color="white"><Folder /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-value">
                  <AnimatedCounter :value="totalMaterials" :duration="1500" />
                </span>
                <span class="stat-label">资料总数</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background: linear-gradient(135deg, #F59E0B, #D97706);">
                <el-icon :size="20" color="white"><Grid /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-value">
                  <AnimatedCounter :value="categories.length" :duration="1500" />
                </span>
                <span class="stat-label">分类数量</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background: linear-gradient(135deg, #10B981, #059669);">
                <el-icon :size="20" color="white"><Download /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-value">
                  <AnimatedCounter :value="totalDownloads" :duration="1500" />
                </span>
                <span class="stat-label">下载次数</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 分类侧边栏 -->
        <div class="category-sidebar">
          <GlassPanel :blur="15" :opacity="0.95" :border-radius="20">
            <div class="sidebar-header">
              <h3 class="sidebar-title">
                <el-icon :size="20"><Folder /></el-icon>
                <span>资料分类</span>
              </h3>
              <button
                v-if="isMinister"
                class="add-category-btn"
                @click="openCategoryDialog()"
                title="添加分类"
              >
                <el-icon :size="16"><Plus /></el-icon>
              </button>
            </div>

            <!-- 一级分类 -->
            <div class="category-list">
              <div
                class="category-item"
                :class="{ active: currentCategory === null }"
                @click="handleCategoryClick()"
              >
                <div class="category-icon" style="background: linear-gradient(135deg, #6B7280, #4B5563);">
                  <el-icon :size="18" color="white"><Folder /></el-icon>
                </div>
                <div class="category-info">
                  <span class="category-name">全部资料</span>
                </div>
                <el-icon :size="16" class="arrow-icon"><ArrowRight /></el-icon>
              </div>

              <div
                v-for="category in categories"
                :key="category.categoryId"
                class="category-item"
                :class="{ active: currentCategory === category.categoryId }"
                @click="handleCategoryClick(category.categoryId)"
              >
                <div class="category-icon" :style="{ background: category.bg }">
                  <el-icon :size="18" color="white"><Folder /></el-icon>
                </div>
                <div class="category-info">
                  <span class="category-name">{{ category.categoryName }}</span>
                </div>
                <div v-if="isMinister" class="category-actions" @click.stop>
                  <button class="action-btn" @click="openCategoryDialog(category)" title="编辑分类">
                    <el-icon :size="14"><Edit /></el-icon>
                  </button>
                  <button class="action-btn" @click="openSubcategoryDialog(category.categoryId)" title="添加子分类">
                    <el-icon :size="14"><Plus /></el-icon>
                  </button>
                </div>
                <el-icon v-else :size="16" class="arrow-icon"><ArrowRight /></el-icon>
              </div>
            </div>

            <!-- 二级分类 -->
            <div v-if="subcategories.length > 0" class="subcategory-section">
              <div class="subcategory-divider"></div>
              <h4 class="subcategory-title">
                <el-icon :size="16"><Files /></el-icon>
                <span>子分类</span>
              </h4>
              <div class="subcategory-list">
                <div
                  class="subcategory-item"
                  :class="{ active: currentSubcategory === null && currentCategory !== null }"
                  @click="currentSubcategory = null; fetchMaterials()"
                >
                  <span class="subcategory-name">全部</span>
                </div>
                <div
                  v-for="subcategory in subcategories"
                  :key="subcategory.subcategoryId"
                  class="subcategory-item"
                  :class="{ active: currentSubcategory === subcategory.subcategoryId }"
                  @click="handleSubcategoryClick(subcategory.subcategoryId)"
                >
                  <span class="subcategory-name">{{ subcategory.subcategoryName }}</span>
                  <div v-if="isMinister" class="subcategory-actions" @click.stop>
                    <button class="action-btn small" @click="openSubcategoryDialog(subcategory.categoryId, subcategory)" title="编辑">
                      <el-icon :size="12"><Edit /></el-icon>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </GlassPanel>
        </div>

        <!-- 资料列表区 -->
        <div class="materials-content">
          <!-- 工具栏 -->
          <div class="toolbar">
            <div class="search-box">
              <el-icon :size="18" class="search-icon"><Search /></el-icon>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索资料..."
                class="search-input"
                @keyup.enter="handleSearch"
              />
            </div>

            <div class="toolbar-actions">
              <button
                v-if="isMinister"
                class="upload-btn"
                @click="showUploadDialog = true"
              >
                <el-icon :size="18"><Upload /></el-icon>
                <span>上传资料</span>
              </button>

              <div class="view-toggle">
                <button
                  class="toggle-btn"
                  :class="{ active: viewMode === 'grid' }"
                  @click="viewMode = 'grid'"
                >
                  <el-icon :size="18"><Grid /></el-icon>
                </button>
                <button
                  class="toggle-btn"
                  :class="{ active: viewMode === 'list' }"
                  @click="viewMode = 'list'"
                >
                  <el-icon :size="18"><List /></el-icon>
                </button>
              </div>
            </div>
          </div>

          <!-- 资料列表 -->
          <div v-loading="loading" class="materials-list" :class="viewMode">
            <div
              v-for="material in filteredMaterials"
              :key="material.materialId"
              class="material-card"
            >
              <div class="material-icon" :style="{ background: getFileColor(material.fileType) + '20' }">
                <el-icon :size="32" :color="getFileColor(material.fileType)">
                  <component :is="getFileIcon(material.fileType)" />
                </el-icon>
              </div>

              <div class="material-info">
                <h4 class="material-name">{{ material.materialName }}</h4>
                <p v-if="material.description" class="material-desc">{{ material.description }}</p>
                <div class="material-meta">
                  <span class="meta-item">
                    <el-icon :size="14"><Clock /></el-icon>
                    {{ new Date(material.uploadTime).toLocaleDateString() }}
                  </span>
                  <span class="meta-item">
                    <el-icon :size="14"><Download /></el-icon>
                    {{ material.downloadCount }} 次下载
                  </span>
                  <span v-if="material.categoryName" class="meta-item category-tag">
                    {{ material.categoryName }}
                  </span>
                  <span v-if="material.subcategoryName" class="meta-item subcategory-tag">
                    {{ material.subcategoryName }}
                  </span>
                </div>
              </div>

              <div class="material-actions">
                <span class="file-size">{{ formatFileSize(material.fileSize) }}</span>
                <div class="action-buttons">
                  <button class="download-btn" @click="handleDownload(material)">
                    <el-icon :size="18"><Download /></el-icon>
                    <span>下载</span>
                  </button>
                  <template v-if="isMinister">
                    <button class="edit-btn" @click="openEditDialog(material)" title="编辑">
                      <el-icon :size="16"><Edit /></el-icon>
                    </button>
                    <button class="delete-btn" @click="handleDelete(material)" title="删除">
                      <el-icon :size="16"><Delete /></el-icon>
                    </button>
                  </template>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredMaterials.length === 0 && !loading" class="empty-state">
              <el-icon :size="64" color="#D1D5DB"><Folder /></el-icon>
              <p>暂无资料</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 上传资料对话框 -->
      <el-dialog
        v-model="showUploadDialog"
        title="上传资料"
        width="500px"
        :close-on-click-modal="false"
        @closed="resetUploadForm"
      >
        <div class="upload-form">
          <div class="form-group">
            <label>选择文件 <span class="required">*</span></label>
            <input
              type="file"
              class="file-input"
              @change="handleFileChange"
            />
            <p v-if="uploadForm.file" class="file-name">{{ uploadForm.file.name }}</p>
          </div>

          <div class="form-group">
            <label>资料名称 <span class="required">*</span></label>
            <input
              v-model="uploadForm.materialName"
              type="text"
              placeholder="请输入资料名称"
              class="text-input"
            />
          </div>

          <div class="form-group">
            <label>分类 <span class="required">*</span></label>
            <select v-model="uploadForm.categoryId" class="select-input" @change="onCategoryChange">
              <option :value="null">请选择分类</option>
              <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                {{ cat.categoryName }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>子分类 <span class="required">*</span></label>
            <select v-model="uploadForm.subcategoryId" class="select-input">
              <option :value="null">请选择子分类</option>
              <option v-for="sub in subcategories" :key="sub.subcategoryId" :value="sub.subcategoryId">
                {{ sub.subcategoryName }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>描述</label>
            <textarea
              v-model="uploadForm.description"
              placeholder="请输入资料描述（可选）"
              class="textarea-input"
              rows="3"
            ></textarea>
          </div>

          <div v-if="uploading" class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            <span class="progress-text">{{ uploadProgress }}%</span>
          </div>
        </div>

        <template #footer>
          <button class="cancel-btn" @click="showUploadDialog = false">取消</button>
          <button class="submit-btn" :disabled="uploading" @click="handleUploadSubmit">
            {{ uploading ? '上传中...' : '确认上传' }}
          </button>
        </template>
      </el-dialog>

      <!-- 编辑资料对话框 -->
      <el-dialog
        v-model="showEditDialog"
        title="编辑资料"
        width="500px"
        :close-on-click-modal="false"
      >
        <div class="upload-form">
          <div class="form-group">
            <label>资料名称 <span class="required">*</span></label>
            <input
              v-model="editForm.materialName"
              type="text"
              placeholder="请输入资料名称"
              class="text-input"
            />
          </div>

          <div class="form-group">
            <label>分类</label>
            <select v-model="editForm.categoryId" class="select-input" @change="editForm.subcategoryId = undefined">
              <option :value="undefined">请选择分类</option>
              <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                {{ cat.categoryName }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>子分类</label>
            <select v-model="editForm.subcategoryId" class="select-input">
              <option :value="undefined">请选择子分类</option>
              <option v-for="sub in subcategories" :key="sub.subcategoryId" :value="sub.subcategoryId">
                {{ sub.subcategoryName }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>描述</label>
            <textarea
              v-model="editForm.description"
              placeholder="请输入资料描述（可选）"
              class="textarea-input"
              rows="3"
            ></textarea>
          </div>
        </div>

        <template #footer>
          <button class="cancel-btn" @click="showEditDialog = false">取消</button>
          <button class="submit-btn" @click="handleEditSubmit">保存修改</button>
        </template>
      </el-dialog>

      <!-- 分类管理对话框 -->
      <el-dialog
        v-model="showCategoryDialog"
        :title="isEditMode ? '编辑分类' : '添加分类'"
        width="400px"
        :close-on-click-modal="false"
      >
        <div class="upload-form">
          <div class="form-group">
            <label>分类名称 <span class="required">*</span></label>
            <input
              v-model="categoryForm.categoryName"
              type="text"
              placeholder="请输入分类名称"
              class="text-input"
            />
          </div>
          <div class="form-group">
            <label>排序</label>
            <input
              v-model.number="categoryForm.sortOrder"
              type="number"
              placeholder="数字越小越靠前"
              class="text-input"
            />
          </div>
        </div>

        <template #footer>
          <button class="cancel-btn" @click="showCategoryDialog = false">取消</button>
          <button class="submit-btn" @click="handleCategorySubmit">
            {{ isEditMode ? '保存修改' : '创建分类' }}
          </button>
        </template>
      </el-dialog>

      <!-- 子分类管理对话框 -->
      <el-dialog
        v-model="showSubcategoryDialog"
        :title="isEditMode ? '编辑子分类' : '添加子分类'"
        width="400px"
        :close-on-click-modal="false"
      >
        <div class="upload-form">
          <div class="form-group">
            <label>子分类名称 <span class="required">*</span></label>
            <input
              v-model="subcategoryForm.subcategoryName"
              type="text"
              placeholder="请输入子分类名称"
              class="text-input"
            />
          </div>
          <div class="form-group">
            <label>排序</label>
            <input
              v-model.number="subcategoryForm.sortOrder"
              type="number"
              placeholder="数字越小越靠前"
              class="text-input"
            />
          </div>
        </div>

        <template #footer>
          <button class="cancel-btn" @click="showSubcategoryDialog = false">取消</button>
          <button class="submit-btn" @click="handleSubcategorySubmit">
            {{ isEditMode ? '保存修改' : '创建子分类' }}
          </button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.materials-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #FEF9F6 0%, #FFF5F0 100%);
}

/* Hero区域 */
.hero-section {
  position: relative;
  padding: 80px 40px 60px;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  top: -100px;
  right: 10%;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #FB7185, #FF6B4A);
  bottom: -50px;
  left: 5%;
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: rgba(255, 107, 74, 0.1);
  border: 1px solid rgba(255, 107, 74, 0.2);
  border-radius: 50px;
  color: #FF6B4A;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  color: #1F2937;
  margin-bottom: 16px;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 18px;
  color: #6B7280;
  max-width: 500px;
  margin: 0 auto 40px;
  line-height: 1.6;
}

/* 统计 */
.stats-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
}

/* 主内容区 */
.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px 60px;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 32px;
}

/* 侧边栏 */
.category-sidebar {
  position: sticky;
  top: 100px;
  height: fit-content;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

.add-category-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.add-category-btn:hover {
  transform: scale(1.1);
}

/* 一级分类 */
.category-list {
  padding: 12px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 4px;
}

.category-item:hover {
  background: rgba(255, 107, 74, 0.05);
}

.category-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.1), rgba(245, 158, 11, 0.1));
  border: 1px solid rgba(255, 107, 74, 0.2);
}

.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.category-name {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.category-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  color: #6B7280;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 107, 74, 0.1);
  color: #FF6B4A;
}

.action-btn.small {
  width: 24px;
  height: 24px;
}

.arrow-icon {
  color: #D1D5DB;
  transition: all 0.3s ease;
}

.category-item:hover .arrow-icon,
.category-item.active .arrow-icon {
  color: #FF6B4A;
  transform: translateX(4px);
}

/* 二级分类 */
.subcategory-section {
  padding: 0 12px 12px;
}

.subcategory-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.1);
  margin: 12px 0;
}

.subcategory-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6B7280;
  margin: 0 0 12px 4px;
}

.subcategory-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.subcategory-item {
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #6B7280;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.subcategory-item:hover {
  background: rgba(255, 107, 74, 0.05);
  color: #FF6B4A;
}

.subcategory-item.active {
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
}

.subcategory-actions {
  display: flex;
  gap: 4px;
}

/* 资料内容区 */
.materials-content {
  min-height: 600px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #9CA3AF;
}

.search-input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 2px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  font-size: 15px;
  background: white;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #FF6B4A;
  box-shadow: 0 0 0 4px rgba(255, 107, 74, 0.1);
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.4);
}

.view-toggle {
  display: flex;
  gap: 8px;
  background: white;
  padding: 4px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.toggle-btn {
  padding: 10px 14px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #9CA3AF;
  transition: all 0.3s ease;
}

.toggle-btn:hover {
  color: #FF6B4A;
}

.toggle-btn.active {
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
}

/* 资料列表 */
.materials-list {
  display: grid;
  gap: 16px;
}

.materials-list.grid {
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
}

.materials-list.list {
  grid-template-columns: 1fr;
}

.material-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.material-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  border-color: rgba(255, 107, 74, 0.2);
}

.material-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.material-desc {
  font-size: 13px;
  color: #6B7280;
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.material-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9CA3AF;
}

.meta-item.category-tag {
  padding: 2px 8px;
  background: rgba(255, 107, 74, 0.1);
  color: #FF6B4A;
  border-radius: 4px;
  font-weight: 500;
}

.meta-item.subcategory-tag {
  padding: 2px 8px;
  background: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
  border-radius: 4px;
  font-weight: 500;
}

.material-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.file-size {
  font-size: 13px;
  color: #9CA3AF;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.download-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.download-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.4);
}

.edit-btn,
.delete-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.edit-btn {
  background: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
}

.edit-btn:hover {
  background: #3B82F6;
  color: white;
}

.delete-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
}

.delete-btn:hover {
  background: #EF4444;
  color: white;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 40px;
  color: #9CA3AF;
}

.empty-state p {
  margin-top: 16px;
  font-size: 16px;
}

/* 对话框样式 */
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.required {
  color: #EF4444;
}

.text-input,
.select-input,
.textarea-input {
  padding: 12px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.text-input:focus,
.select-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: #FF6B4A;
  box-shadow: 0 0 0 3px rgba(255, 107, 74, 0.1);
}

.file-input {
  padding: 12px;
  border: 2px dashed #E5E7EB;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.file-input:hover {
  border-color: #FF6B4A;
}

.file-name {
  font-size: 13px;
  color: #6B7280;
  margin: 4px 0 0;
}

.progress-bar {
  position: relative;
  height: 24px;
  background: #F3F4F6;
  border-radius: 12px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  transition: width 0.3s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.cancel-btn {
  padding: 10px 20px;
  background: #F3F4F6;
  color: #6B7280;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: #E5E7EB;
}

.submit-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .category-sidebar {
    position: static;
  }

  .category-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .category-item {
    flex: 1;
    min-width: 200px;
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 20px 40px;
  }

  .hero-title {
    font-size: 32px;
  }

  .main-content {
    padding: 0 20px 40px;
  }

  .stats-row {
    gap: 16px;
  }

  .stat-item {
    padding: 16px 20px;
  }

  .material-card {
    flex-direction: column;
    text-align: center;
  }

  .material-actions {
    align-items: center;
    width: 100%;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    max-width: none;
  }

  .toolbar-actions {
    justify-content: space-between;
  }
}
</style>
