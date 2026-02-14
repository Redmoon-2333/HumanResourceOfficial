<template>
  <Layout>
    <div class="daily-image-management">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon :size="32"><PictureRounded /></el-icon>
          </div>
          <div class="header-text">
            <h1 class="page-title">日常活动图片管理</h1>
            <p class="page-subtitle">管理"我们的日常"板块轮播图</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button
            type="primary"
            @click="showAddDialog = true"
            :icon="Plus"
          >
            添加图片
          </el-button>
        </div>
      </div>

      <!-- 图片列表 -->
      <div class="images-list">
        <el-table
          :data="images"
          v-loading="loading"
          row-key="imageId"
          default-expand-all
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        >
          <el-table-column prop="imageId" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="150">
            <template #default="{ row }">
              <span v-if="row.title">{{ row.title }}</span>
              <span v-else class="text-muted">无标题</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="{ row }">
              <span v-if="row.description">{{ row.description }}</span>
              <span v-else class="text-muted">无描述</span>
            </template>
          </el-table-column>
          <el-table-column label="图片预览" width="120">
            <template #default="{ row }">
              <el-image
                :src="getFullImageUrl(row.imageUrl)"
                :preview-src-list="getFullImageUrlList([row.imageUrl])"
                fit="cover"
                style="width: 80px; height: 60px; border-radius: 4px;"
                :preview-teleported="true"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" sortable />
          <el-table-column prop="isActive" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">
                {{ row.isActive ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <button
                  class="action-btn edit-btn"
                  @click="editImage(row)"
                >
                  <el-icon><Edit /></el-icon>
                  <span>编辑</span>
                </button>
                <button
                  class="action-btn toggle-btn"
                  :class="{ 'enable-btn': !row.isActive }"
                  @click="toggleStatus(row)"
                >
                  <el-icon><component :is="row.isActive ? 'CircleClose' : 'CircleCheck'" /></el-icon>
                  <span>{{ row.isActive ? '禁用' : '启用' }}</span>
                </button>
                <button
                  class="action-btn delete-btn"
                  @click="deleteImage(row)"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 添加/编辑图片对话框 -->
      <el-dialog
        v-model="showAddDialog"
        :title="editingImage ? '编辑图片' : '添加图片'"
        width="600px"
        @close="resetForm"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
        >
          <!-- 图片上传区域 -->
          <el-form-item :label="editingImage ? '替换图片' : '上传图片'">
            <el-upload
              class="image-uploader"
              :auto-upload="false"
              :on-change="handleFileChange"
              :show-file-list="false"
              accept="image/*"
            >
              <!-- 编辑模式：显示当前图片或新上传的预览 -->
              <div v-if="editingImage && !uploadPreview" class="upload-preview current-image">
                <img :src="getFullImageUrl(form.imageUrl)" alt="当前图片" />
                <div class="upload-overlay">
                  <el-icon><Upload /></el-icon>
                  <span>点击上传新图片替换</span>
                </div>
              </div>
              <!-- 上传预览（新增或替换后） -->
              <div v-else-if="uploadPreview" class="upload-preview">
                <img :src="uploadPreview" alt="预览" />
                <div class="upload-overlay">
                  <el-icon><Upload /></el-icon>
                  <span>{{ editingImage ? '更换图片' : '重新选择' }}</span>
                </div>
              </div>
              <!-- 添加模式：空状态 -->
              <div v-else class="upload-placeholder">
                <el-icon :size="40"><Plus /></el-icon>
                <span>点击上传图片</span>
                <span class="upload-tip">支持 JPG、PNG、GIF 格式</span>
              </div>
            </el-upload>
            <!-- 文件信息 -->
            <div v-if="uploadFile" class="upload-file-info">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ uploadFile.name }}</span>
              <span class="file-size">{{ formatFileSize(uploadFile.size) }}</span>
              <el-button 
                v-if="editingImage" 
                type="primary" 
                link 
                size="small"
                @click.stop="cancelReplaceImage"
              >
                取消替换
              </el-button>
            </div>
            <!-- 编辑模式提示 -->
            <div v-if="editingImage && !uploadFile" class="edit-image-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>点击图片可上传新图片替换当前图片</span>
            </div>
            <el-progress
              v-if="uploadProgress > 0 && uploadProgress < 100"
              :percentage="uploadProgress"
              :stroke-width="6"
              class="upload-progress"
            />
          </el-form-item>

          <!-- 图片URL输入 - 在未选择上传文件时显示 -->
          <el-form-item v-if="!uploadFile" label="图片URL" prop="imageUrl">
            <el-input
              v-model="form.imageUrl"
              placeholder="请输入图片URL地址"
              clearable
            />
          </el-form-item>

          <el-form-item label="标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入图片标题"
              clearable
            />
          </el-form-item>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入图片描述"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="排序序号" prop="sortOrder">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="状态" prop="isActive">
            <el-switch
              v-model="form.isActive"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showAddDialog = false">取消</el-button>
            <el-button
              type="primary"
              @click="submitForm"
              :loading="submitting"
            >
              {{ editingImage ? '更新' : '添加' }}
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  PictureRounded,
  Plus,
  Picture,
  Edit,
  Delete,
  CircleClose,
  CircleCheck,
  Upload,
  Document,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  getAllImages,
  addImage,
  updateImage,
  deleteImage as deleteImageAPI,
  updateImageStatus,
  uploadDailyImage,
  uploadDailyImageFile,
  deleteDailyImageWithFile,
  deleteDailyImageFileOnly,
  type DailyImage
} from '@/api/dailyImage'
import { getFullImageUrl, getFullImageUrlList } from '@/utils/image'

