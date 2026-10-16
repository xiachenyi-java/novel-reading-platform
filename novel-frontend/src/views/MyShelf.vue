<template>
  <div class="page">
    <!-- 顶栏：全站统一（2026-10-16 接上，之前这页自己写了一份 header）-->
    <SiteHeader>
      <template #right>
        <span class="welcome">欢迎回来，{{ username }}</span>
        <el-button size="small" @click="$router.push('/store')">返回书城</el-button>
        <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
      </template>
    </SiteHeader>

    <div class="container">
      <!-- ===== 页头 ===== -->
      <div class="head">
        <h1>我的书架</h1>
        <p>读到哪儿都记着 —— 挑一本，接着往下看</p>
      </div>

      <!-- ===== 工具栏：排序 + 批量操作 ===== -->
      <div class="toolbar">
        <div class="chips">
          <a
            v-for="tab in sortTabs"
            :key="tab.value"
            :class="{ on: sort === tab.value }"
            @click="switchSort(tab.value)"
          >
            {{ tab.label }}
          </a>
        </div>

        <div class="toolbar-right">
          <template v-if="selectedIds.length > 0">
            <span class="selected-count">已选 {{ selectedIds.length }} 本</span>
            <el-button type="danger" size="small" @click="handleBatchRemove">
              把这 {{ selectedIds.length }} 本移出
            </el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
          </template>
          <span v-else class="toolbar-tip">点一下左边的方框，能一次搬走好几本</span>
        </div>
      </div>

      <!-- ===== 加载中 ===== -->
      <div v-if="loading" class="state-box">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在取你的书架…</p>
      </div>

      <!-- ===== 列表 ===== -->
      <div v-else-if="list.length > 0" class="shelf-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="shelf-item"
          :class="{ 'is-top': item.top }"
        >
          <el-checkbox
            :model-value="selectedIds.includes(item.id)"
            @change="(v) => toggleSelect(item.id, v)"
            class="item-checkbox"
          />

          <!-- 封面：有图用图，没图用"书名色块"（颜色由书名算，全站一致）-->
          <div class="item-cover" :class="coverColor(item.title)" @click="goDetail(item.novelId)">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="封面" />
            <template v-else>{{ item.title || '未命名' }}</template>
            <!-- ★ 字段名是 top（后端 VO 里是 Boolean），不是 isTop（1/0）。
                 原来读 item.isTop 永远是 undefined —— 这个"置顶"角标
                 从来没有显示过。 -->
            <div v-if="item.top" class="top-badge">置顶</div>
          </div>

          <div class="item-info">
            <h3 class="item-title" @click="goDetail(item.novelId)">
              {{ item.title || `小说 #${item.novelId}` }}
            </h3>

            <p class="item-meta">
              <span>{{ item.author || '佚名' }}</span>
              <span v-if="item.category" class="dot">·</span>
              <span v-if="item.category">{{ item.category }}</span>
              <span v-if="item.status" class="item-status" :class="statusClass(item.status)">
                {{ statusText(item.status) }}
              </span>
            </p>

            <!-- 进度：文案统一由 progressText 生成，刻意不说"第几章" ——
                 后端没给章节号，编一个数字出来比不写更糟 -->
            <p class="item-progress">{{ progressText(item) }}</p>

            <!-- 更新 + 字数：两个都是后端真有的字段 -->
            <p class="item-update" v-if="item.lastUpdateTime || item.totalWords">
              <template v-if="item.lastUpdateTime">
                {{ relativeTime(item.lastUpdateTime) }}更新过
              </template>
              <template v-if="item.lastUpdateTime && item.totalWords">
                <span class="dot">·</span>
              </template>
              <template v-if="item.totalWords">{{ formatWords(item.totalWords) }}</template>
            </p>
          </div>

          <!-- 动作：主按钮用近黑（跟全站一致），次要动作是白底描边 -->
          <div class="item-actions">
            <button v-if="item.lastReadChapterId" class="btn-primary" @click="continueRead(item)">
              接着读
            </button>
            <button v-else class="btn-primary" @click="startRead(item)">开始读</button>

            <button class="btn-ghost" @click="handleToggleTop(item)">
              {{ item.top ? '取消置顶' : '置顶' }}
            </button>

            <button class="btn-ghost danger" @click="handleRemove(item)">移出</button>
          </div>
        </div>
      </div>

      <!-- ===== 报错态 ===== -->
      <!-- 顺序很关键：先"有数据"，再"出错了"，最后才是"真的没有"。
           以前没有这一段，接口一挂就显示"书架还是空的" ——
           用户根本看不出是坏了，还是自己真没收藏过书。 -->
      <div v-else-if="loadError" class="state-box">
        <div class="state-emoji">🔌</div>
        <p>书架没打开</p>
        <p class="state-sub">{{ errorMsg }}</p>
        <el-button type="primary" class="state-btn" @click="retry">重试</el-button>
      </div>

      <!-- ===== 空态 ===== -->
      <div v-else class="state-box">
        <div class="state-emoji">🍓</div>
        <p>书架还是空的</p>
        <p class="state-sub">你还没往这儿放过书。挑一本，今晚就有得看了</p>
        <el-button type="primary" class="state-btn" @click="$router.push('/store')">
          去书城挑一本
        </el-button>
      </div>

      <!-- ===== 分页 ===== -->
      <div v-if="total > 0" class="pagination-box">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="pageSizes"
          layout="total, sizes, prev, pager, next"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  getBookshelfList,
  toggleTop,
  removeFromBookshelf,
  batchRemoveFromBookshelf
} from '../api/bookshelf'
import { getNovelDetail } from '../api/novel'
import { usePagination } from '../composables/usePagination'
import { useAuth } from '../composables/useAuth'
import SiteHeader from '../components/SiteHeader.vue'
// 把后端的时间 / 数字变成人话（"昨天"、"12.3 万字"）—— 全站共用一份
import { relativeTime, formatWords } from '../utils/format'
import { coverColor } from '../mock/data'

