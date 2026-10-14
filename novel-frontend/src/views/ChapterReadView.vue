<template>
  <div class="read-wrapper">
    <header class="read-header">
      <div class="header-left">
        <el-button type="text" @click="$router.push(`/novels/${novelId}`)">← 返回小说详情</el-button>
        <span class="novel-title">{{ novelTitle }}</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="prevChapter" :disabled="!hasPrev">上一章</el-button>
        <el-button size="small" @click="nextChapter" :disabled="!hasNext">下一章</el-button>
      </div>
    </header>

    <div class="read-container" v-loading="loading">
      <div v-if="chapter" class="chapter-content">
        <h1 class="chapter-title">{{ chapter.title }}</h1>
        <div class="chapter-body" v-html="chapter.content"></div>
        <div class="chapter-footer">
          <span>字数：{{ chapter.wordCount || 0 }}</span>
          <span>更新时间：{{ chapter.updateTime ? chapter.updateTime.slice(0,10) : '' }}</span>
        </div>
      </div>
      <div v-else-if="!loading" style="text-align:center;padding:60px 0;color:#999;">
        章节内容加载失败
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { readChapter, getNovelDetail } from '../api/novel'

const route = useRoute()
const router = useRouter()

// ★ computed：路由参数变了要跟着变。
//   原来的 const 是死值，从 /chapters/1 跳到 /chapters/2 时不会更新。
const novelId = computed(() => Number(route.params.novelId))
const chapterId = computed(() => Number(route.params.chapterId))

const chapter = ref(null)
const loading = ref(false)
const novelTitle = ref(route.query.novelTitle || '小说')
const chapters = ref([])

// ===== 拉取章节列表 =====
const fetchChapterList = async () => {
  if (!novelId.value) return
  try {
    const res = await getNovelDetail(novelId.value)
    const data = res.data.data || res.data
    const list =
      data.chapters ||
      data.chapterList ||
      (data.novel && (data.novel.chapters || data.novel.chapterList)) ||
      []
    if (Array.isArray(list) && list.length) {
      chapters.value = list
    }
  } catch (e) {
    console.warn('拉取章节列表失败，仅使用本地 state 中的列表', e)
  }
}

// ===== 拉取章节正文 =====
const fetchChapter = async () => {
  if (!novelId.value || !chapterId.value) return
  loading.value = true
  try {
    const res = await readChapter(novelId.value, chapterId.value)
    const data = res.data.data || res.data
    chapter.value = data
    if (data.novelTitle) {
      novelTitle.value = data.novelTitle
    }
  } catch (error) {
    console.error('加载章节失败:', error)
    ElMessage.error('加载章节失败')
    chapter.value = null
  } finally {
    loading.value = false
  }
}

// ===== ★ 从当前路由信息初始化页面（首次进入 + 切章都走这里）=====
const initFromRoute = async () => {
  if (route.query.novelTitle) {
    novelTitle.value = route.query.novelTitle
  }
  if (history.state?.chapters) {
    chapters.value = history.state.chapters
  }
  await fetchChapter()
}

// ===== 上下章计算 =====
const currentIndex = computed(() => {
  return chapters.value.findIndex(c => c.id === chapterId.value)
})
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value >= 0 && currentIndex.value < chapters.value.length - 1)

const prevChapter = () => {
  if (hasPrev.value) {
    const prev = chapters.value[currentIndex.value - 1]
    router.push({
      path: `/novels/${novelId.value}/chapters/${prev.id}`,
      query: { novelTitle: novelTitle.value },
      state: { chapters: chapters.value }
    })
  }
}

const nextChapter = () => {
  if (hasNext.value) {
    const next = chapters.value[currentIndex.value + 1]
    router.push({
      path: `/novels/${novelId.value}/chapters/${next.id}`,
      query: { novelTitle: novelTitle.value },
      state: { chapters: chapters.value }
    })
  }
}

// ===== 首次进入 =====
onMounted(async () => {
  await initFromRoute()
  await fetchChapterList()
})

// ===== ★ 监听 chapterId 变化：同路由参数变化时组件会被复用，onMounted 不再触发 =====
watch(
  () => chapterId.value,
  async () => {
    chapter.value = null
    await initFromRoute()
  }
)

// ===== ★ 监听 novelId 变化：从一本书跳到另一本书时重拉章节列表 =====
watch(
  () => novelId.value,
  async () => {
    chapters.value = []
    await fetchChapterList()
  }
)
</script>

<style scoped>
.read-wrapper {
  min-height: 100vh;
  background: #f5f7fa;
}
.read-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.novel-title {
  font-weight: 600;
  color: #333;
}
.read-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 30px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  min-height: 70vh;
}
.chapter-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 30px;
}
.chapter-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}
.chapter-footer {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  color: #999;
  font-size: 14px;
  display: flex;
  justify-content: space-between;
}
</style>