// 状态管理
const loading = ref(false)
const submitting = ref(false)
const images = ref<DailyImage[]>([])
const showAddDialog = ref(false)
const editingImage = ref<DailyImage | null>(null)
const formRef = ref()

// 上传相关状态
const uploadFile = ref<File | null>(null)
const uploadPreview = ref<string>('')
const uploadProgress = ref<number>(0)

// 表单数据
const form = reactive({
  imageUrl: '',
  title: '',
  description: '',
  sortOrder: 0,
  isActive: true
})

// 表单验证规则
const rules = {
  imageUrl: [
    { required: true, message: '请输入图片URL', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        // 允许绝对URL (http://, https://) 或相对路径 (/uploads/...)
        if (!value || value.match(/^(https?:\/\/|\/)/)) {
          callback()
        } else {
          callback(new Error('请输入正确的URL格式（以http://、https://或/开头）'))
        }
      },
      trigger: 'blur'
    }
  ],
  title: [
    { max: 100, message: '标题长度不能超过100个字符', trigger: 'blur' }
  ],
  description: [
    { max: 255, message: '描述长度不能超过255个字符', trigger: 'blur' }
  ]
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

// 加载图片列表
const loadImages = async () => {
  loading.value = true
  try {
    const res = await getAllImages()
    if (res.code === 200) {
      images.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error: unknown) {
    console.error('加载图片列表失败:', error)
    const errMsg = error instanceof Error ? error.message : '加载失败'
    ElMessage.error(errMsg)
  } finally {
    loading.value = false
  }
}

// 编辑图片
const editImage = (image: DailyImage) => {
  editingImage.value = image
  form.imageUrl = image.imageUrl
  form.title = image.title || ''
  form.description = image.description || ''
  form.sortOrder = image.sortOrder || 0
  form.isActive = image.isActive || false
  showAddDialog.value = true
}

// 切换图片状态
const toggleStatus = async (image: DailyImage) => {
  try {
    await ElMessageBox.confirm(
      `确定要${image.isActive ? '禁用' : '启用'}这张图片吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await updateImageStatus(image.imageId, !image.isActive)
    if (res.code === 200) {
      ElMessage.success(`${image.isActive ? '禁用' : '启用'}成功`)
      loadImages()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      console.error('切换状态失败:', error)
      const errMsg = error instanceof Error ? error.message : '操作失败'
      ElMessage.error(errMsg)
    }
  }
}

// 删除图片
const deleteImage = async (image: DailyImage) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这张图片吗？此操作不可恢复',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    // 使用新的删除接口（同时删除本地文件）
    const res = await deleteDailyImageWithFile(image.imageId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadImages()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      console.error('删除图片失败:', error)
      const errMsg = error instanceof Error ? error.message : '删除失败'
      ElMessage.error(errMsg)
    }
  }
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    let res

    if (editingImage.value) {
      // 编辑模式
      if (uploadFile.value) {
        // 1. 先上传新图片文件（不创建数据库记录）
        const uploadRes = await uploadDailyImageFile(
          uploadFile.value,
          (percent) => {
            uploadProgress.value = percent
          }
        )

        if (uploadRes.code !== 200) {
          ElMessage.error(uploadRes.message || '图片上传失败')
          return
        }

        // 2. 删除旧图片文件（只删文件，不删记录）
        // Why: 使用 deleteDailyImageFileOnly 而不是 deleteDailyImageWithFile
        // deleteDailyImageWithFile 会删除数据库记录，导致后续 updateImage 失败
        if (editingImage.value.imageUrl) {
          try {
            await deleteDailyImageFileOnly(editingImage.value.imageId)
          } catch (e) {
            console.warn('删除旧图片文件失败:', e)
            // 继续执行，不阻断流程
          }
        }

        // 3. 更新数据库记录（使用新图片URL）
        const imageData = {
          imageUrl: uploadRes.data,
          title: form.title,
          description: form.description,
          sortOrder: form.sortOrder,
          isActive: form.isActive
        }
        res = await updateImage(editingImage.value.imageId, imageData)
      } else {
        // 仅更新信息，不上传新图片
        const imageData = {
          imageUrl: form.imageUrl,
          title: form.title,
          description: form.description,
          sortOrder: form.sortOrder,
          isActive: form.isActive
        }
        res = await updateImage(editingImage.value.imageId, imageData)
      }
    } else {
      // 添加模式 - 如果有上传文件则使用上传接口
      if (uploadFile.value) {
        // 使用上传接口
        res = await uploadDailyImage(
          uploadFile.value,
          form.title,
          form.description,
          (percent) => {
            uploadProgress.value = percent
          }
        )
      } else {
        // 使用原有接口（手动输入URL）
        const imageData = {
          imageUrl: form.imageUrl,
          title: form.title,
          description: form.description,
          sortOrder: form.sortOrder,
          isActive: form.isActive
        }
        res = await addImage(imageData)
      }
    }

    if (res.code === 200) {
      ElMessage.success(editingImage.value ? '更新成功' : '添加成功')
      showAddDialog.value = false
      loadImages()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: unknown) {
    console.error('提交表单失败:', error)
    if (error !== 'cancel') {
      const errMsg = error instanceof Error ? error.message : '操作失败'
      ElMessage.error(errMsg)
    }
  } finally {
    submitting.value = false
    uploadProgress.value = 0
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  form.imageUrl = ''
  form.title = ''
  form.description = ''
  form.sortOrder = 0
  form.isActive = true
  editingImage.value = null
  // 重置上传状态
  uploadFile.value = null
  uploadPreview.value = ''
  uploadProgress.value = 0
}

// 处理文件选择
const handleFileChange = (file: any) => {
  const rawFile = file.raw
  if (!rawFile) return

  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(rawFile.type)) {
    ElMessage.error('请上传 JPG、PNG、GIF 或 WebP 格式的图片')
    return
  }

  // 验证文件大小 (限制为 50MB)
  const maxSize = 50 * 1024 * 1024
  if (rawFile.size > maxSize) {
    ElMessage.error('图片大小不能超过 50MB')
    return
  }

  uploadFile.value = rawFile

  // 生成预览
  const reader = new FileReader()
  reader.onload = (e) => {
    uploadPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(rawFile)

  // 自动填充标题（使用文件名，去掉扩展名）
  if (!form.title) {
    const fileName = rawFile.name.replace(/\.[^/.]+$/, '')
    form.title = fileName
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 取消替换图片
const cancelReplaceImage = () => {
  uploadFile.value = null
  uploadPreview.value = ''
  uploadProgress.value = 0
}

// 页面加载时获取数据
onMounted(() => {
  loadImages()
})
</script>

<style scoped>
.daily-image-management {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffece6 100%);
  border-radius: 16px;
  border: 1px solid #ffd6cc;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #ff6b4a, #e35532);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.3);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1c1917;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #78716c;
  margin: 0;
}

.images-list {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

.text-muted {
  color: #9ca3af;
  font-style: italic;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 操作按钮组 - 高对比度设计 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

/* 编辑按钮 - 蓝色系 */
.edit-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.edit-btn:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 禁用按钮 - 橙色系 */
.toggle-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.toggle-btn:hover {
  background: linear-gradient(135deg, #d97706, #b45309);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

/* 启用按钮 - 绿色系 */
.enable-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.enable-btn:hover {
  background: linear-gradient(135deg, #059669, #047857);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

/* 删除按钮 - 红色系 */
.delete-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.delete-btn:hover {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

/* 按钮点击效果 */
.action-btn:active {
  transform: translateY(0) scale(0.96);
}

/* 按钮图标 */
.action-btn .el-icon {
  font-size: 14px;
}

/* 图片上传区域样式 */
.image-uploader {
  width: 100%;
}

.image-uploader :deep(.el-upload) {
  width: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 180px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #8c939d;
  gap: 8px;
}

.upload-placeholder:hover {
  border-color: #ff6b4a;
  color: #ff6b4a;
  background: rgba(255, 107, 74, 0.05);
}

.upload-tip {
  font-size: 12px;
  color: #a8a29e;
}

.upload-preview {
  width: 100%;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s ease;
  gap: 4px;
}

.upload-preview:hover .upload-overlay {
  opacity: 1;
}

.upload-file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 6px;
  font-size: 13px;
  color: #57534e;
}

.upload-file-info .file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-file-info .file-size {
  color: #a8a29e;
  font-size: 12px;
}

.upload-progress {
  margin-top: 12px;
}

/* 当前图片样式 */
.current-image {
  border: 2px solid #e5e7eb;
}

.current-image img {
  opacity: 0.9;
}

/* 编辑模式提示 */
.edit-image-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 12px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 6px;
  font-size: 13px;
  color: #3b82f6;
}

.edit-image-tip .el-icon {
  font-size: 14px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .action-buttons {
    flex-wrap: wrap;
    gap: 6px;
  }

  .action-btn {
    padding: 5px 10px;
    font-size: 12px;
  }

  .action-btn span {
    display: none;
  }

  .action-btn .el-icon {
    font-size: 16px;
  }
}
</style>
