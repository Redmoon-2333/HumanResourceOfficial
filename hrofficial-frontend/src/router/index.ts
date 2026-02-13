import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

/**
 * 角色权限定义
 * - public: 公开页面，无需登录
 * - guestOnly: 仅游客可访问（登录/注册）
 * - requiresAuth: 需要登录
 * - requiresMember: 需要部员身份（有roleHistory）
 * - requiresMinister: 需要部长/副部长身份
 */

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
      meta: { title: '首页', public: true }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', public: true, guestOnly: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册', public: true, guestOnly: true }
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
      meta: { title: '活动详情', public: true }
    },
    {
      path: '/materials',
      name: 'Materials',
      component: () => import('@/views/Materials.vue'),
      meta: { title: '资料管理', requiresMember: true }
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
      meta: { title: 'AI对话', requiresMember: true }
    },
    {
      path: '/plan-generator',
      name: 'PlanGenerator',
      component: () => import('@/views/NewPlanGenerator.vue'),
      meta: { title: '策划案生成', requiresMember: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/activation-code-manager',
      name: 'ActivationCodeManager',
      component: () => import('@/views/ActivationCodeManager.vue'),
      meta: { title: '激活码管理', requiresMinister: true }
    },
    {
      path: '/rag-management',
      name: 'RagManagement',
      component: () => import('@/views/RagManagement.vue'),
      meta: { title: 'RAG知识库管理', requiresMinister: true }
    },
    {
      path: '/daily-image-management',
      name: 'DailyImageManagement',
      component: () => import('@/views/DailyImageManagement.vue'),
      meta: { title: '日常图片管理', requiresMinister: true }
    },

    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue'),
      meta: { title: '页面不存在', public: true }
    }
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} - 人力资源管理系统`

  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn
  const isMember = userStore.isMember
  const isMinister = userStore.isMinister

  const isPublic = to.meta.public
  const requiresAuth = to.meta.requiresAuth
  const requiresMember = to.meta.requiresMember
  const requiresMinister = to.meta.requiresMinister
  const guestOnly = to.meta.guestOnly

  // 已登录用户访问登录/注册页面，重定向到首页
  if (guestOnly && isLoggedIn) {
    ElMessage.info('您已登录')
    return next('/home')
  }

  // 需要部长权限
  if (requiresMinister) {
    if (!isLoggedIn) {
      ElMessage.warning('请先登录')
      return next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
    if (!isMinister) {
      ElMessage.error('权限不足，该页面仅部长/副部长可访问')
      return next('/home')
    }
  }

  // 需要部员身份（有roleHistory）
  if (requiresMember) {
    if (!isLoggedIn) {
      ElMessage.warning('请先登录')
      return next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
    if (!isMember) {
      ElMessage.warning('该功能需要部员身份，请先激活账号')
      return next('/profile')
    }
  }

  // 普通登录即可访问
  if (requiresAuth && !isLoggedIn) {
    ElMessage.warning('请先登录')
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  next()
})

export default router
