<template>
  <div class="shelf-wrapper">
    <!-- ===== 顶部导航 ===== -->
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <span class="brand" @click="$router.push('/')">📚 夏辰义的书城</span>
          <span class="page-title">我的书架</span>
        </div>
        <div class="header-right">
          <span class="welcome">欢迎回来，{{ getStorage('username') || '读者' }}</span>
          <el-button size="small" @click="$router.push('/')">← 返回书城</el-button>
          <el-button type="danger" size="small" @click="logout">退出</el-button>
        </div>
      </div>
    </header>

    <div class="container">
      <!-- ===== 工具栏：排序 + 批量操作 ===== -->
      <div class="toolbar">
        <div class="sort-tabs">
          <span
            v-for="tab in sortTabs"
            :key="tab.value"
            class="sort-tab"
            :class="{ active: sort === tab.value }"
            @click="switchSort(tab.value)"
          >
            {{ tab.label }}
          </span>
        </div>

        <div class="toolbar-right">
          <template v-if="selectedIds.length > 0">
            <span class="selected-count">已选 {{ selectedIds.length }} 本</span>
            <el-button type="danger" size="small" @click="handleBatchRemove">
              批量移出
            </el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
          </template>
          <span v-else class="toolbar-tip">勾选左侧方框可批量移出</span>
        </div>
      </div>

      <!-- ===== 加载中 ===== -->
      <div v-if="loading" class="state-box">
        <el-icon class="is-loading" :size="36"><Loading /></el-icon>
        <p>加载书架中...</p>
      </div>

      <!-- ===== 列表 ===== -->
      <div v-else-if="list.length > 0" class="shelf-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="shelf-item"
          :class="{ 'is-top': item.isTop === 1 }"
        >
          <el-checkbox
            :model-value="selectedIds.includes(item.id)"
            @change="(v) => toggleSelect(item.id, v)"
            class="item-checkbox"
          />

          <div class="item-cover" @click="goDetail(item.novelId)">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="封面" />
            <span v-else class="cover-placeholder">📖</span>
            <div v-if="item.isTop === 1" class="top-badge">📌 置顶</div>
          </div>

          <div class="item-info">
            <h3 class="item-title" @click="goDetail(item.novelId)">
              {{ item.title || `小说 #${item.novelId}` }}
            </h3>

            <p class="item-meta">
              <span>{{ item.author || '佚名' }}</span>
              <span v-if="item.category">· {{ item.category }}</span>
              <span v-if="item.status" class="item-status" :class="statusClass(item.status)">
                {{ statusText(item.status) }}
              </span>
            </p>

            <p class="item-progress">
              <template v-if="item.lastReadChapterNumber">
                📖 读到第 {{ item.lastReadChapterNumber }} 章
              </template>
              <template v-else>📖 尚未开始阅读</template>
              <template v-if="item.totalChapters">
                <span class="divider">·</span>共 {{ item.totalChapters }} 章
              </template>
            </p>

            <p class="item-update" v-if="item.latestChapterTitle">
              🆕 最新章节：{{ item.latestChapterTitle }}
            </p>
          </div>

          <div class="item-actions">
            <el-button
              size="small"
              :type="item.isTop === 1 ? 'warning' : 'default'"
              plain
              @click="handleToggleTop(item)"
            >
              {{ item.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>

            <el-button
              v-if="item.lastReadChapterId"
              type="primary"
              size="small"
              @click="continueRead(item)"
            >
              继续阅读
            </el-button>
            <el-button
              v-else
              type="primary"
              size="small"
              @click="startRead(item)"
            >
              开始阅读
            </el-button>

            <el-button type="danger" size="small" plain @click="handleRemove(item)">
              移出
            </el-button>
          </div>
        </div>
      </div>

      <!-- ===== 空态 ===== -->
      <div v-else class="state-box">
        <div style="font-size:64px;opacity:0.25;">📭</div>
        <p>书架还是空的</p>
        <el-button type="primary" @click="$router.push('/')" style="margin-top:12px;">
          去书城逛逛
        </el-button>
      </div>

      <!-- ===== 分页 ===== -->
      <div v-if="total > 0" class="pagination-box">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 30]"
          layout="total, sizes, prev, pager, next"
          @change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

const router = useRouter()
const getStorage = (key) => localStorage.getItem(key)

// ===== 排序 tab =====
// 注意：这三个值必须和后端 BookshelfSort 枚举一致
const sortTabs = [
  { label: '最近阅读', value: 'RECENT_READ' },
  { label: '最近添加', value: 'RECENT_ADD' },
  { label: '最近更新', value: 'NOVEL_UPDATE' }
]

// ===== 状态 =====
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const sort = ref('RECENT_READ')
const selectedIds = ref([])

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

// ===== 加载列表 =====
const loadList = async () => {
  loading.value = true
  try {
    const res = await getBookshelfList(page.value, size.value, sort.value)
    const body = res.data

    if (body.code !== 200) {
      ElMessage.error(body.msg || '加载书架失败')
      list.value = []
      total.value = 0
      return
    }

    const data = body.data || {}
    list.value = data.content || []
    total.value = data.totalElements || 0

    // 刷新后清掉已经不在当前页的选中项
    const currentIds = new Set(list.value.map((i) => i.id))
    selectedIds.value = selectedIds.value.filter((id) => currentIds.has(id))
  } catch (e) {
    console.error('加载书架失败:', e)
    ElMessage.error('加载书架失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ===== 排序切换 =====
const switchSort = (val) => {
  if (sort.value === val) return
  sort.value = val
  page.value = 1
  selectedIds.value = []
  loadList()
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
    ElMessage.error('加载章节失败')
  }
}

// ===== 置顶 / 取消置顶 =====
const handleToggleTop = async (item) => {
  const newTop = item.isTop !== 1
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
    `确定把《${item.title || '这本小说'}》移出书架吗？`,
    '提示',
    { confirmButtonText: '确定移出', cancelButtonText: '取消', type: 'warning' }
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
    `确定把选中的 ${selectedIds.value.length} 本移出书架吗？`,
    '提示',
    { confirmButtonText: '确定移出', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      try {
        const res = await batchRemoveFromBookshelf([...selectedIds.value])
        if (res.data.code === 200) {
          ElMessage.success('批量移出成功')
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
const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示')
    .then(() => {
      localStorage.clear()
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
.shelf-wrapper {
  min-height: 100vh;
  background: #f5f7fa;
}

/* ===== 顶部导航 ===== */
.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 12px 0;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.brand {
  font-size: 22px;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.page-title {
  font-size: 16px;
  color: #666;
  padding-left: 16px;
  border-left: 1px solid #e8e8e8;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.welcome {
  color: #999;
  font-size: 14px;
}

/* ===== 内容 ===== */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 20px 40px;
}

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 12px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}
.sort-tabs {
  display: flex;
  gap: 6px;
}
.sort-tab {
  padding: 6px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.25s;
}
.sort-tab:hover {
  color: #7c3aed;
  background: #f3f0ff;
}
.sort-tab.active {
  color: white;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.selected-count {
  color: #7c3aed;
  font-weight: 600;
  font-size: 14px;
}
.toolbar-tip {
  color: #bbb;
  font-size: 13px;
}

/* ===== 状态盒子 ===== */
.state-box {
  background: white;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
  color: #999;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.state-box p {
  margin-top: 12px;
  font-size: 15px;
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
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s, transform 0.2s;
  border-left: 4px solid transparent;
}
.shelf-item:hover {
  box-shadow: 0 6px 20px rgba(124, 58, 237, 0.12);
  transform: translateY(-1px);
}
.shelf-item.is-top {
  border-left-color: #f59e0b;
  background: linear-gradient(90deg, #fffbeb 0%, #ffffff 60%);
}
.item-checkbox {
  flex-shrink: 0;
  margin-right: 12px;
}

/* 封面 */
.item-cover {
  flex-shrink: 0;
  width: 72px;
  height: 96px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e0d7ff, #c4b5fd);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  cursor: pointer;
  position: relative;
}
.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  font-size: 32px;
  opacity: 0.6;
}
.top-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  color: white;
  font-size: 11px;
  text-align: center;
  padding: 2px 0;
}

/* 信息 */
.item-info {
  flex: 1;
  padding: 0 20px;
  min-width: 0;
}
.item-title {
  margin: 0 0 6px 0;
  font-size: 16px;
  color: #1a1a2e;
  cursor: pointer;
  transition: color 0.2s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-title:hover {
  color: #7c3aed;
}
.item-meta {
  font-size: 13px;
  color: #888;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.item-meta span {
  white-space: nowrap;
}
.item-status {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
}
.item-status.ongoing {
  color: #d97706;
  background: #fef3c7;
}
.item-status.finished {
  color: #059669;
  background: #d1fae5;
}
.item-progress {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
.item-progress .divider {
  margin: 0 6px;
  color: #ccc;
}
.item-update {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作 */
.item-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 110px;
}
.item-actions .el-button {
  width: 100%;
  margin-left: 0 !important;
}

/* 分页 */
.pagination-box {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 加载图标 */
.is-loading {
  animation: rotate 1.5s linear infinite;
}
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .header-inner {
    flex-direction: column;
    gap: 10px;
  }
  .toolbar {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
  .toolbar-right {
    justify-content: center;
  }
  .shelf-item {
    flex-wrap: wrap;
    gap: 12px;
  }
  .item-info {
    padding: 0;
  }
  .item-actions {
    width: 100%;
    flex-direction: row;
  }
}
</style>