<template>
  <div class="page">
    <SiteHeader />

    <div class="container">
      <!-- ===== 页头 ===== -->
      <div class="head">
        <h1>排行榜</h1>
        <p>按阅读和收藏算出来的热度 —— 不知道读什么的时候，从这儿挑</p>
      </div>

      <!-- ===== 榜单类型 =====
           ★ 真的能点的只有「总榜」和「日榜」（后端 Redis 里就这两个 zset）。
             另外三个是**占位**：做成灰色不可点，点了会告诉你为什么没有。
             为什么不直接藏起来：藏起来这张页看起来"功能很少"，
             摆出来但明确标"还没做"，既能让人看见规划的边界，又不会骗人。 -->
      <div class="chips">
        <a
          v-for="tab in REAL_TABS"
          :key="tab.key"
          :class="{ on: currentTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </a>
        <a
          v-for="tab in PLACEHOLDER_TABS"
          :key="tab.key"
          class="disabled"
          :title="tab.reason"
          @click="onPlaceholder(tab)"
        >
          {{ tab.label }}<span class="soon">待做</span>
        </a>
      </div>

      <div class="layout">
        <!-- ============ 榜单 ============ -->
        <main>
          <!-- 加载中 -->
          <div v-if="loading" class="state-box">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>正在算榜单…</p>
          </div>

          <!-- 报错态（原来没有：接口挂了这里只会空一片，看不出是坏还是没数据）-->
          <div v-else-if="loadError" class="state-box">
            <div class="state-emoji">🔌</div>
            <p>榜单没打开</p>
            <p class="state-sub">{{ errorMsg }}</p>
            <el-button type="primary" class="state-btn" @click="loadRanking">重试</el-button>
          </div>

          <!-- 有数据 -->
          <div v-else-if="list.length > 0" class="board">
            <div class="board-hd">
              <span class="col-no">名次</span>
              <span class="col-cover"></span>
              <span class="col-title">作品</span>
              <span class="col-heat">热度</span>
            </div>

            <div
              v-for="(item, i) in list"
              :key="item.id"
              class="row"
              @click="goDetail(item.id)"
            >
              <span class="col-no" :class="{ top: i < 3 }">
                {{ String(i + 1).padStart(2, '0') }}
              </span>

              <div class="col-cover cover" :class="coverColor(item.title)">
                <img v-if="item.coverUrl" :src="item.coverUrl" alt="封面" />
                <template v-else>{{ item.title.slice(0, 2) }}</template>
              </div>

              <div class="col-title info">
                <b>{{ item.title }}</b>
                <span v-if="i === 0" class="crown">当前第一</span>
              </div>

              <span class="col-heat heat">
                {{ item.heat.toLocaleString() }}
                <small>热度</small>
              </span>
            </div>
          </div>

          <!-- ===== 空态 =====
               ★ 这个状态**经常真的会发生**：后端的榜单是 Redis 里的 zset，
                 Redis 重启 / 刚上线没数据的时候，接口会"降级返回空列表"
                 （不是报错，是空）。所以空态必须说人话。 -->
          <div v-else class="state-box">
            <div class="state-emoji">🍓</div>
            <p>{{ currentLabel }}还没有数据</p>
            <p class="state-sub">
              榜单是 Redis 算出来的，重启或刚上线时会空一阵子 —— 有人看书就会长出来
            </p>
            <el-button type="primary" class="state-btn" @click="$router.push('/store')">
              去书城挑一本
            </el-button>
          </div>
        </main>

        <!-- ============ 右栏 ============ -->
        <aside class="side">
          <div class="card">
            <div class="card-hd">
              <span>{{ currentLabel }} TOP 5</span>
            </div>
            <div
              v-for="(item, i) in top5"
              :key="item.id"
              class="mini"
              @click="goDetail(item.id)"
            >
              <span class="mini-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <div class="mini-cover cover" :class="coverColor(item.title)">
                <img v-if="item.coverUrl" :src="item.coverUrl" alt="封面" />
                <template v-else>{{ item.title.slice(0, 2) }}</template>
              </div>
              <div class="mini-info">
                <b>{{ item.title }}</b>
                <span>{{ item.heat.toLocaleString() }} 热度</span>
              </div>
            </div>
            <div v-if="top5.length === 0" class="mini-empty">这一栏等榜单有数据就有了</div>
          </div>

          <div class="card">
            <div class="card-hd"><span>榜单说明</span></div>
            <div class="note">
              <p>总榜按累计热度排，日榜只看今天 —— 两个榜都是后端用 Redis 的
                zset 实时算的，不是定时刷的静态表。</p>
              <p class="dim">
                <b>周榜 / 月榜 / 收藏榜</b> 还没做：后端目前只维护了总榜和日榜两个榜单。
                等做出来，上面那几个灰的就是正常的标签。
              </p>
            </div>
          </div>

          <div class="card">
            <div class="card-hd"><span>我的书架</span></div>
            <div class="note">
              <p v-if="isLogin">上次读到哪儿都记着，接着往下看就行。</p>
              <p v-else>登录之后，你收藏的书和阅读进度都在这儿。</p>
              <button class="mini-btn" @click="goShelf">
                {{ isLogin ? '打开我的书架' : '登录查看书架' }}
              </button>
            </div>
          </div>
        </aside>
      </div>

      <p class="update-time">榜单更新于 {{ updateTime }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import SiteHeader from '../components/SiteHeader.vue'
import { getRanking } from '../api/novel'
import { useAuth } from '../composables/useAuth'
import { rankList } from '../utils/normalize'
import { coverColor } from '../mock/data'

const router = useRouter()

// ★ 从 useAuth 拿登录态（右栏那个书架入口要区分登录/游客）
const { isLogin } = useAuth()

// ===== 真能用的榜单（后端有数据源）=====
const REAL_TABS = [
  { key: 'total', label: '总榜' },
  { key: 'daily', label: '日榜' }
]

// ===== 占位的榜单（后端没有数据源）=====
// ★ 做成灰色不可点 + 点了解释原因。
//   绝不能做成"看着能点、点了没反应" —— 那样用户会以为是自己网卡了，
//   而且一个点了不动的东西，比压根没有更让人恼火。
const PLACEHOLDER_TABS = [
  {
    key: 'week',
    label: '周榜',
    reason: '周榜还没做 —— 后端现在只维护了「总榜」和「日榜」两个 zset'
  },
  { key: 'month', label: '月榜', reason: '月榜还没做 —— 同上，后端只有总榜和日榜' },
  { key: 'collect', label: '收藏榜', reason: '收藏榜还没做 —— 后端还没有按收藏排的榜' }
]

// ===== 数据 =====
const list = ref([])
const loading = ref(false)
const currentTab = ref('total')

// ===== 报错态（原来没有）=====
const loadError = ref(false)
const errorMsg = ref('')

// ===== 派生 =====
const currentLabel = computed(
  () => REAL_TABS.find((t) => t.key === currentTab.value)?.label || '榜单'
)
const top5 = computed(() => list.value.slice(0, 5))

// ===== 更新时间 =====
// ★ 这里保留"绝对值"（2026-10-16 14:30）而不是"10 分钟前"：
//   榜单的更新时间是个**可信度信号**（说明它不是陈年数据），
//   这种地方报得精确反而让人放心，跟"上次读到什么时候"不一样
const updateTime = computed(() => {
  const now = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())} ${p(now.getHours())}:${p(now.getMinutes())}`
})

// ===== 切换榜单 =====
const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  loadRanking()
}

const onPlaceholder = (tab) => ElMessage.info(tab.reason)

// ===== 加载 =====
const loadRanking = async () => {
  loading.value = true
  loadError.value = false
  errorMsg.value = ''

  try {
    const res = await getRanking(20, currentTab.value)

    // 业务失败（HTTP 200 但 code != 200）
    if (res.data.code !== 200) {
      loadError.value = true
      errorMsg.value = res.data.msg || '后端返回了错误'
      list.value = []
      return
    }

    // ★ 必须过 rankList()：排行榜的字段名是大写开头的（NovelId / Title / Heat），
    //   直接读 item.title 会永远 undefined。原因见 utils/normalize.js
    list.value = rankList(res.data.data)
  } catch (e) {
    console.error('[排行榜] 加载失败:', e)
    loadError.value = true
    errorMsg.value = '没连上服务器，检查一下后端起了没'
    list.value = []
  } finally {
    loading.value = false
  }
}

// ===== 跳转 =====
const goDetail = (id) => {
  if (id) router.push(`/novels/${id}`)
}
const goShelf = () => router.push(isLogin.value ? '/shelf' : '/login')

onMounted(loadRanking)
</script>

<style scoped>
/* ============================================================
   排行榜
   ------------------------------------------------------------
   对标起点：榜单是**表格**（名次 / 封面 / 书名 / 热度列），
   不是卡片流 —— 榜单要的是"一眼扫完 20 名"。
   配色沿用草莓这套，不是起点那种红黑。
   ============================================================ */
.page {
  min-height: 100vh;
  background: var(--bg-page);
}
.container {
  max-width: 1120px;
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

/* ===== 榜单类型 ===== */
.chips {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  padding: 6px 0 22px;
  border-bottom: 1px solid var(--line);
}
.chips a {
  display: flex;
  align-items: center;
  gap: 6px;
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
.chips a.on {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
  font-weight: 600;
}
/* ★ 占位标签：明确地"灰掉 + 不可用"，鼠标是禁止图标 */
.chips a.disabled {
  color: var(--ink-4);
  background: var(--line-2);
  border-color: var(--line-2);
  cursor: not-allowed;
}
.chips a.disabled:hover {
  border-color: var(--line-2);
  color: var(--ink-4);
}
.chips .soon {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--warn-bg);
  color: var(--warn);
  font-weight: 600;
}

/* ===== 两栏 ===== */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  padding: 24px 0 10px;
  align-items: start;
}

/* ===== 榜单表格 ===== */
.board {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}
.board-hd {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  background: var(--bg-page);
  font-size: var(--fs-sm);
  color: var(--ink-3);
  border-bottom: 1px solid var(--line);
}
.row {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--line-2);
  cursor: pointer;
  transition: background var(--t-base);
}
.row:last-child {
  border-bottom: none;
}
.row:hover {
  background: var(--bg-page);
}

.col-no {
  width: 52px;
  flex-shrink: 0;
  font-size: var(--fs-base);
  font-weight: 700;
  color: var(--ink-4);
  font-variant-numeric: tabular-nums;
}
/* 前三名用避雷那支琥珀色 —— 不用金银铜那种廉价的彩色 */
.col-no.top {
  color: var(--warn);
  font-size: var(--fs-md);
}

.col-cover {
  width: 44px;
  height: 58px;
  border-radius: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--fs-xs);
  font-weight: 600;
  text-align: center;
  overflow: hidden;
  letter-spacing: 0.5px;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.col-title {
  flex: 1;
  min-width: 0;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.col-title b {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.crown {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: var(--warn-bg);
  color: var(--warn);
  flex-shrink: 0;
}

.col-heat {
  width: 110px;
  flex-shrink: 0;
  text-align: right;
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.col-heat small {
  display: block;
  font-size: 10px;
  font-weight: 400;
  color: var(--ink-4);
}

/* ===== 右栏 ===== */
.side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 76px;
}
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}
.card-hd {
  padding: 14px 18px;
  border-bottom: 1px solid var(--line-2);
  font-size: var(--fs-base);
  font-weight: 700;
  color: var(--ink);
}
.mini {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  cursor: pointer;
}
.mini:hover {
  background: var(--bg-page);
}
.mini-no {
  width: 16px;
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--ink-4);
  flex-shrink: 0;
}
.mini-no.top {
  color: var(--warn);
}
.mini-cover {
  width: 32px;
  height: 42px;
  border-radius: 3px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  overflow: hidden;
}
.mini-info {
  min-width: 0;
  flex: 1;
}
.mini-info b {
  font-size: 13.5px;
  font-weight: 600;
  display: block;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-info span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.mini-empty {
  padding: 16px 18px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
}

/* 说明卡 / 书架卡 */
.note {
  padding: 16px 18px;
  font-size: var(--fs-sm);
  color: var(--ink-2);
  line-height: 1.8;
}
.note .dim {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line-2);
  color: var(--ink-3);
  font-size: var(--fs-xs);
}
.note .dim b {
  color: var(--warn);
}
.mini-btn {
  margin-top: 12px;
  width: 100%;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 0;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.mini-btn:hover {
  opacity: 0.86;
}

/* ===== 三态 ===== */
.state-box {
  background: #fff;
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
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.8;
}
.state-btn {
  margin-top: 16px;
}
.is-loading {
  animation: rotate 1.5s linear infinite;
}
@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ===== 更新时间 ===== */
.update-time {
  font-size: var(--fs-xs);
  color: var(--ink-4);
  padding: 8px 0 60px;
}

/* ===== 窄屏 ===== */
@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .side {
    position: static;
  }
}
@media (max-width: 640px) {
  .container {
    padding: 0 16px;
  }
  .head h1 {
    font-size: var(--fs-2xl);
  }
  /* 手机上把封面列和表头撤掉，名次 + 书名 + 热度三列就够 */
  .board-hd .col-cover,
  .row .col-cover {
    display: none;
  }
  .col-no {
    width: 36px;
  }
  .col-heat {
    width: 74px;
  }
  .col-title {
    padding: 0 10px;
  }
}
</style>
