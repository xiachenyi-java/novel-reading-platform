<template>
  <div class="novel-list-wrapper">
    <!-- ===== 顶部导航 =====
         壳和样式在 components/SiteHeader.vue（全站统一），
         下面这些是"只属于首页"的内容，通过插槽塞进去。 -->
    <SiteHeader>
      <!-- 品牌名右侧：分类切换 + 榜入口 -->
      <div class="nav-links">
        <span class="nav-link" :class="{ active: currentTab === 'all' }" @click="switchTab('all')">全部</span>
        <span class="nav-link" :class="{ active: currentTab === 'finish' }" @click="switchTab('finish')">完本</span>
        <span class="nav-link" :class="{ active: currentTab === 'free' }" @click="switchTab('free')">免费</span>
        <span class="nav-link" @click="$router.push('/ranking')">🏆 排行</span>
      </div>

      <!-- 右侧：登录态相关 -->
      <template #right>
        <span class="welcome" v-if="!isLogin">游客模式 · 浏览全站</span>
        <span class="welcome" v-else>欢迎回来，{{ username }}</span>

        <el-button v-if="isAdmin" type="success" size="small" @click="goWrite">
          ✍️ 发布新书
        </el-button>

        <el-button v-if="!isLogin" type="primary" size="small" @click="goLogin">登录</el-button>
        <template v-else>
          <el-button size="small" @click="$router.push('/shelf')">📚 我的书架</el-button>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </template>
      </template>
    </SiteHeader>

    <!-- ===== Banner 推荐位 ===== -->
    <div class="banner-section" v-if="bannerNovels.length > 0">
      <el-carousel height="280px" indicator-position="outside">
        <el-carousel-item v-for="(novel, index) in bannerNovels" :key="index">
          <div class="banner-item" @click="goDetail(novel.id)">
            <div class="banner-content">
              <div class="banner-info">
                <h2>{{ novel.title }}</h2>
                <p>{{ novel.summary || '暂无简介' }}</p>
                <div class="banner-meta">
                  <span>📂 {{ novel.category || '未分类' }}</span>
                  <span>📊 {{ novel.totalWords || 0 }} 字</span>
                  <span>👁️ {{ novel.viewCount || 0 }} 阅读</span>
                </div>
                <el-button type="primary">立即阅读</el-button>
              </div>
              <div class="banner-cover">
                <img
                  v-if="novel.coverUrl || novel.coverUr1"
                  :src="novel.coverUrl || novel.coverUr1"
                  alt="封面"
                />
                <span v-else style="font-size:64px;">📖</span>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- ===== 分类导航 + 搜索 ===== -->
    <div class="category-nav">
      <div class="container">
        <div class="category-list">
          <span
            v-for="cat in categories"
            :key="cat"
            class="category-item"
            :class="{ active: selectedCategory === cat }"
            @click="filterByCategory(cat)"
          >
            {{ cat }}
          </span>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索书名..."
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- ===== 小说列表 ===== -->
    <div v-if="loading" class="container" style="text-align:center;padding:60px 0;">
      <el-icon class="is-loading" style="font-size:32px;"><Loading /></el-icon>
      <p style="color:#999;margin-top:12px;">加载小说中...</p>
    </div>

    <div v-else-if="novels.length > 0" class="container">
      <div class="section-header">
        <h2 class="section-title">
          {{ keyword ? `🔍 "${keyword}" 的搜索结果` : (selectedCategory === '全部' ? '📚 全部作品' : `📂 ${selectedCategory}`) }}
        </h2>
        <span class="section-count">共 {{ total }} 本</span>
      </div>

      <el-row :gutter="24">
        <el-col :span="6" v-for="novel in novels" :key="novel.id" style="margin-bottom:24px;">
          <el-card shadow="hover" class="novel-card" @click="goDetail(novel.id)">
            <div class="card-cover">
              <img
                v-if="novel.coverUrl || novel.coverUr1"
                :src="novel.coverUrl || novel.coverUr1"
                alt="封面"
                style="width:100%;height:100%;object-fit:cover;"
              />
              <div v-else class="cover-placeholder">📖</div>
              <div class="card-badge" v-if="novel.status === 'ONGOING'">连载中</div>
              <div class="card-badge finish" v-else>已完结</div>
            </div>
            <div class="card-body">
              <h3>{{ novel.title }}</h3>
              <p class="summary">{{ novel.summary || '暂无简介' }}</p>
              <div class="meta">
                <span>📊 {{ novel.totalWords || 0 }} 字</span>
                <span>📅 {{ novel.lastUpdateTime ? novel.lastUpdateTime.slice(0,10) : '未更新' }}</span>
              </div>
              <div class="admin-actions" v-if="isAdmin" @click.stop>
                <el-button type="danger" size="small" plain @click="handleDelete(novel.id)">
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="display:flex;justify-content:center;margin-top:20px;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[8, 16, 24]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadNovels"
          @size-change="loadNovels"
        />
      </div>
    </div>

    <div v-else class="container" style="text-align:center;padding:60px 0;">
      <div style="font-size:48px;opacity:0.3;">📭</div>
      <p style="color:#999;margin-top:12px;">
        {{ keyword ? `没有找到与 "${keyword}" 相关的小说` : '暂无小说，敬请期待！' }}
      </p>
      <el-button v-if="isAdmin" type="primary" @click="goWrite" style="margin-top:16px;">
        ✍️ 发布第一本书
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Loading, Search } from '@element-plus/icons-vue'
import { getNovelList, getCategories, deleteNovel } from '../api/novel'
import { useAuth } from '../composables/useAuth'
import SiteHeader from '../components/SiteHeader.vue'

