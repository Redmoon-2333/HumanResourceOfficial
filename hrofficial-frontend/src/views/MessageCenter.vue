<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getMessages, getUnreadCount, markAsRead, markAllAsRead, deleteMessage, sendMessage } from '@/api/messages'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Delete, Check, ChatDotRound, Plus, EditPen } from '@element-plus/icons-vue'
import { useMessageStore } from '@/stores/message'
import type { Message } from '@/types'
import { getUsers } from '@/api/roles'
import { useUserStore } from '@/stores/user'

const messageStore = useMessageStore()
const userStore = useUserStore()
const loading = ref(false)
const messages = ref<Message[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('all')
const selectedMessage = ref<Message | null>(null)

const showComposeDialog = ref(false)
const composeForm = ref({ receiverId: null as number | null, title: '', content: '' })
const composing = ref(false)
const receiverOptions = ref<{ label: string; value: number }[]>([])

const typeLabels: Record<string, string> = {
  TASK_ASSIGNED: '任务分配',
  TASK_REMIND: '任务催促',
  TASK_COMPLETED: '任务完成',
  ROLE_CHANGED: '身份变更',
  SYSTEM: '系统通知'
}

const filteredMessages = computed(() => {
  if (activeTab.value === 'unread') return messages.value.filter(m => !m.isRead)
  if (activeTab.value === 'task') return messages.value.filter(m => m.type?.startsWith('TASK_'))
  if (activeTab.value === 'system') return messages.value.filter(m => m.type === 'SYSTEM' || m.type === 'ROLE_CHANGED')
  return messages.value
})

async function fetchMessages() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, pageSize: pageSize.value }
    if (activeTab.value === 'unread') params.isRead = false
    const res = await getMessages(params)
    if ((res.code === 0 || res.code === 200) && res.data) {
      messages.value = res.data.records
      total.value = res.data.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取站内信失败')
  } finally {
    loading.value = false
  }
}

async function handleRead(msg: Message) {
  if (!msg.isRead) {
    try {
      await markAsRead(msg.messageId)
      msg.isRead = true
      messageStore.fetchUnreadCount()
    } catch { /* ignore */ }
  }
  selectedMessage.value = msg
}

async function handleReadAll() {
  try {
    await markAllAsRead()
    messages.value.forEach(m => { m.isRead = true })
    messageStore.fetchUnreadCount()
    ElMessage.success('已全部标记为已读')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除此消息？', '删除确认', { type: 'warning' })
    await deleteMessage(id)
    ElMessage.success('已删除')
    if (selectedMessage.value?.messageId === id) selectedMessage.value = null
    fetchMessages()
    messageStore.fetchUnreadCount()
  } catch { /* cancel */ }
}

async function openCompose() {
  showComposeDialog.value = true
  composeForm.value = { receiverId: null, title: '', content: '' }
  receiverOptions.value = []
  try {
    const res = await getUsers({ page: 1, pageSize: 100 })
    if ((res.code === 0 || res.code === 200) && res.data?.records) {
      receiverOptions.value = res.data.records
        .filter((u: any) => u.userId !== userStore.userInfo?.userId)
        .map((u: any) => ({ label: `${u.name || u.username} (${u.username})`, value: u.userId }))
    }
  } catch {
    ElMessage.error('获取用户列表失败')
  }
}

async function handleSend() {
  if (!composeForm.value.receiverId) {
    ElMessage.error('请选择收件人')
    return
  }
  if (!composeForm.value.title.trim()) {
    ElMessage.error('请输入标题')
    return
  }
  if (!composeForm.value.content.trim()) {
    ElMessage.error('请输入内容')
    return
  }
  composing.value = true
  try {
    const res = await sendMessage({
      receiverId: composeForm.value.receiverId,
      title: composeForm.value.title,
      content: composeForm.value.content
    })
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('发送成功')
      showComposeDialog.value = false
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    composing.value = false
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchMessages()
  messageStore.startPolling()
})
onBeforeUnmount(() => {
  messageStore.stopPolling()
})
</script>

