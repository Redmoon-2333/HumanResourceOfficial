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
        <p class="page-subtitle">查看和编辑个人信息</p>
      </div>
      
      <el-card v-if="userStore.userInfo">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.userInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ userStore.userInfo.studentId }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ userStore.userInfo.major }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ userStore.userInfo.grade }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{ userStore.userInfo.roleHistory }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.userInfo.email }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ userStore.userInfo.phone }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <el-empty v-else description="加载中..." />
    </div>
  </Layout>
</template>