const router = useRouter()

// ★ 从 useAuth 拿登录态 / 权限 / 用户名 / 退出
const { isLogin, isAdmin, username, logout } = useAuth()

const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)
const novels = ref([])
const loading = ref(false)
const selectedCategory = ref('全部')
const currentTab = ref('all')
const keyword = ref('')

// 分类列表（从后端动态加载）
const categories = ref(['全部'])

// Banner 推荐（取前3本作为推荐）
const bannerNovels = computed(() => {
  return novels.value.slice(0, 3)
})

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = ['全部', ...(res.data.data || [])]
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const loadNovels = async () => {
  loading.value = true
  try {
    // 分类：「全部」不传
    const categoryParam = selectedCategory.value === '全部' ? '' : selectedCategory.value

    // 状态：currentTab → 后端 status 值
    let statusParam = ''
    if (currentTab.value === 'finish') {
      statusParam = 'FINISHED'
    }
    // 'all' 和 'free' 都传空（free 暂不处理）

    const res = await getNovelList(
      pageNum.value,
      pageSize.value,
      categoryParam,
      statusParam,
      keyword.value
    )
    const data = res.data.data || res.data
    const list = data.content || data || []

    novels.value = list
    total.value = data.totalElements || list.length
  } catch (error) {
    console.error('加载小说列表失败:', error)
    ElMessage.error('加载小说列表失败')
    novels.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const filterByCategory = (cat) => {
  selectedCategory.value = cat
  pageNum.value = 1
  loadNovels()
}

const handleSearch = () => {
  pageNum.value = 1
  loadNovels()
}

const switchTab = (tab) => {
  currentTab.value = tab
  pageNum.value = 1
  loadNovels()
}

const goLogin = () => router.push('/login')
const goWrite = () => router.push('/write')
const goDetail = (id) => router.push(`/novels/${id}`)

// ★ 退出：从 localStorage.clear() + window.location.href（全页刷新）
//   改成 useAuth.logout() + router.push（SPA 内跳转）
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示').then(() => {
    logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这本小说吗？删除后无法恢复！', '危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteNovel(id)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        loadNovels()
      } else {
        ElMessage.error(res.data.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请检查权限或网络')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadCategories()
  loadNovels()
})
</script>

<style scoped>
.novel-list-wrapper {
  min-height: 100vh;
  background: var(--bg-page);
}

/* ===== 顶部导航里属于"首页自己"的那几块 =====
   （导航栏的壳和通用样式在 components/SiteHeader.vue，不在这里）
   ★ 这些样式是塞进 SiteHeader 插槽的内容，Vue 的规矩是"谁的页面谁管样式"，
     所以必须留在本文件，不能挪到 SiteHeader 里去。 */
.nav-links {
  display: flex;
  gap: var(--sp-5);
}
.nav-link {
  cursor: pointer;
  color: var(--text-3);
  font-size: var(--fs-base);
  white-space: nowrap;
  transition: color var(--t-base);
}
.nav-link:hover {
  color: var(--brand);
}
.nav-link.active {
  color: var(--brand);
  font-weight: 600;
}
.welcome {
  color: var(--text-4);
  font-size: var(--fs-base);
  white-space: nowrap;
}

/* ===== Banner ===== */
/* ★ 左右留白必须和 SiteHeader 用同一个值（--sp-5），
   否则导航栏和下面内容区会差几个像素，看起来"没对齐" */
.banner-section {
  max-width: var(--container);
  margin: var(--sp-5) auto 0;
  padding: 0 var(--sp-5);
}
.banner-item {
  background: linear-gradient(135deg, #1a1040, #2d1b69);
  border-radius: 16px;
  padding: 30px 40px;
  height: 100%;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.banner-info {
  flex: 1;
  color: white;
}
.banner-info h2 {
  font-size: 24px;
  margin-bottom: 8px;
}
.banner-info p {
  opacity: 0.8;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}
.banner-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  opacity: 0.7;
  margin-bottom: 16px;
}
.banner-cover {
  width: 120px;
  height: 160px;
  border-radius: 8px;
  background: rgba(255,255,255,0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  flex-shrink: 0;
}
.banner-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ===== 分类导航 + 搜索 ===== */
.category-nav {
  background: white;
  border-bottom: 1px solid #f0f0f0;
  padding: 8px 0;
  margin-top: 16px;
}
.category-nav .container {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 var(--sp-5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-5);
}
.category-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}
.category-item {
  padding: 6px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
}
.category-item:hover {
  color: #7c3aed;
  background: #f3f0ff;
}
.category-item.active {
  color: white;
  background: #7c3aed;
}
.search-box {
  width: 240px;
  flex-shrink: 0;
}
.search-box :deep(.el-input-group__append) {
  background: #7c3aed;
  border-color: #7c3aed;
  color: white;
  padding: 0 14px;
}
.search-box :deep(.el-input-group__append .el-button) {
  color: white;
  background: transparent;
  border: none;
  margin: 0;
  padding: 0;
}
.search-box :deep(.el-input-group__append .el-button:hover) {
  color: #e9d5ff;
  background: transparent;
}

/* ===== 内容区 ===== */
.container {
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--sp-5) var(--sp-5) var(--sp-7);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-title {
  font-size: 20px;
  color: #1a1a2e;
}
.section-count {
  color: #999;
  font-size: 14px;
}

/* ===== 小说卡片 ===== */
.novel-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 12px;
  overflow: hidden;
}
.novel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.10);
}
.card-cover {
  height: 180px;
  background: linear-gradient(135deg, #e0d7ff, #c4b5fd);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}
.cover-placeholder {
  font-size: 64px;
  opacity: 0.6;
}
.card-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #f59e0b;
  color: white;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
}
.card-badge.finish {
  background: #10b981;
}
.card-body {
  padding: 12px 14px 8px;
}
.card-body h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary {
  font-size: 13px;
  color: #888;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 38px;
}
.meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 12px;
  margin-top: 6px;
}
.admin-actions {
  margin-top: 8px;
  text-align: right;
}
.is-loading {
  animation: rotate 1.5s linear infinite;
}
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 响应式 ===== */
/* ★ 导航栏的窄屏折行规则已经搬进 SiteHeader.vue 统一管，
   这里只留"首页自己"的部分 */
@media (max-width: 768px) {
  /* 手机屏位置紧张，把欢迎语藏掉，给按钮腾地方 */
  .welcome {
    display: none;
  }
  .nav-link {
    font-size: var(--fs-sm);
  }
  .banner-item {
    padding: 20px;
  }
  .banner-info h2 {
    font-size: 18px;
  }
  .banner-cover {
    width: 80px;
    height: 110px;
  }
  /* ★ 这条的 padding 必须和上面 .container 一致。
     它选择器更长（优先级更高），会盖掉 .container 的 padding，
     所以这里得单独再说一遍，否则分类栏会比书本列表多凸出 8px。 */
  .category-nav .container {
    flex-direction: column;
    align-items: stretch;
    padding: 0 var(--sp-4);
  }
  .category-list {
    justify-content: center;
  }
  .search-box {
    width: 100%;
  }
  /* 窄屏左右留白也要和导航栏一致（SiteHeader 窄屏用的是 --sp-4） */
  .container {
    padding: var(--sp-4) var(--sp-4) var(--sp-6);
  }
}
</style>