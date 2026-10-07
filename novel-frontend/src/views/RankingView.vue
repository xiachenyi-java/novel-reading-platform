<template>
  <div class="ranking-wrapper">
    <!-- ===== 顶部导航 ===== -->
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <span class="brand" @click="$router.push('/')">📚 夏辰义的书城</span>
        </div>
        <div class="header-right">
          <span class="update-time">📅 {{ updateTime }} 更新</span>
          <el-button size="small" @click="$router.push('/')">← 返回书城</el-button>
        </div>
      </div>
    </header>

    <!-- ===== 主体 ===== -->
    <div class="ranking-body">
      <div class="container">
        <!-- 左侧：榜单切换 + 标题 -->
        <div class="ranking-sidebar">
          <div class="sidebar-title">{{ currentTab === 'total' ? '🏆 热门总榜' : '📈 日榜' }}</div>
          <div class="sidebar-subtitle">{{ currentTab === 'total' ? '综合热度排名' : '今日热门排行' }} · {{ updateTime }}</div>
          <div class="sidebar-divider"></div>

          <!-- ===== 榜单切换 ===== -->
          <div class="tab-switch">
            <div
              class="tab-item"
              :class="{ active: currentTab === 'total' }"
              @click="switchTab('total')"
            >
              总榜
            </div>
            <div
              class="tab-item"
              :class="{ active: currentTab === 'daily' }"
              @click="switchTab('daily')"
            >
              📊 日榜
            </div>
          </div>

          <div class="sidebar-desc">{{ currentTab === 'total' ? '基于总阅读量、收藏等综合计算' : '今日新增阅读、收藏等综合计算' }}</div>
        </div>

        <!-- 中间：排行榜列表 -->
        <div class="ranking-main" v-loading="loading">
          <div class="rank-header">
            <span class="rank-no">#</span>
            <span class="rank-cover-col">封面</span>
            <span class="rank-title-col">作品</span>
            <span class="rank-heat-col">🔥 热度</span>
          </div>

          <div
            v-for="(item, index) in rankingList"
            :key="item.NovelId || index"
            class="rank-item"
            @click="goDetail(item.NovelId)"
          >
            <span class="rank-no" :class="getRankClass(index)">
              {{ index + 1 }}
            </span>
            <div class="rank-cover-col">
              <img v-if="item.CoverUrl" :src="item.CoverUrl" alt="封面" />
              <span v-else>📖</span>
            </div>
            <div class="rank-title-col">
              <span class="rank-title">{{ item.Title }}</span>
            </div>
            <span class="rank-heat-col">
              <span class="heat-number">{{ (item.Heat || 0).toLocaleString() }}</span>
            </span>
          </div>

          <div v-if="rankingList.length === 0 && !loading" class="empty-state">
            <div style="font-size:48px;opacity:0.3;">📭</div>
            <p>暂无{{ currentTab === 'total' ? '总榜' : '日榜' }}数据</p>
          </div>
        </div>

        <!-- 右侧：热门推荐 + 书架 -->
        <div class="ranking-sidebar-right">
          <div class="sidebar-card">
            <div class="sidebar-card-title">🔥 热门推荐</div>
            <div
              v-for="(item, index) in hotRecommends"
              :key="index"
              class="sidebar-card-item"
              @click="goDetail(item.NovelId)"
            >
              <span class="sidebar-rank">{{ index + 1 }}</span>
              <span class="sidebar-title-text">{{ item.Title }}</span>
            </div>
          </div>
          <div class="sidebar-card" style="margin-top:16px;">
            <div class="sidebar-card-title">📚 我的书架</div>
            <div class="sidebar-card-action">
              <el-button type="primary" size="small" @click="$router.push('/shelf')" v-if="getStorage('token')">
                查看我的书架
              </el-button>
              <el-button type="primary" size="small" @click="goLogin" v-else>
                登录查看书架
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRanking } from '../api/novel'

const router = useRouter()

// ===== 数据 =====
const rankingList = ref([])
const loading = ref(false)
const currentTab = ref('total')  // 'total' | 'daily'

// ===== 辅助 =====
const getStorage = (key) => localStorage.getItem(key)

// ===== 更新时间 =====
const updateTime = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
})

// ===== 排名样式 =====
const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

// ===== 热门推荐（前5） =====
const hotRecommends = computed(() => {
  return rankingList.value.slice(0, 5)
})

// ===== 切换榜单 =====
const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  loadRanking()
}

// ===== 加载数据 =====
const loadRanking = async () => {
  loading.value = true
  try {
    const res = await getRanking(20, currentTab.value)
    const data = res.data.data || res.data
    rankingList.value = data || []
    console.log(`${currentTab.value === 'total' ? '总榜' : '日榜'}数据:`, rankingList.value)
  } catch (error) {
    console.error('加载排行榜失败:', error)
    ElMessage.error('加载排行榜失败')
    rankingList.value = []
  } finally {
    loading.value = false
  }
}

