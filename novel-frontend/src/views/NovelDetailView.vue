<template>
  <div class="detail-wrapper">
    <header class="header">
      <div class="header-left">
        <span class="brand">📚 夏辰义的书城</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="$router.push('/')">← 返回书城</el-button>
        <el-button
          v-if="isAdmin"
          type="primary"
          size="small"
          @click="showAddChapter = true"
        >
          ➕ 添加章节
        </el-button>
        <el-button
          v-if="isAdmin"
          type="danger"
          size="small"
          @click="handleDelete"
        >
          删除小说
        </el-button>
      </div>
    </header>

    <div class="container" v-loading="loading">
      <div v-if="novel" class="detail-card">
        <div class="detail-cover">
          <img
            v-if="novel.coverUrl || novel.coverUr1"
            :src="novel.coverUrl || novel.coverUr1"
            alt="封面"
            style="width:100%;height:100%;object-fit:cover;border-radius:12px;"
          />
          <span v-else style="font-size:80px;opacity:0.5;">📖</span>
        </div>
        <div class="detail-info">
          <h1>{{ novel.title }}</h1>
          <p class="category">📂 分类：{{ novel.category || '未分类' }}</p>
          <p class="desc">{{ novel.summary || '暂无简介' }}</p>
          <div class="stats">
            <span>📊 总字数：{{ novel.totalWords || 0 }} 字</span>
            <span>📅 最后更新：{{ novel.lastUpdateTime ? novel.lastUpdateTime.slice(0,10) : '未更新' }}</span>
          </div>
          <div class="actions">
            <el-button type="primary" plain @click="startReading">📖 开始阅读</el-button>
            <el-button plain @click="handleAddToShelf">📚 加入书架</el-button>
            <el-button plain @click="handleAction('评分')">⭐ 打分</el-button>
          </div>
        </div>
      </div>

      <!-- 章节目录 -->
      <div class="chapter-list">
        <h3>📑 章节目录</h3>
        <ul v-if="chapters && chapters.length">
          <li v-for="chapter in chapters" :key="chapter.id">
            <span class="chapter-title" @click="goRead(chapter.id)">
              {{ chapter.title || '无标题' }}
            </span>
            <span class="chapter-right">
              <span class="word-count">约 {{ chapter.wordCount || 0 }} 字</span>
              <template v-if="isAdmin">
                <el-button type="primary" size="small" link @click="openEditChapter(chapter)">
                  编辑
                </el-button>
                <el-button type="danger" size="small" link @click="handleDeleteChapter(chapter.id)">
                  删除
                </el-button>
              </template>
            </span>
          </li>
        </ul>
        <p v-else style="color:#999;text-align:center;padding:20px 0;">暂无章节</p>
      </div>
    </div>

    <!-- ====== 添加章节对话框 ====== -->
    <el-dialog
      v-model="showAddChapter"
      title="📝 添加章节"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="chapterFormRef"
        :model="chapterForm"
        :rules="chapterRules"
        label-width="80px"
      >
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="正文内容" prop="content">
          <el-input
            v-model="chapterForm.content"
            type="textarea"
            :rows="12"
            placeholder="请输入章节正文"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChapter = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitChapter">
          确认发布
        </el-button>
      </template>
    </el-dialog>

    <!-- ====== 编辑章节对话框 ====== -->
    <el-dialog
      v-model="showEditChapter"
      title="✏️ 编辑章节"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editChapterFormRef"
        :model="editChapterForm"
        :rules="chapterRules"
        label-width="80px"
        v-loading="editLoading"
      >
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="editChapterForm.title" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="正文内容" prop="content">
          <el-input
            v-model="editChapterForm.content"
            type="textarea"
            :rows="12"
            placeholder="请输入章节正文"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditChapter = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEditChapter">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNovelDetail,
  deleteNovel,
  addChapter,
  updateChapter,
  deleteChapter,
  readChapter
} from '../api/novel'
import { addToBookshelf } from '../api/bookshelf'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()

// ★ 从 useAuth 拿 isAdmin / requireLogin
const { isAdmin, requireLogin } = useAuth()

const novel = ref(null)
const chapters = ref([])
const loading = ref(false)

// ====== 添加章节 ======
const showAddChapter = ref(false)
const submitting = ref(false)
const chapterFormRef = ref(null)
const chapterForm = ref({ title: '', content: '' })
const chapterRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
}

// ====== 编辑章节 ======
const showEditChapter = ref(false)
const editSubmitting = ref(false)
const editLoading = ref(false)
const editChapterFormRef = ref(null)
const editChapterForm = ref({ id: null, title: '', content: '' })

// 获取小说详情
const fetchDetail = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getNovelDetail(id)
    const data = res.data.data || res.data
    if (data.novel) {
      novel.value = data.novel
      chapters.value = data.chapters || []
    } else {
      novel.value = data
      chapters.value = data.chapters || data.chapterList || []
    }
  } catch (error) {
    console.error('加载小说详情失败:', error)
    ElMessage.error('加载小说详情失败')
  } finally {
    loading.value = false
  }
}

