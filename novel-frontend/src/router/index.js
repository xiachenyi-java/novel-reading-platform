import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import NovelListView from '../views/NovelListView.vue'
import NovelDetailView from '../views/NovelDetailView.vue'

const routes = [
  { path: '/', name: 'home', component: NovelListView },
  { path: '/novels/:id', name: 'novelDetail', component: NovelDetailView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  { path: '/ranking', name: 'ranking', component: () => import('../views/RankingView.vue') },
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
    meta: { requiresAuth: true }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router