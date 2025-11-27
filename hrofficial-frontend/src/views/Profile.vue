<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <Layout>
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">查看个人信息</p>
      </div>
      
      <el-card v-if="userStore.userInfo">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.userInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{ userStore.userInfo.roleHistory }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <el-empty v-else description="加载中..." />
    </div>
  </Layout>
</template>

<style scoped>
.container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-title {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: bold;
  color: var(--color-text-dark);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-light);
}
</style>