const router = useRouter()

// ★ 从 useAuth 拿 username / logout
const { username, logout } = useAuth()

// ===== 排序 tab =====
const sortTabs = [
  { label: '最近阅读', value: 'RECENT_READ' },
  { label: '最近添加', value: 'RECENT_ADD' },
  { label: '最近更新', value: 'NOVEL_UPDATE' }
]

// ===== 本地状态 =====
const sort = ref('RECENT_READ')
const selectedIds = ref([])

// ===== 报错态 =====
// ★ 这是原来没有的东西。以前接口挂了，页面会走到"空态"分支显示"书架还是空的"
//   —— 用户分不清"我真没收藏"和"它坏了"（前端说明第十章记着这个已知问题）。
//   现在把"出错"单独拎出来，当成一个正经状态来对待。
const loadError = ref(false)
const errorMsg = ref('')

// ===== 分页（统一逻辑）=====
const {
  page,
  size,
  total,
  list,
  loading,
  pageSizes,
  load: loadList,
  handleCurrentChange,
  handleSizeChange,
  reset
} = usePagination(
  async (p, s) => {
    // ★ 每次真发起请求前先清掉上一次的错误。
    //   否则"重试成功了，错误框还挂在上面"—— 那种界面比报错更让人糊涂。
    loadError.value = false
    errorMsg.value = ''

    const res = await getBookshelfList(p, s, sort.value)
    const body = res.data

    if (body.code !== 200) {
      // 业务失败（比如登录过期）：不弹 toast，改成让错误态一直挂在页面上 ——
      // toast 两秒就没了，用户回头根本看不见发生过什么
      loadError.value = true
      errorMsg.value = body.msg || '后端返回了错误'
      return { list: [], total: 0 }
    }

    const data = body.data || {}
    return {
      list: data.content || [],
      total: data.totalElements || 0
    }
  },
  {
    initialSize: 10,
    pageSizes: [10, 20, 30],
    // 网络层失败（后端没起 / 断网）走这里，同样落进报错态
    onError: (e) => {
      console.error('[书架] 加载失败:', e)
      loadError.value = true
      errorMsg.value = '没连上服务器，检查一下后端起了没'
    }
  }
)

// ===== 选中项清理 =====
watch(list, (newList) => {
  const currentIds = new Set(newList.map((i) => i.id))
  selectedIds.value = selectedIds.value.filter((id) => currentIds.has(id))
})

// ===== 工具函数 =====
const statusText = (s) => {
  if (s === 'ONGOING') return '连载中'
  if (s === 'FINISHED') return '已完结'
  return s || ''
}
const statusClass = (s) => {
  if (s === 'ONGOING') return 'ongoing'
  if (s === 'FINISHED') return 'finished'
  return ''
}

