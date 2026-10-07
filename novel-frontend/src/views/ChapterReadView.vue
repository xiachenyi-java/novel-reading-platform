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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { readChapter } from '../api/novel'

const route = useRoute()
const router = useRouter()

const novelId = Number(route.params.novelId)
const chapterId = Number(route.params.chapterId)

const chapter = ref(null)
const loading = ref(false)

// 小说标题（从路由参数或 store 获取，这里简单从 localStorage 或传参）
// 更好的方式是从详情页传递，但为了简单，我们通过路由 state 或再次请求
// 暂用路由 query 或 state
const novelTitle = ref(route.query.novelTitle || '小说')

// 获取章节内容
const fetchChapter = async () => {
  if (!novelId || !chapterId) return
  loading.value = true
  try {
    const res = await readChapter(novelId, chapterId)
    // 后端返回 Result<Chapter>
    const data = res.data.data || res.data
    chapter.value = data
  } catch (error) {
    console.error('加载章节失败:', error)
    ElMessage.error('加载章节失败')
  } finally {
    loading.value = false
  }
}

// 上一章/下一章（需要知道总章节数和当前索引，简单处理：通过路由传递章节ID列表）
// 这里简化：假设我们只知道当前章节ID，无法自动获取前后，所以需要从详情页传递章节列表
// 我们通过路由 state 传递章节数组
const chapters = ref([]) // 从路由 state 获取
const currentIndex = computed(() => {
  return chapters.value.findIndex(c => c.id === chapterId)
})
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < chapters.value.length - 1)

const prevChapter = () => {
  if (hasPrev.value) {
    const prev = chapters.value[currentIndex.value - 1]
    router.push({
      path: `/novels/${novelId}/chapters/${prev.id}`,
      query: { novelTitle: novelTitle.value },
      state: { chapters: chapters.value }
    })
  }
}

const nextChapter = () => {
  if (hasNext.value) {
    const next = chapters.value[currentIndex.value + 1]
    router.push({
      path: `/novels/${novelId}/chapters/${next.id}`,
      query: { novelTitle: novelTitle.value },
      state: { chapters: chapters.value }
    })
  }
}

onMounted(() => {
  // 从路由 state 获取章节列表
  if (history.state?.chapters) {
    chapters.value = history.state.chapters
  }
  fetchChapter()
})
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