// ====== 添加章节提交 ======
const submitChapter = async () => {
  if (!chapterFormRef.value) return
  await chapterFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const novelId = route.params.id
      const res = await addChapter(novelId, {
        title: chapterForm.value.title,
        content: chapterForm.value.content
      })
      if (res.data.code === 200) {
        ElMessage.success('章节发布成功！')
        showAddChapter.value = false
        chapterForm.value = { title: '', content: '' }
        chapterFormRef.value?.resetFields()
        await fetchDetail()
      } else {
        ElMessage.error(res.data.msg || '发布失败')
      }
    } catch (error) {
      console.error('发布章节失败:', error)
      ElMessage.error('发布失败，请检查权限或网络')
    } finally {
      submitting.value = false
    }
  })
}

// ====== 打开编辑对话框：先确认有正文，没有就从后端拉全文 ======
const openEditChapter = async (chapter) => {
  let content = chapter.content || ''
  if (!content) {
    editLoading.value = true
    try {
      const res = await readChapter(route.params.id, chapter.id)
      const data = res.data.data || res.data
      content = data.content || ''
    } catch (e) {
      console.error('拉取章节正文失败:', e)
      ElMessage.error('拉取章节正文失败，请稍后重试')
      editLoading.value = false
      return
    } finally {
      editLoading.value = false
    }
  }

  if (!content) {
    ElMessage.warning('未获取到章节正文，无法编辑')
    return
  }

  editChapterForm.value = {
    id: chapter.id,
    title: chapter.title,
    content
  }
  showEditChapter.value = true
}

// ====== 编辑章节提交 ======
const submitEditChapter = async () => {
  if (!editChapterFormRef.value) return
  await editChapterFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      const novelId = route.params.id
      const chapterId = editChapterForm.value.id
      const res = await updateChapter(novelId, chapterId, {
        title: editChapterForm.value.title,
        content: editChapterForm.value.content
      })
      if (res.data.code === 200) {
        ElMessage.success('章节修改成功！')
        showEditChapter.value = false
        await fetchDetail()
      } else {
        ElMessage.error(res.data.msg || '修改失败')
      }
    } catch (error) {
      console.error('修改章节失败:', error)
      ElMessage.error('修改失败，请检查权限或网络')
    } finally {
      editSubmitting.value = false
    }
  })
}

// ====== 删除章节 ======
const handleDeleteChapter = (chapterId) => {
  ElMessageBox.confirm('确定要删除这个章节吗？删除后无法恢复！', '危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const novelId = route.params.id
      const res = await deleteChapter(novelId, chapterId)
      if (res.data.code === 200) {
        ElMessage.success('章节删除成功')
        await fetchDetail()
      } else {
        ElMessage.error(res.data.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除章节失败:', error)
      ElMessage.error('删除失败，请检查权限或网络')
    }
  }).catch(() => {})
}

// ====== 跳转阅读 ======
const goRead = (chapterId) => {
  if (!chapterId) return
  router.push({
    path: `/novels/${route.params.id}/chapters/${chapterId}`,
    query: { novelTitle: novel.value?.title || '' },
    state: { chapters: chapters.value }
  })
}

const startReading = () => {
  if (!chapters.value || chapters.value.length === 0) {
    ElMessage.info('暂无章节')
    return
  }
  goRead(chapters.value[0].id)
}

// ====== 加入书架 ======
const handleAddToShelf = async () => {
  // ★ 用 requireLogin 替换原来的手工弹窗
  if (!requireLogin('使用「加入书架」')) return

  const novelId = route.params.id
  try {
    const res = await addToBookshelf(novelId)
    if (res.data.code === 200) {
      ElMessage.success('已加入书架')
    } else {
      ElMessage.error(res.data.msg || '加入书架失败')
    }
  } catch (error) {
    console.error('加入书架失败:', error)
    ElMessage.error('加入书架失败')
  }
}

// ====== 通用动作 ======
const handleAction = (action) => {
  // ★ 同上
  if (!requireLogin(`使用「${action}」`)) return
  ElMessage.success(`已执行「${action}」操作（模拟）`)
}

// ====== 删除小说 ======
const handleDelete = () => {
  ElMessageBox.confirm('确定要删除这本小说吗？删除后无法恢复！', '危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const id = route.params.id
      const res = await deleteNovel(id)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        router.push('/')
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
  fetchDetail()
})
</script>

<style scoped>
.detail-wrapper {
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.brand {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
}
.detail-card {
  display: flex;
  background: white;
  border-radius: 16px;
  padding: 32px;
  gap: 32px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.detail-cover {
  flex-shrink: 0;
  width: 200px;
  height: 260px;
  background: linear-gradient(135deg, #e0d7ff, #c4b5fd);
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.detail-info {
  flex: 1;
}
.detail-info h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
}
.category {
  color: #888;
  font-size: 14px;
  margin-bottom: 12px;
}
.desc {
  color: #444;
  font-size: 15px;
  line-height: 1.8;
  margin-bottom: 16px;
}
.stats {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 12px;
}
.chapter-list {
  margin-top: 30px;
  background: white;
  border-radius: 16px;
  padding: 24px 32px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.chapter-list h3 {
  margin: 0 0 16px 0;
}
.chapter-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.chapter-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.chapter-title {
  cursor: pointer;
  transition: color 0.2s;
  flex: 1;
}
.chapter-title:hover {
  color: #7c3aed;
}
.chapter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.word-count {
  color: #999;
  font-size: 13px;
}
</style>