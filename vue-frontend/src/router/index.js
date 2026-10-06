import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/student/award',
    name: 'StudentAward',
    component: () => import('../views/StudentAward.vue'),
    meta: { requiresAuth: true, roles: ['student', 'admin'] }
  },
  {
    path: '/student/record',
    name: 'ApplicationRecord',
    component: () => import('../views/ApplicationRecord.vue'),
    meta: { requiresAuth: true, roles: ['student', 'admin'] }
  },
  {
    path: '/student/teams',
    name: 'TeamHub',
    component: () => import('../views/TeamHub.vue'),
    meta: { requiresAuth: true, roles: ['student'] }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/NotificationCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/student/application/detail/:id',
    name: 'ApplicationDetail',
    component: () => import('../views/ApplicationDetail.vue'),
    meta: { requiresAuth: true, roles: ['student', 'admin', 'mentor'] }
  },
  {
    path: '/admin/review',
    name: 'AdminReview',
    component: () => import('../views/AdminReview.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  },
  {
    path: '/admin/statistics',
    name: 'AdminStatistics',
    component: () => import('../views/AdminStatistics.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  },
  {
    path: '/admin/competition',
    name: 'CompetitionManage',
    component: () => import('../views/CompetitionManage.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  },
  {
    path: '/admin/student',
    name: 'StudentManage',
    component: () => import('../views/Student.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  }
  ,{
    path: '/admin/teams',
    name: 'AdminTeams',
    component: () => import('../views/AdminTeams.vue'),
    meta: { requiresAuth: true, roles: ['admin'] }
  },
  {
    path: '/student/honor',
    name: 'HonorHub',
    component: () => import('../views/HonorHub.vue'),
    meta: { requiresAuth: true, roles: ['student', 'admin'] }
  },
  {
    path: '/admin/honor',
    name: 'HonorManage',
    component: () => import('../views/HonorManage.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  },
  {
    path: '/admin/risk-rule',
    name: 'RiskRuleConfig',
    component: () => import('../views/RiskRuleConfig.vue'),
    meta: { requiresAuth: true, roles: ['admin'] }
  },
  {
    path: '/admin/duplicate',
    name: 'DuplicateCheck',
    component: () => import('../views/DuplicateCheck.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'mentor'] }
  },
  {
    path: '/admin/student-import',
    name: 'StudentImport',
    component: () => import('../views/StudentImport.vue'),
    meta: { requiresAuth: true, roles: ['admin'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // 从 localStorage 获取用户信息
  const userInfo = localStorage.getItem('saims_user')
  
  const token = localStorage.getItem('saims_token')
  if (to.path === '/login') {
    next()
  } else {
    if (!userInfo || !token) {
      next('/login')
    } else {
      const user = JSON.parse(userInfo)
      const roles = String(user.role || '').split(',').map(v => v.trim())
      const allowed = !to.meta.roles || to.meta.roles.some(role => roles.includes(role))
      if (allowed) next()
      else next('/student/award')
    }
  }
})

export default router
