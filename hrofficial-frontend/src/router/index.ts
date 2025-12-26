import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册', public: true }
    },
    {
      path: '/activities',
      name: 'Activities',
      component: () => import('@/views/Activities.vue'),
      meta: { title: '活动介绍', public: true }
    },
    {
      path: '/activities/:id',
      name: 'ActivityDetail',
      component: () => import('@/views/ActivityDetail.vue'),
      meta: { title: '活动详情' }
    },
    {
      path: '/materials',
      name: 'Materials',
      component: () => import('@/views/Materials.vue'),
      meta: { title: '资料管理' }
    },
    {
      path: '/past-activities',
      name: 'PastActivities',
      component: () => import('@/views/PastActivities.vue'),
      meta: { title: '往届活动', public: true }
    },
    {
      path: '/alumni',
      name: 'Alumni',
      component: () => import('@/views/Alumni.vue'),
      meta: { title: '往届成员', public: true }
    },
    {
      path: '/ai-chat',
      name: 'AIChat',
      component: () => import('@/views/AIChat.vue'),
      meta: { title: 'AI对话' }
    },
    {
      path: '/plan-generator',
      name: 'PlanGenerator',
      component: () => import('@/views/PlanGenerator.vue'),
      meta: { title: '策划案生成' }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { title: '个人中心' }
    },
    {
      path: '/activation-code-manager',
      name: 'ActivationCodeManager',
      component: () => import('@/views/ActivationCodeManager.vue'),
      meta: { title: '激活码管理' }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || ''} - 人力资源管理系统`
  
  // 所有页面均允许游客访问，在接口层面检查权限
  next()
})

export default router
