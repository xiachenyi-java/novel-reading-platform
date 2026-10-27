<template>
  <div class="page">
    <!-- 顶栏：全站统一，这是新首页 -->
    <SiteHeader show-search @search="onSearch" />

    <div class="container">
      <!-- ===== 页头 ===== -->
      <div class="head">
        <h1>大家最近在读什么</h1>
        <p>真实的推荐、书评和避雷，帮你在翻开之前就知道值不值得</p>
      </div>

      <!-- ===== 筛选：只保留后端真支持的两种极性 ===== -->
      <div class="chips">
        <a
          v-for="tab in TABS"
          :key="tab.key"
          :class="{ on: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </a>
      </div>

      <div class="feed">
        <!-- ===== 加载中 ===== -->
        <div v-if="loading" class="state-box">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <p>正在加载书评…</p>
        </div>

        <!-- ===== 报错态 =====
             ★ 接口挂了必须和"没有书评"分开，不能伪装成空数据 -->
        <div v-else-if="loadError" class="state-box">
          <div class="state-emoji">🔌</div>
          <p>书评没加载出来</p>
          <p class="state-sub">{{ errorMsg }}</p>
          <button class="state-btn" @click="load(true)">重试</button>
        </div>

        <template v-else>
          <!-- ===== 书评列表 ===== -->
          <ContentCard v-for="item in contents" :key="item.id" :content="item" />

          <!-- ===== 空态 ===== -->
          <div v-if="contents.length === 0" class="state-box">
            <div class="state-emoji">🍓</div>
            <p>{{ emptyTitle }}</p>
            <p class="state-sub">
              {{ isAdmin ? '管理员可以去书详情页生成第一篇' : '换个标签看看' }}
            </p>
            <button
              v-if="activeTab !== 'ALL'"
              class="state-btn"
              @click="switchTab('ALL')"
            >
              看全部
            </button>
          </div>

          <!-- ===== 分页：加载更多（内容流不做页码跳转） ===== -->
          <div v-if="hasMore" class="more">
            <button class="more-btn" :disabled="loadingMore" @click="loadMore">
              {{ loadingMore ? '加载中…' : '加载更多' }}
            </button>
          </div>
          <p v-else-if="contents.length > 0" class="end">已经到底了</p>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import SiteHeader from '../components/SiteHeader.vue'
import ContentCard from '../components/ContentCard.vue'
import { getContents } from '../api/content'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isAdmin } = useAuth()

// 本期只有三个 tab：长评（NOTE）不单独筛，只在「全部」里出现
const TABS = [
  { key: 'ALL', label: '全部' },
  { key: 'REC', label: '推荐' },
  { key: 'WARN', label: '避雷' }
]

const activeTab = ref('ALL')

const contents = ref([])
const page = ref(1)
const total = ref(0)
const SIZE = 10

const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)
const errorMsg = ref('')

const hasMore = computed(() => contents.value.length < total.value)

const emptyTitle = computed(() => {
  if (activeTab.value === 'REC') return '还没有人推荐过'
  if (activeTab.value === 'WARN') return '还没有人避过雷'
  return '还没有书评'
})

// ★ 防止快速切 tab 时，先发出的请求后回来，把新结果覆盖掉
let requestId = 0

/**
 * 拉取书评
 * @param {boolean} reset true = 从第 1 页重新拉（首次进入 / 切 tab / 重试）
 *                        false = 当前页 +1（加载更多）
 */