/**
 * 进度文案 —— 书架卡片上那句"人话"
 * ★ 关键：后端只给了 lastReadChapterId（读到哪一章的 id）和 lastReadTime，
 *   **没给章节号，也没给总章数**。所以这里绝不写"读到第 12 章 / 还剩 18 章"——
 *   编一个数字出来，比不写更糟（点两下就露馅了）。
 *   要显示"第 N 章"得后端补字段，已记进 docs/设计/前端改版-设计方案.txt。
 */
const progressText = (item) => {
  if (item.lastReadChapterId) {
    const when = relativeTime(item.lastReadTime)
    return when ? `在读 · ${when}读到这儿` : '在读'
  }
  const added = relativeTime(item.createTime)
  return added ? `还没开始 · ${added}加进来的` : '还没开始'
}

// 报错态里的「重试」：把当前页重新拉一次
const retry = () => loadList()

// ===== 排序切换 =====
const switchSort = (val) => {
  if (sort.value === val) return
  sort.value = val
  selectedIds.value = []
  reset()
}

// ===== 选择 =====
const toggleSelect = (id, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((i) => i !== id)
  }
}
const clearSelection = () => {
  selectedIds.value = []
}

// ===== 跳转 =====
const goDetail = (novelId) => {
  if (novelId) router.push(`/novels/${novelId}`)
}

const continueRead = (item) => {
  if (!item.lastReadChapterId) return
  router.push({
    path: `/novels/${item.novelId}/chapters/${item.lastReadChapterId}`,
    query: { novelTitle: item.title || '' }
  })
}

const startRead = async (item) => {
  try {
    const res = await getNovelDetail(item.novelId)
    const data = res.data.data || res.data
    const chapters = data.chapters || data.chapterList || []
    if (!chapters.length) {
      ElMessage.info('这本小说还没有章节')
      return
    }
    const first = chapters[0]
    router.push({
      path: `/novels/${item.novelId}/chapters/${first.id}`,
      query: { novelTitle: item.title || '' },
      state: { chapters }
    })
  } catch (e) {
    console.error('加载章节失败:', e)
    ElMessage.error('章节没加载出来，再试一次')
  }
}

