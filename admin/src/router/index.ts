import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'

// 静态路由
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据概览', icon: 'DataAnalysis' },
      },
      {
        path: 'articles',
        name: 'Articles',
        component: () => import('@/views/articles/index.vue'),
        meta: { title: '作品管理', icon: 'Document' },
      },
      {
        path: 'articles/pending',
        name: 'PendingArticles',
        component: () => import('@/views/articles/pending.vue'),
        meta: { title: '待审核作品', icon: 'EditPen' },
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/categories/index.vue'),
        meta: { title: '分类管理', icon: 'Menu' },
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/tags/index.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' },
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/comments/index.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' },
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/index.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'sensitive-words',
        name: 'SensitiveWords',
        component: () => import('@/views/sensitive-words/index.vue'),
        meta: { title: '敏感词管理', icon: 'Warning' },
      },
      {
        path: 'banners',
        name: 'Banners',
        component: () => import('@/views/banners/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' },
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('@/views/announcements/index.vue'),
        meta: { title: '公告管理', icon: 'Bell' },
      },
      {
        path: 'operation-logs',
        name: 'OperationLogs',
        component: () => import('@/views/operation-logs/index.vue'),
        meta: { title: '操作日志', icon: 'List' },
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/reports/index.vue'),
        meta: { title: '举报管理', icon: 'WarningFilled' },
      },
      {
        path: 'topics',
        name: 'Topics',
        component: () => import('@/views/topics/index.vue'),
        meta: { title: '话题管理', icon: 'Collection' },
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/templates/index.vue'),
        meta: { title: '模板管理', icon: 'Tickets' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' },
      },
    ],
  },
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
})

// 安装路由守卫
setupGuards(router)

export default router
