import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
// ★ 2026-10-15：首页从"书城"换成"发现（内容流）"，书城挪到 /store。
//   为什么书城是改 NovelListView 而不是新建一个文件：它接的是**真接口**
//   （列表 / 分类 / 搜索 / 分页），改版只是换了副样子，逻辑得原样保住。
import FeedView from '../views/FeedView.vue'
import NovelListView from '../views/NovelListView.vue'
import NovelDetailView from '../views/NovelDetailView.vue'

const routes = [
  { path: '/', name: 'home', component: FeedView },
  { path: '/store', name: 'store', component: NovelListView },
  { path: '/booklists', name: 'booklists', component: () => import('../views/BookListView.vue') },
  { path: '/author/:id', name: 'author', component: () => import('../views/AuthorView.vue') },
  { path: '/novels/:id', name: 'novelDetail', component: NovelDetailView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  { path: '/ranking', name: 'ranking', component: () => import('../views/RankingView.vue') },
{ path: '/about',    name: 'about',    component: () => import('../views/static/AboutView.vue') },
{ path: '/contact',  name: 'contact',  component: () => import('../views/static/ContactView.vue') },
{ path: '/terms',    name: 'terms',    component: () => import('../views/static/TermsView.vue') },
{ path: '/privacy',  name: 'privacy',  component: () => import('../views/static/PrivacyView.vue') },
{ path: '/feedback', name: 'feedback', component: () => import('../views/static/FeedbackView.vue') },
  {
    path: '/novels/:novelId/chapters/:chapterId',
    name: 'chapterRead',
    component: () => import('../views/ChapterReadView.vue'),
  },
  {
    path: '/shelf',
    name: 'shelf',
    component: () => import('../views/MyShelf.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/write',
    name: 'write',
    component: () => import('../views/WriteNovel.vue'),
    // ★ 改了：加 role 限制，普通用户即使有 token 也不能进发书页
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  // 需要登录的页面
  if (to.meta.requiresAuth && !token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 需要管理员
  if (to.meta.role && to.meta.role !== role) {
    return next({ path: '/' })
  }

  // 其他一律放行（游客可看）
  next()
})

export default router