// ===== 跳转小说详情 =====
const goDetail = (novelId) => {
  if (novelId) {
    router.push(`/novels/${novelId}`)
  }
}

const goLogin = () => router.push('/login')

onMounted(() => {
  loadRanking()
})
</script>

<style scoped>
.ranking-wrapper { min-height: 100vh; background: #f5f7fa; }
.header { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 12px 0; position: sticky; top: 0; z-index: 100; }
.header-inner { max-width: 1300px; margin: 0 auto; padding: 0 20px; display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 22px; font-weight: 700; cursor: pointer; background: linear-gradient(135deg, #7c3aed, #4f46e5); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header-right { display: flex; align-items: center; gap: 16px; }
.update-time { color: #999; font-size: 13px; }
.ranking-body { max-width: 1300px; margin: 0 auto; padding: 20px; }
.container { display: flex; gap: 24px; align-items: flex-start; }

/* ===== 左侧 ===== */
.ranking-sidebar { width: 200px; flex-shrink: 0; background: white; border-radius: 12px; padding: 20px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 80px; }
.sidebar-title { font-size: 20px; font-weight: 700; color: #1a1a2e; padding: 0 20px; }
.sidebar-subtitle { font-size: 12px; color: #999; padding: 0 20px; margin-top: 2px; }
.sidebar-divider { height: 1px; background: #f0f0f0; margin: 12px 16px; }

/* ===== 切换按钮 ===== */
.tab-switch { display: flex; margin: 0 16px 12px; border-radius: 8px; overflow: hidden; border: 1px solid #e8e8e8; }
.tab-item { flex: 1; text-align: center; padding: 8px 0; cursor: pointer; font-size: 14px; font-weight: 500; color: #666; background: #fafafa; transition: all 0.3s; }
.tab-item:first-child { border-right: 1px solid #e8e8e8; }
.tab-item:hover { color: #7c3aed; background: #f3f0ff; }
.tab-item.active { color: white; background: linear-gradient(135deg, #7c3aed, #4f46e5); border-color: #7c3aed; }

.sidebar-desc { font-size: 12px; color: #aaa; padding: 0 20px; line-height: 1.6; }

/* ===== 中间 ===== */
.ranking-main { flex: 1; min-width: 0; background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.rank-header { display: flex; align-items: center; padding: 8px 12px; background: #f8f9fa; border-radius: 8px; font-size: 13px; color: #999; font-weight: 500; margin-bottom: 8px; }
.rank-item { display: flex; align-items: center; padding: 12px; border-radius: 8px; cursor: pointer; transition: background 0.2s; }
.rank-item:hover { background: #f8f5ff; }
.rank-no { width: 50px; flex-shrink: 0; text-align: center; font-weight: 600; font-size: 16px; color: #999; }
.rank-no.rank-1 { color: #ffd700; font-size: 22px; }
.rank-no.rank-2 { color: #c0c0c0; font-size: 20px; }
.rank-no.rank-3 { color: #cd7f32; font-size: 20px; }
.rank-cover-col { width: 50px; flex-shrink: 0; height: 66px; border-radius: 6px; background: linear-gradient(135deg, #e0d7ff, #c4b5fd); display: flex; justify-content: center; align-items: center; overflow: hidden; font-size: 24px; }
.rank-cover-col img { width: 100%; height: 100%; object-fit: cover; }
.rank-title-col { flex: 1; padding: 0 16px; min-width: 0; }
.rank-title { display: block; font-size: 15px; font-weight: 600; color: #1a1a2e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-heat-col { width: 120px; flex-shrink: 0; text-align: center; }
.heat-number { font-size: 16px; font-weight: 700; color: #7c3aed; }

/* ===== 右侧 ===== */
.ranking-sidebar-right { width: 220px; flex-shrink: 0; }
.sidebar-card { background: white; border-radius: 12px; padding: 16px 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.sidebar-card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 2px solid #f0f0f0; }
.sidebar-card-item { display: flex; align-items: center; gap: 10px; padding: 6px 0; cursor: pointer; transition: color 0.2s; }
.sidebar-card-item:hover { color: #7c3aed; }
.sidebar-rank { width: 22px; height: 22px; border-radius: 50%; background: #f0f0f0; display: flex; justify-content: center; align-items: center; font-size: 12px; color: #999; flex-shrink: 0; }
.sidebar-title-text { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sidebar-card-action { text-align: center; padding-top: 4px; }
.empty-state { text-align: center; padding: 40px 0; color: #999; }

/* ===== 响应式 ===== */
@media (max-width: 1100px) { .ranking-sidebar-right { display: none; } }
@media (max-width: 768px) {
  .container { flex-direction: column; }
  .ranking-sidebar { width: 100%; position: static; padding: 12px 0; }
  .tab-switch { margin: 0 16px 12px; }
  .rank-header { display: none; }
  .rank-item { flex-wrap: wrap; gap: 8px; }
  .rank-heat-col { width: auto; text-align: left; padding-left: 16px; }
}
</style>