<template>
  <Layout>
    <GlassPanel>
      <div class="message-center">
        <div class="hero">
          <h1>站内信</h1>
          <el-badge :value="messageStore.unreadCount" :hidden="messageStore.unreadCount === 0" :max="99">
            <el-icon :size="24"><Bell /></el-icon>
          </el-badge>
        </div>

        <div class="toolbar">
          <el-tabs v-model="activeTab" @tab-change="fetchMessages">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="未读" name="unread" />
            <el-tab-pane label="任务相关" name="task" />
            <el-tab-pane label="系统/身份" name="system" />
          </el-tabs>
          <div class="toolbar-actions">
            <el-button type="primary" size="small" :icon="EditPen" @click="openCompose">写信</el-button>
            <el-button type="default" size="small" :icon="Check" @click="handleReadAll">全部已读</el-button>
          </div>
        </div>

        <div class="message-layout">
          <div class="message-list" v-loading="loading">
            <el-empty v-if="!loading && filteredMessages.length === 0" description="暂无站内信" />

            <div
              v-for="msg in filteredMessages"
              :key="msg.messageId"
              class="message-item"
              :class="{ unread: !msg.isRead, selected: selectedMessage?.messageId === msg.messageId }"
              @click="handleRead(msg)"
            >
              <div v-if="!msg.isRead" class="unread-dot"></div>
              <div class="message-item-content">
                <div class="message-item-header">
                  <el-tag size="small" type="info">{{ typeLabels[msg.type] || msg.type }}</el-tag>
                  <span class="message-time">{{ formatDate(msg.createTime) }}</span>
                </div>
                <div class="message-item-title" :class="{ bold: !msg.isRead }">{{ msg.title }}</div>
                <div class="message-item-summary">{{ msg.content?.substring(0, 60) }}{{ msg.content?.length > 60 ? '...' : '' }}</div>
              </div>
            </div>
          </div>

          <div class="message-detail" v-if="selectedMessage">
            <div class="detail-header">
              <h2>{{ selectedMessage.title }}</h2>
              <el-button size="small" :icon="Delete" type="danger" @click="handleDelete(selectedMessage.messageId)">删除</el-button>
            </div>
            <div class="detail-meta">
              <span>来自：{{ selectedMessage.senderName || '系统' }}</span>
              <span>时间：{{ formatDate(selectedMessage.createTime) }}</span>
              <el-tag size="small">{{ typeLabels[selectedMessage.type] || selectedMessage.type }}</el-tag>
            </div>
            <div class="detail-content">{{ selectedMessage.content }}</div>
          </div>

          <div class="message-detail message-detail-empty" v-else>
            <el-empty description="选择一条消息查看详情" />
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchMessages"
          class="pagination"
        />
      </div>

      <el-dialog v-model="showComposeDialog" title="写信" width="500px" align-trickle="center">
        <el-form :model="composeForm" label-width="80px">
          <el-form-item label="收件人">
            <el-select v-model="composeForm.receiverId" placeholder="请选择收件人" filterable style="width: 100%">
              <el-option
                v-for="option in receiverOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="composeForm.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input
              v-model="composeForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入内容"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showComposeDialog = false">取消</el-button>
          <el-button type="primary" :loading="composing" @click="handleSend">发送</el-button>
        </template>
      </el-dialog>
    </GlassPanel>
  </Layout>
</template>

<style scoped>
.message-center { padding: 24px; }
.hero { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.hero h1 { font-size: 28px; font-weight: 700; color: var(--text-primary); background: var(--gradient-text-primary); -webkit-background-clip: text; background-clip: text; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.message-layout { display: flex; gap: 16px; min-height: 400px; }
.message-list { flex: 1; max-height: 500px; overflow-y: auto; }
.message-item { display: flex; align-items: flex-start; gap: 8px; padding: 12px; border-radius: 8px; cursor: pointer; transition: all 0.2s; border-bottom: 1px solid #f5f5f5; }
.message-item:hover { background: rgba(102,126,234,0.05); }
.message-item.selected { background: rgba(102,126,234,0.1); }
.message-item.unread { background: rgba(64,158,255,0.03); }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #409eff; margin-top: 6px; flex-shrink: 0; }
.message-item-content { flex: 1; min-width: 0; }
.message-item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.message-time { color: #909399; font-size: 12px; }
.message-item-title { font-size: 14px; color: #303133; margin-bottom: 2px; }
.message-item-title.bold { font-weight: 600; }
.message-item-summary { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.message-detail { flex: 1; padding: 16px; border-left: 1px solid #f0f0f0; }
.message-detail-empty { display: flex; align-items: center; justify-content: center; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.detail-header h2 { font-size: 18px; color: #303133; }
.detail-meta { display: flex; gap: 16px; color: #909399; font-size: 13px; margin-bottom: 16px; }
.detail-content { line-height: 1.8; color: #606266; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>