// ===== 置顶 / 取消置顶 =====
const handleToggleTop = async (item) => {
  // ★ 后端 VO 里叫 top（true / false），不是 isTop（1 / 0）。
  //   原来读 item.isTop 永远是 undefined → newTop 永远是 true，
  //   所以"置顶"只能设成置顶、**永远取消不掉**，按钮文案也一直卡在"置顶"。
  const newTop = !item.top
  try {
    const res = await toggleTop(item.id, newTop)
    if (res.data.code === 200) {
      ElMessage.success(newTop ? '已置顶' : '已取消置顶')
      loadList()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (e) {
    console.error('置顶操作失败:', e)
    ElMessage.error('操作失败')
  }
}

// ===== 移出单个 =====
const handleRemove = (item) => {
  ElMessageBox.confirm(
    // ★ 后半句不是客套：移出书架时后端会连 Redis 里的 progress:* 一起删
    //   （BookshelfServiceIml 的 remove / 批量移出里都有这一步），
    //   阅读进度是真的会丢。用户有权在按下按钮之前就知道。
    `把《${item.title || '这本'}》移出书架？阅读进度也会一起清掉。`,
    '提示',
    { confirmButtonText: '移出', cancelButtonText: '再想想', type: 'warning' }
  )
    .then(async () => {
      try {
        const res = await removeFromBookshelf(item.id)
        if (res.data.code === 200) {
          ElMessage.success('已移出书架')
          loadList()
        } else {
          ElMessage.error(res.data.msg || '移出失败')
        }
      } catch (e) {
        console.error('移出失败:', e)
        ElMessage.error('移出失败')
      }
    })
    .catch(() => {})
}

// ===== 批量移出 =====
const handleBatchRemove = () => {
  if (selectedIds.value.length === 0) return
  ElMessageBox.confirm(
    `把这 ${selectedIds.value.length} 本移出书架？阅读进度也会一起清掉。`,
    '提示',
    { confirmButtonText: '移出', cancelButtonText: '再想想', type: 'warning' }
  )
    .then(async () => {
      try {
        const res = await batchRemoveFromBookshelf([...selectedIds.value])
        if (res.data.code === 200) {
          ElMessage.success('已移出选中的书')
          selectedIds.value = []
          loadList()
        } else {
          ElMessage.error(res.data.msg || '批量移出失败')
        }
      } catch (e) {
        console.error('批量移出失败:', e)
        ElMessage.error('批量移出失败')
      }
    })
    .catch(() => {})
}

// ===== 退出 =====
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示')
    .then(() => {
      logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
/* ============================================================
   我的书架
   ------------------------------------------------------------
   对标番茄小说：卡片流 + 阅读进度看得见。
   ★ 但这页真正的重点是**那句进度文案**（"在读 · 昨晚读到这儿"）——
     所以卡片是横条不是竖格子：那句话需要一行横向的空间才读得舒服。
   配色沿用草莓这套。
   ============================================================ */
.page {
  min-height: 100vh;
  background: var(--bg-page);
}
.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

/* 导航栏右侧那行小字（样式留在本页 —— 它是塞进 SiteHeader 插槽的内容）*/
.welcome {
  color: var(--ink-3);
  font-size: var(--fs-sm);
  white-space: nowrap;
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

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 12px 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
.chips a.on {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
  font-weight: 600;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.selected-count {
  color: var(--ink);
  font-weight: 600;
  font-size: var(--fs-sm);
}
.toolbar-tip {
  color: var(--ink-4);
  font-size: var(--fs-xs);
}

/* ===== 书架列表 ===== */
.shelf-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.shelf-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 16px 20px;
  transition: box-shadow var(--t-base);
  border-left: 3px solid transparent;
}
.shelf-item:hover {
  box-shadow: var(--shadow-hover);
}
/* 置顶的那本：左边一条琥珀色（用避雷那支色，不另发明新色）*/
.shelf-item.is-top {
  border-left-color: var(--warn);
}
.item-checkbox {
  flex-shrink: 0;
}

/* 封面 */
.item-cover {
  position: relative;
  flex-shrink: 0;
  width: 72px;
  height: 96px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.35;
  padding: 6px;
  overflow: hidden;
  cursor: pointer;
  letter-spacing: 0.4px;
}
.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.top-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: var(--warn);
  color: #fff;
  font-size: 10px;
  text-align: center;
  padding: 2px 0;
  letter-spacing: 1px;
}

/* 中间信息 */
.item-info {
  flex: 1;
  min-width: 0;
}
.item-title {
  margin: 0 0 6px 0;
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--ink);
  cursor: pointer;
  transition: color var(--t-base);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-title:hover {
  color: var(--brand);
}
.item-meta {
  font-size: var(--fs-sm);
  color: var(--ink-3);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.item-meta .dot {
  color: var(--ink-4);
}
.item-status {
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
}
/* 连载中 = 琥珀（还没完，别急着入坑）｜已完结 = 青绿（放心读）*/
.item-status.ongoing {
  color: var(--warn);
  background: var(--warn-bg);
}
.item-status.finished {
  color: var(--rec);
  background: var(--rec-bg);
}
/* ★ 这行是这页的主角：进度说人话 */
.item-progress {
  font-size: 13.5px;
  color: var(--ink-2);
  margin-bottom: 4px;
}
.item-update {
  font-size: var(--fs-xs);
  color: var(--ink-4);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-update .dot {
  margin: 0 4px;
}

/* 动作 */
.item-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 104px;
}
.item-actions button {
  width: 100%;
  padding: 7px 0;
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all var(--t-base);
}
/* 主按钮近黑 —— 跟全站一致（设计稿里主按钮就不是紫的）*/
.btn-primary {
  background: var(--ink);
  color: #fff;
  border: 1px solid var(--ink);
}
.btn-primary:hover {
  opacity: 0.86;
}
.btn-ghost {
  background: #fff;
  color: var(--ink-2);
  border: 1px solid var(--line);
}
.btn-ghost:hover {
  border-color: var(--ink-4);
  color: var(--ink);
}
/* 移出是破坏性操作：平时低调，hover 才变红 */
.btn-ghost.danger:hover {
  border-color: var(--danger);
  color: var(--danger);
}

/* ===== 三态 ===== */
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

/* ===== 分页 ===== */
.pagination-box {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.pagination-box :deep(.el-pagination) {
  --el-pagination-font-size: var(--fs-sm);
}

/* ===== 窄屏 ===== */
@media (max-width: 768px) {
  .container {
    padding: 0 16px 40px;
  }
  .head h1 {
    font-size: var(--fs-2xl);
  }
  .welcome {
    display: none;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .chips,
  .toolbar-right {
    justify-content: center;
  }
  /* 手机上：封面和文字一行，动作按钮铺满一整行放下面 */
  .shelf-item {
    flex-wrap: wrap;
    gap: 12px;
  }
  .item-info {
    min-width: 160px;
  }
  .item-actions {
    width: 100%;
    flex-direction: row;
  }
}
</style>