async function load(reset = true) {
  const current = ++requestId
  const targetPage = reset ? 1 : page.value + 1

  if (reset) {
    loading.value = true
    loadError.value = false
    // ★ 切 tab / 重试时把"加载更多"的状态一起清掉：
    //   否则上一次加载更多如果还没回来，按钮会永远停在"加载中"
    loadingMore.value = false
  } else {
    loadingMore.value = true
  }

  try {
    const res = await getContents({
      page: targetPage,
      size: SIZE,
      polarity: activeTab.value === 'ALL' ? '' : activeTab.value
    })

    // ★ 业务失败是 HTTP 200 + body.code=500，不能只靠 catch
    if (res.data?.code !== 200) {
      throw new Error(res.data?.msg || '后端返回了错误')
    }

    const data = res.data.data || {}
    const list = data.content || []

    contents.value = reset ? list : [...contents.value, ...list]
    page.value = targetPage
    total.value = data.totalElements || 0
  } catch (e) {
    // 过期的请求失败了，别干扰当前这次
    if (current !== requestId) return

    if (reset) {
      loadError.value = true
      errorMsg.value = e?.response
        ? `请求失败（HTTP ${e.response.status}），后端启动了吗？`
        : e?.message || '网络异常'
    } else {
      // 加载更多失败：已经收到的列表保留，只提示重试
      ElMessage.error('加载更多失败，请重试')
    }
  } finally {
    if (current === requestId) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

function switchTab(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  load(true)
}

function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  load(false)
}

onMounted(() => load(true))

// 搜索：发现页自己不搜，直接带着关键词跳到书城页（那边接的是真接口）
const onSearch = (keyword) => {
  router.push(keyword ? { path: '/store', query: { keyword } } : '/store')
}
</script>

<style scoped>
/* ============================================================
   发现页（首页）—— 内容流
   ------------------------------------------------------------
   单栏，宽度收在 760px：内容流是来"读"的，不是来扫货架的。
   （原来右栏那三个榜（避雷榜 / 口碑推荐 / 热门雷点）没有后端
     聚合接口，已撤掉 —— 假榜单比没有榜单更糟。）
   ============================================================ */
.page {
  min-height: 100vh;
  background: var(--bg-page);
}

.container {
  max-width: 760px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ===== 页头 ===== */
.head {
  padding: 38px 0 20px;
}
.head h1 {
  font-size: var(--fs-3xl);
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--ink);
}
.head p {
  font-size: var(--fs-base);
  color: var(--ink-3);
  margin-top: 8px;
}

/* ===== 筛选标签 ===== */
.chips {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  padding: 6px 0 22px;
  border-bottom: 1px solid var(--line);
}
.chips a {
  font-size: var(--fs-sm);
  padding: 6px 15px;
  border-radius: var(--radius-full);
  background: #fff;
  color: var(--ink-2);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: all var(--t-base);
}
.chips a:hover {
  border-color: var(--ink-4);
  color: var(--ink);
}
/* ★ 选中的标签是黑底白字，不是紫底 —— 这套设计里紫色不铺大面积 */
.chips a.on {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
  font-weight: 600;
}

/* ===== 内容流 ===== */
.feed {
  padding: 24px 0 40px;
}

/* ===== 三态（加载 / 报错 / 空）===== */
.state-box {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 60px 20px;
  text-align: center;
  color: var(--ink-3);
}
.state-box p {
  margin-top: 12px;
  font-size: var(--fs-md);
}
.state-box .state-emoji {
  font-size: 56px;
  line-height: 1;
  opacity: 0.3;
}
.state-box .state-sub {
  margin-top: 8px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
}
.state-btn {
  margin-top: 16px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 24px;
  font-size: var(--fs-base);
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: opacity var(--t-base);
}
.state-btn:hover {
  opacity: 0.86;
}

/* ===== 加载更多 ===== */
.more {
  text-align: center;
  padding: 8px 0 20px;
}
.more-btn {
  background: #fff;
  color: var(--ink-2);
  border: 1px solid var(--line);
  border-radius: var(--radius-full);
  padding: 9px 28px;
  font-size: var(--fs-sm);
  font-family: inherit;
  cursor: pointer;
  transition: all var(--t-base);
}
.more-btn:hover:not(:disabled) {
  border-color: var(--ink-4);
  color: var(--ink);
}
.more-btn:disabled {
  cursor: default;
  opacity: 0.6;
}
.end {
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--ink-4);
  padding: 4px 0 24px;
}

/* ===== 窄屏 ===== */
@media (max-width: 600px) {
  .container {
    padding: 0 16px;
  }
  .head h1 {
    font-size: var(--fs-2xl);
  }
}
</style>
