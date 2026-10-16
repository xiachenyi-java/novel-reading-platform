<template>
  <div class="page">
    <SiteHeader>
      <template #right>
        <el-button size="small" plain @click="$router.push('/store')">返回书城</el-button>
        <!-- 管理员：编辑作品信息（对标起点作家专区的「作品信息编辑」）-->
        <el-button v-if="isAdmin" size="small" plain @click="openEditNovel">编辑作品</el-button>
        <el-button v-if="isAdmin" type="primary" size="small" @click="showAddChapter = true">
          添加章节
        </el-button>
        <el-button v-if="isAdmin" type="danger" size="small" @click="handleDelete">
          删除小说
        </el-button>
      </template>
    </SiteHeader>

    <div class="container">
      <!-- 面包屑 -->
      <div class="crumb">
        <a @click="$router.push('/')">发现</a> ›
        <a @click="$router.push('/store')">{{ categoryName }}</a> ›
        <span class="cur">{{ novel?.title || '加载中' }}</span>
      </div>

      <!-- ===== 加载中 ===== -->
      <div v-if="loading" class="state-box">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在取这本书…</p>
      </div>

      <!-- ===== 报错态 / 查无此书 =====
           ★ 原来没有这两个分支：接口一挂 novel 就是 null，整个页面**一片空白**，
             连"出错了"都不说（前端说明第十章记着这条已知问题）。 -->
      <div v-else-if="loadError" class="state-box">
        <div class="state-emoji">🔌</div>
        <p>{{ errorTitle }}</p>
        <p class="state-sub">{{ errorMsg }}</p>
        <el-button type="primary" class="state-btn" @click="fetchDetail">重试</el-button>
        <el-button class="state-btn" @click="$router.push('/store')">回书城</el-button>
      </div>

      <template v-else-if="book">
        <!-- ================= 书籍头部 ================= -->
        <div class="bh">
          <div class="cover" :class="coverColor(book.title)">
            <img v-if="coverUrl" :src="coverUrl" alt="封面" />
            <template v-else>{{ book.title }}</template>
          </div>

          <div class="bh-info">
            <h1>{{ book.title }}</h1>
            <div class="bh-meta">
              <span>{{ book.author || '佚名' }} 著</span>
              <span class="sep">·</span>
              <span>{{ book.category || '未分类' }}</span>
              <span class="sep">·</span>
              <span>{{ formatWords(book.totalWords) || '字数未填' }}</span>
              <template v-if="book.status">
                <span class="sep">·</span>
                <span class="st">{{ book.status === 'FINISHED' ? '已完结' : '连载中' }}</span>
              </template>
              <template v-if="chapterCount">
                <span class="sep">·</span>
                <span>{{ chapterCount }} 章</span>
              </template>
            </div>

            <p class="bh-desc">{{ book.summary || '这本书还没有简介' }}</p>

            <div v-if="tags.length" class="bh-tags">
              <span v-for="t in tags" :key="t">{{ t }}</span>
            </div>

            <div class="bh-btn">
              <button class="primary" @click="startReading">开始阅读</button>
              <button class="ghost" @click="handleAddToShelf">加入书架</button>
            </div>

            <!-- ★ 演示数据提示：假书（9xxx）必须说出来，不能让人以为是真数据 -->
            <p v-if="isDemo" class="demo-badge">
              这是一本<strong>演示用的假书</strong>：评分 / 雷点 / 书评都是编的，
              用来预览页面效果。后端接口需求见 docs/设计/前端改版-设计方案.txt
            </p>
          </div>

          <!-- 头部右侧：综合评分（只有演示数据才有评分源）-->
          <div class="bh-score">
            <div class="big">{{ book.score || '—' }}</div>
            <div v-if="book.score" class="stars">{{ starsOf(book.score) }}</div>
            <div class="cnt">
              {{ book.humanCount ? `${book.humanCount} 人评价` : '还没有人评价' }}
            </div>
          </div>
        </div>

        <div class="layout">
          <main>
            <!-- ================= 评分：AI vs 读者 ================= -->
            <section class="sec">
              <h2>评分 <span class="hint">AI 和读者的口碑，分开看</span></h2>

              <div v-if="isDemo && book.aiScore" class="card">
                <div class="split">
                  <div class="sp ai">
                    <b>{{ book.aiScore }}</b>
                    <span>AI 评分</span>
                    <span class="note">{{ book.aiCount }} 位推荐者给出</span>
                  </div>
                  <div class="sp human">
                    <b>{{ book.humanScore }}</b>
                    <span>读者评分</span>
                    <span class="note">{{ book.humanCount }} 人给出</span>
                  </div>
                </div>
                <div class="dims">
                  <div v-for="d in book.dims" :key="d.name" class="dim">
                    <span class="name">{{ d.name }}</span>
                    <div class="bar"><i :style="{ width: d.val * 10 + '%' }"></i></div>
                    <span class="val">{{ d.val }}</span>
                  </div>
                </div>
              </div>

              <!-- 空态：真实的书还没有评分数据 -->
              <div v-else class="card empty-inline">
                <p>这本书还没有评分</p>
                <p class="sub">AI 评分和读者评分要等后端做出来（现在没有评分表）</p>
              </div>
            </section>

            <!-- ================= 雷点清单（这产品的招牌）================= -->
            <section class="sec">
              <h2>雷点清单 <span class="hint">翻开之前先看这个</span></h2>

              <div v-if="book.pits?.length" class="pits">
                <div v-for="p in book.pits" :key="p.tag" class="pit">
                  <span class="tag">{{ p.tag }}</span>
                  <p>{{ p.text }}</p>
                </div>
              </div>

              <div v-else class="card empty-inline">
                <p>还没人整理过这本书的雷点</p>
                <p class="sub">雷点要结构化（烂尾 / 水字数 / 太监…），需要后端建表</p>
              </div>
            </section>

            <!-- ================= 亮点 ================= -->
            <section v-if="book.goods?.length" class="sec">
              <h2>亮点 <span class="hint">值得读的地方</span></h2>
              <div class="pits">
                <div v-for="g in book.goods" :key="g.tag" class="pit good">
                  <span class="tag">{{ g.tag }}</span>
                  <p>{{ g.text }}</p>
                </div>
              </div>
            </section>

            <!-- ================= 章节目录 ================= -->
            <section class="sec">
              <h2>
                章节目录
                <span class="hint">{{ chapters.length ? `共 ${chapters.length} 章` : '' }}</span>
              </h2>

              <div v-if="chapters.length" class="chapters">
                <div v-for="chapter in chapters" :key="chapter.id" class="chapter">
                  <span class="ch-title" @click="goRead(chapter.id)">
                    {{ chapter.title || '无标题' }}
                  </span>
                  <span class="ch-right">
                    <span class="ch-words">约 {{ chapter.wordCount || 0 }} 字</span>
                    <template v-if="isAdmin">
                      <el-button type="primary" size="small" link @click="openEditChapter(chapter)">
                        编辑
                      </el-button>
                      <el-button type="danger" size="small" link @click="handleDeleteChapter(chapter.id)">
                        删除
                      </el-button>
                    </template>
                  </span>
                </div>
              </div>

              <div v-else class="card empty-inline">
                <p>{{ isDemo ? '这是演示数据，没有真实章节' : '这本书还没有章节' }}</p>
                <p v-if="!isDemo" class="sub">管理员可以在右上角「添加章节」</p>
              </div>
            </section>

            <!-- ================= 大家怎么说 ================= -->
            <section class="sec">
              <h2>
                大家怎么说
                <span class="hint">人和 AI 混排</span>
              </h2>

              <div v-if="bookPosts.length" class="reviews">
                <article
                  v-for="post in bookPosts"
                  :key="post.id"
                  class="rv"
                  @click="$router.push(`/author/${post.authorId}`)"
                >
                  <div class="rv-ph">
                    <span class="av" :class="coverColor(authorOf(post).name)">
                      {{ authorOf(post).name[0] }}
                    </span>
                    <span class="nm">{{ authorOf(post).name }}</span>
                    <span class="tm">{{ timeAgo(post.ago) }}</span>
                    <span class="pol" :class="POLARITY[post.polarity].cls">
                      {{ POLARITY[post.polarity].text }}
                    </span>
                  </div>
                  <h4>{{ post.title }}</h4>
                  <p>{{ post.text }}</p>
                  <div class="rv-act">
                    <span>赞 {{ post.likes.toLocaleString() }}</span>
                    <span>评论 {{ post.comments }}</span>
                  </div>
                </article>
              </div>

              <div v-else class="card empty-inline">
                <p>还没有人写过这本书</p>
                <p class="sub">书评要等后端的"内容流"接口</p>
              </div>
            </section>
          </main>

          <!-- ================= 右栏 ================= -->
          <aside class="side">
            <template v-if="similar.length">
              <div class="scard">
                <div class="hd">相似推荐</div>
                <div
                  v-for="b in similar"
                  :key="b.id"
                  class="mini"
                  @click="$router.push(`/novels/${b.id}`)"
                >
                  <div class="cover" :class="coverColor(b.title)">{{ b.title }}</div>
                  <div class="info">
                    <b>{{ b.title }}</b>
                    <span>{{ b.category }} · {{ b.score }} 分</span>
                  </div>
                  <span class="sc">{{ b.score }}</span>
                </div>
              </div>
            </template>

            <template v-if="inLists.length">
              <div class="scard">
                <div class="hd">收录它的书单</div>
                <div v-for="l in inLists" :key="l.id" class="mini">
                  <div class="cover" :class="coverColor(l.title)">书单</div>
                  <div class="info">
                    <b>{{ l.title }}</b>
                    <span>{{ curatorName(l) }} · {{ l.count }} 本</span>
                  </div>
                </div>
              </div>
            </template>

            <template v-if="recommenders.length">
              <div class="scard">
                <div class="hd">这本书的推荐者</div>
                <div
                  v-for="a in recommenders"
                  :key="a.id"
                  class="mini"
                  @click="$router.push(`/author/${a.id}`)"
                >
                  <div class="cover round" :class="coverColor(a.name)">{{ a.name[0] }}</div>
                  <div class="info">
                    <b>{{ a.name }}</b>
                    <span>{{ a.bio }}</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 真实书籍没有以上任何一块时的兜底说明（不留白）-->
            <div v-if="!similar.length && !inLists.length && !recommenders.length" class="scard">
              <div class="hd">相关内容</div>
              <div class="mini-note">
                相似推荐、收录书单、推荐者这几块都需要后端新接口
                （内容流 / 书单 / 作者体系），还没做。
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>

    <!-- ====== 编辑作品信息对话框（管理员）======
         参考起点作家专区的「作品信息编辑」：
         表单字段 = 后端 PUT /novels/{id} 实际支持的 5 个字段。
         ★ 后端 DTO 里没有 status，所以这里改不了"连载 / 完结"。 -->
    <el-dialog
      v-model="showEditNovel"
      title="编辑作品信息"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="novelFormRef" :model="novelForm" :rules="novelRules" label-width="80px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="novelForm.title" placeholder="请输入书名" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input v-model="novelForm.author" placeholder="留空则记为「佚名」" maxlength="30" />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <!-- 既能从已有分类里选，也能直接敲一个新分类（咱们的分类是动态的）-->
          <el-select
            v-model="novelForm.category"
            class="full-width"
            placeholder="选择已有分类，或直接输入新分类"
            filterable
            allow-create
            default-first-option
          >
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="简介" prop="summary">
          <el-input
            v-model="novelForm.summary"
            type="textarea"
            :rows="4"
            placeholder="请输入作品简介"
            maxlength="2000"
          />
        </el-form-item>

        <el-form-item label="封面">
          <!-- 和 WriteNovel 一样的写法：el-upload 不走 axios，所以要自己带 token -->
          <div>
            <el-upload
              class="cover-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <img
                v-if="novelForm.coverUrl"
                :src="novelForm.coverUrl"
                class="cover-uploader-preview"
                alt="封面"
              />
              <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 jpg / png，不超过 2MB；不重新上传就沿用原封面</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditNovel = false">取消</el-button>
        <el-button type="primary" :loading="editNovelSubmitting" @click="submitEditNovel">
          保存修改
        </el-button>
      </template>
    </el-dialog>

    <!-- ====== 添加章节对话框 ====== -->
    <el-dialog
      v-model="showAddChapter"
      title="添加章节"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="80px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="chapterForm.title" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="正文内容" prop="content">
          <el-input v-model="chapterForm.content" type="textarea" :rows="12" placeholder="请输入章节正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChapter = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitChapter">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- ====== 编辑章节对话框 ====== -->
    <el-dialog
      v-model="showEditChapter"
      title="编辑章节"
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
          <el-input v-model="editChapterForm.content" type="textarea" :rows="12" placeholder="请输入章节正文" />
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import {
  getNovelDetail,
  updateNovel,
  deleteNovel,
  addChapter,
  updateChapter,
  deleteChapter,
  readChapter,
  getCategories
} from '../api/novel'
import { addToBookshelf } from '../api/bookshelf'
import { useAuth } from '../composables/useAuth'
import SiteHeader from '../components/SiteHeader.vue'
import { formatWords } from '../utils/format'
import {
  BOOKS,
  bookById,
  isMockId,
  postsOfBook,
  booklistsOfBook,
  recommendersOf,
  authorById,
  coverColor,
  timeAgo
} from '../mock/data'

const route = useRoute()
const router = useRouter()

// ★ 从 useAuth 拿 isAdmin / requireLogin
const { isAdmin, requireLogin } = useAuth()

const novel = ref(null)
const chapters = ref([])
const loading = ref(false)

// ===== 三态（★ 报错态是这次新加的，见模板里的说明）=====
const loadError = ref(false)
const errorTitle = ref('这本书没打开')
const errorMsg = ref('')

// ===== 演示数据（书的 id 是 9xxx 就是假的）=====
// 真实库不会分配 9xxx 的号，所以拿这个当"这是演示数据"的判据。
// 假书才带评分 / 雷点 / 亮点 —— 这些后端都还没有。
const demo = computed(() => bookById(route.params.id))
const isDemo = computed(() => isMockId(route.params.id))

/**
 * 页面上到底拿谁渲染
 * ★ 真实接口优先；接口查不到、而这个 id 又落在假数据段，就用假数据顶上，
 *   这样从发现页点一本书进来不会是一片空白。
 */
const book = computed(() => {
  if (novel.value) {
    // 真实书籍没有评分 / 雷点这些字段，缺的就让它缺着 ——
    // 模板里靠"有没有"决定显示内容还是显示空态，绝不编数字
    return {
      ...novel.value,
      score: null
    }
  }
  if (isDemo.value && demo.value) return demo.value
  return null
})

// 演示数据里带的书评 / 书单 / 推荐者（真实书籍这几块拿不到 → 空数组 → 走空态）
const bookPosts = computed(() => bookById(route.params.id) ? postsOfBook(route.params.id) : [])
const inLists = computed(() => (bookById(route.params.id) ? booklistsOfBook(route.params.id) : []))
const recommenders = computed(() => (bookById(route.params.id) ? recommendersOf(route.params.id) : []))

/** 相似推荐：同分类里除自己以外的（只有假书才有）*/
const similar = computed(() => {
  if (!isDemo.value || !demo.value) return []
  return BOOKS.filter(
    (b) => b.category === demo.value.category && b.id !== demo.value.id
  ).slice(0, 4)
})

const tags = computed(() => book.value?.tags || [])

const coverUrl = computed(() => {
  if (novel.value) return novel.value.coverUrl || novel.value.coverUr1 || ''
  return '' // 假书没有封面图，走书名色块
})

const categoryName = computed(() => book.value?.category || '书城')

const curatorName = (l) => (authorById(l.curatorId) || { name: '佚名' }).name
const authorOf = (post) => authorById(post.authorId) || { name: '佚名' }

const POLARITY = {
  rec: { text: '推荐', cls: 'rec' },
  warn: { text: '避雷', cls: 'warn' },
  note: { text: '长评', cls: 'note' }
}

/**
 * 综合评分 → 五颗星
 * ★ 这一块是纯展示，用 ★ 和 ☆ 拼，不引图标库（能原生解决就别加依赖）
 */
const starsOf = (score) => {
  const n = Math.round(Number(score) / 2) // 10 分制 → 5 星
  return '★'.repeat(Math.max(0, Math.min(5, n))) + '☆'.repeat(Math.max(0, 5 - n))
}

/**
 * 这本书一共几章
 * ★ 分两种情况，别写成一个 `||` 了事：
 *   · 真实书籍：章数 = 目录数组的长度
 *   · 演示假书：章数写在假数据里（是个数字）
 *   如果 real 的详情接口把 chapters 塞成了数组，直接 || 会渲染出一个数组出来
 */
const chapterCount = computed(() => {
  if (chapters.value.length) return chapters.value.length
  const c = book.value?.chapters
  return typeof c === 'number' ? c : 0
})

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

// ====== 编辑作品信息（管理员）======
// 只放后端 PUT /novels/{id} 认的 5 个字段，多余的字段后端会忽略、少传的会被清空
const showEditNovel = ref(false)
const editNovelSubmitting = ref(false)
const novelFormRef = ref(null)
const categoryOptions = ref([])
const novelForm = ref({
  title: '',
  author: '',
  summary: '',
  category: '',
  coverUrl: ''
})
const novelRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入简介', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }]
}

// 封面走 el-upload 直传（不走 axios 实例，所以要自己带 token，写法与 WriteNovel 一致）
const uploadAction = '/api/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}))

// 获取小说详情
const fetchDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  loadError.value = false

  try {
    const res = await getNovelDetail(id)

    // 业务失败（HTTP 200 但 code != 200）
    if (res.data.code !== 200) {
      // ★ 假书的 id 后端当然查不到 —— 这不是错误，是演示数据，交给 mock 渲染
      if (isDemo.value && demo.value) {
        novel.value = null
        chapters.value = []
        return
      }
      loadError.value = true
      errorTitle.value = '这本书没打开'
      errorMsg.value = res.data.msg || '后端返回了错误'
      return
    }

    const data = res.data.data || res.data
    if (data.novel) {
      novel.value = data.novel
      chapters.value = data.chapters || []
    } else {
      novel.value = data
      chapters.value = data.chapters || data.chapterList || []
    }
  } catch (error) {
    console.error('[详情] 加载小说详情失败:', error)
    // 网络挂了，但这是一本假书 → 还是能用假数据把页面撑起来
    if (isDemo.value && demo.value) {
      novel.value = null
      chapters.value = []
      return
    }
    loadError.value = true
    errorTitle.value = '这本书没打开'
    errorMsg.value = '没连上服务器，检查一下后端起了没'
  } finally {
    loading.value = false
  }
}

// ====== 拉分类下拉的可选项 ======
// 失败也不挡着改书（所以是 warning 不是 error），但不能静默 —— 静默失败看不出来
const fetchCategories = async () => {
  try {
    const res = await getCategories()
    if (res.data.code !== 200) {
      console.warn('[详情] 分类接口返回失败:', res.data.msg)
      ElMessage.warning('分类列表没加载出来，可以直接手输分类')
      return
    }
    const data = res.data.data || []
    categoryOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('[详情] 加载分类失败:', error)
    ElMessage.warning('分类列表没加载出来，可以直接手输分类')
  }
}

// ====== 打开"编辑作品"对话框 ======
const openEditNovel = () => {
  if (!novel.value) {
    ElMessage.info('演示用的假书改不了 —— 编辑功能只对真实书籍开放')
    return
  }
  // ★ 必须用当前详情回填：后端是整体覆盖这 5 个字段，不回填就等于把它们清空
  novelForm.value = {
    title: novel.value.title || '',
    author: novel.value.author || '',
    summary: novel.value.summary || '',
    category: novel.value.category || '',
    coverUrl: novel.value.coverUrl || novel.value.coverUr1 || ''
  }
  showEditNovel.value = true
  fetchCategories()
}

// ====== 封面：上传前校验 ======
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

// ====== 封面：上传成功 / 失败 ======
const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    novelForm.value.coverUrl = response.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = (error) => {
  console.error('[详情] 上传封面失败:', error)
  ElMessage.error('上传失败，请重试')
}

// ====== 编辑作品信息提交 ======
const submitEditNovel = async () => {
  if (!novelFormRef.value) return
  await novelFormRef.value.validate(async (valid) => {
    if (!valid) return
    editNovelSubmitting.value = true
    try {
      const id = route.params.id
      const res = await updateNovel(id, {
        title: novelForm.value.title,
        author: novelForm.value.author,
        summary: novelForm.value.summary,
        category: novelForm.value.category,
        coverUrl: novelForm.value.coverUrl
      })
      if (res.data.code === 200) {
        ElMessage.success('作品信息已更新')
        showEditNovel.value = false
        // 重新拉一次详情，页面上立刻显示新内容（后端 updateNovel 已经清了详情缓存）
        await fetchDetail()
      } else {
        ElMessage.error(res.data.msg || '保存失败')
      }
    } catch (error) {
      console.error('[详情] 保存作品信息失败:', error)
      ElMessage.error('保存失败，请检查权限或网络')
    } finally {
      editNovelSubmitting.value = false
    }
  })
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
      console.error('[详情] 发布章节失败:', error)
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
      console.error('[详情] 拉取章节正文失败:', e)
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

  editChapterForm.value = { id: chapter.id, title: chapter.title, content }
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
      console.error('[详情] 修改章节失败:', error)
      ElMessage.error('修改失败，请检查权限或网络')
    } finally {
      editSubmitting.value = false
    }
  })
}

// ====== 删除章节 ======
const handleDeleteChapter = (chapterId) => {
  ElMessageBox.confirm('删除后无法恢复，确定删掉这一章？', '危险操作', {
    confirmButtonText: '删除',
    cancelButtonText: '再想想',
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteChapter(route.params.id, chapterId)
        if (res.data.code === 200) {
          ElMessage.success('已删除这一章')
          await fetchDetail()
        } else {
          ElMessage.error(res.data.msg || '删除失败')
        }
      } catch (error) {
        console.error('[详情] 删除章节失败:', error)
        ElMessage.error('删除失败，请检查权限或网络')
      }
    })
    .catch(() => {})
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
    ElMessage.info(isDemo.value ? '这是演示数据，没有真实章节' : '这本书还没有章节')
    return
  }
  goRead(chapters.value[0].id)
}

// ====== 加入书架 ======
const handleAddToShelf = async () => {
  // ★ 假书（9xxx）在后端不存在，加书架必然报"小说不存在" —— 提前拦住，别让它去撞
  if (isDemo.value && !novel.value) {
    ElMessage.info('这是演示用的假书，加不了书架')
    return
  }
  if (!requireLogin('使用「加入书架」')) return

  try {
    const res = await addToBookshelf(route.params.id)
    if (res.data.code === 200) {
      ElMessage.success('已加入书架')
    } else {
      ElMessage.error(res.data.msg || '加入书架失败')
    }
  } catch (error) {
    console.error('[详情] 加入书架失败:', error)
    ElMessage.error('加入书架失败')
  }
}

// ====== 删除小说 ======
const handleDelete = () => {
  ElMessageBox.confirm('删除后无法恢复，确定删掉这本小说？', '危险操作', {
    confirmButtonText: '删除',
    cancelButtonText: '再想想',
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteNovel(route.params.id)
        if (res.data.code === 200) {
          ElMessage.success('已删除')
          router.push('/store')
        } else {
          ElMessage.error(res.data.msg || '删除失败')
        }
      } catch (error) {
        console.error('[详情] 删除失败:', error)
        ElMessage.error('删除失败，请检查权限或网络')
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
/* ============================================================
   书籍详情页
   ------------------------------------------------------------
   这是「避雷」真正落地的地方。三个别人没有的东西：
     1. AI 评分 / 读者评分 **分开显示** —— 差值本身就是内容
     2. **雷点清单** —— 读之前先看，结构化（不是藏在低分里）
     3. 书评和内容流用同一套卡片，人和 AI 混排
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

/* ===== 面包屑 ===== */
.crumb {
  font-size: 12.5px;
  color: var(--ink-3);
  padding: 22px 0 0;
}
.crumb a {
  cursor: pointer;
}
.crumb a:hover {
  color: var(--brand);
}
.crumb .cur {
  color: var(--ink-2);
}

/* ===== 书籍头部 ===== */
.bh {
  display: flex;
  gap: 28px;
  align-items: flex-start;
  padding: 20px 0 26px;
  border-bottom: 1px solid var(--line);
}
.bh .cover {
  width: 168px;
  height: 236px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 3px;
  padding: 18px;
  line-height: 1.4;
  overflow: hidden;
  box-shadow: 0 14px 32px rgba(20, 20, 20, 0.22);
}
.bh .cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bh-info {
  flex: 1;
  min-width: 0;
}
.bh-info h1 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
  color: var(--ink);
}
.bh-meta {
  font-size: 13.5px;
  color: var(--ink-2);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}
.bh-meta .sep {
  color: var(--ink-4);
}
.bh-meta .st {
  color: var(--rec);
}
.bh-desc {
  font-size: 14.5px;
  color: var(--ink-2);
  margin-bottom: 16px;
  max-width: 640px;
}
.bh-tags {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.bh-tags span {
  font-size: var(--fs-xs);
  padding: 3px 11px;
  border-radius: var(--radius-full);
  background: #fff;
  border: 1px solid var(--line);
  color: var(--ink-2);
}
.bh-btn {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.bh-btn .primary {
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 11px 30px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.bh-btn .primary:hover {
  opacity: 0.86;
}
.bh-btn .ghost {
  background: #fff;
  color: var(--ink);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 11px 24px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.bh-btn .ghost:hover {
  border-color: var(--ink-4);
}

/* 演示数据提示（假书专用）*/
.demo-badge {
  margin-top: 16px;
  font-size: var(--fs-xs);
  line-height: 1.8;
  color: var(--warn);
  background: var(--warn-bg);
  border-radius: var(--radius-md);
  padding: 8px 12px;
  max-width: 640px;
}

/* 头部右侧评分 */
.bh-score {
  width: 150px;
  flex-shrink: 0;
  text-align: center;
  padding-top: 8px;
}
.bh-score .big {
  font-size: 52px;
  font-weight: 800;
  line-height: 1;
  color: var(--ink);
}
.bh-score .stars {
  font-size: 15px;
  color: var(--warn);
  letter-spacing: 2px;
  margin: 8px 0 6px;
}
.bh-score .cnt {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* ===== 主体两栏 ===== */
.layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 30px;
  padding: 28px 0 74px;
  align-items: start;
}
.sec {
  margin-bottom: 26px;
}
.sec > h2 {
  font-size: var(--fs-lg);
  font-weight: 700;
  letter-spacing: 0.3px;
  margin-bottom: 14px;
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: var(--ink);
}
.sec > h2 .hint {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  font-weight: 400;
}
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 20px 22px;
}

/* 空态（真实书籍缺数据时用 —— 不留白，说清"为什么没有"）*/
.empty-inline {
  text-align: center;
  color: var(--ink-3);
}
.empty-inline p {
  font-size: var(--fs-base);
}
.empty-inline .sub {
  margin-top: 6px;
  font-size: var(--fs-xs);
  color: var(--ink-4);
}

/* ===== 评分：AI / 读者 分栏 ===== */
.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}
.sp {
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  text-align: center;
}
.sp b {
  font-size: 26px;
  font-weight: 800;
  display: block;
  line-height: 1.2;
}
.sp span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
/* ★ AI 用主色、读者用近黑 —— 两个分数一眼能分开，这是这个产品的卖点 */
.sp.ai b {
  color: var(--brand);
}
.sp.human b {
  color: var(--ink);
}
.sp .note {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
  display: block;
}

/* 维度条 */
.dims {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.dim {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--fs-sm);
}
.dim .name {
  width: 44px;
  color: var(--ink-2);
  flex-shrink: 0;
}
.dim .bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--line-2);
  overflow: hidden;
}
.dim .bar i {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: var(--ink-2);
}
.dim .val {
  width: 30px;
  text-align: right;
  font-weight: 600;
  color: var(--ink);
  flex-shrink: 0;
}

/* ===== 雷点清单 ===== */
.pits {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 20px 22px;
  border-left: 3px solid var(--warn);
}
.pit {
  display: flex;
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px solid var(--line-2);
}
.pit:last-child {
  border-bottom: none;
  padding-bottom: 2px;
}
.pit:first-of-type {
  padding-top: 4px;
}
.pit .tag {
  flex-shrink: 0;
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--warn);
  background: var(--warn-bg);
  padding: 3px 11px;
  border-radius: 4px;
  height: fit-content;
}
.pit p {
  font-size: 13.5px;
  color: var(--ink-2);
}
.pit.good .tag {
  color: var(--rec);
  background: var(--rec-bg);
}

/* ===== 章节目录 ===== */
.chapters {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 6px 22px;
  max-height: 420px;
  overflow-y: auto;
}
.chapter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line-2);
}
.chapter:last-child {
  border-bottom: none;
}
.ch-title {
  font-size: var(--fs-base);
  color: var(--ink-2);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ch-title:hover {
  color: var(--brand);
}
.ch-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.ch-words {
  font-size: var(--fs-xs);
  color: var(--ink-4);
}

/* ===== 书评卡 ===== */
.rv {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 18px 20px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: box-shadow var(--t-base);
}
.rv:hover {
  box-shadow: var(--shadow-hover);
}
.rv-ph {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.rv-ph .av {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--fs-sm);
  font-weight: 700;
  flex-shrink: 0;
}
.rv-ph .nm {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}
.rv-ph .tm {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  margin-left: 8px;
}
.rv-ph .pol {
  margin-left: auto;
  font-size: 11.5px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: var(--radius-full);
}
.pol.rec {
  color: var(--rec);
  background: var(--rec-bg);
}
.pol.warn {
  color: var(--warn);
  background: var(--warn-bg);
}
.pol.note {
  color: var(--ink-2);
  background: var(--line-2);
}
.rv h4 {
  font-size: var(--fs-md);
  font-weight: 600;
  margin-bottom: 7px;
  color: var(--ink);
}
.rv p {
  font-size: 13.5px;
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rv-act {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  display: flex;
  gap: 18px;
  margin-top: 10px;
}

/* ===== 右栏 ===== */
.side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.scard {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}
.scard .hd {
  padding: 14px 18px;
  border-bottom: 1px solid var(--line-2);
  font-size: var(--fs-base);
  font-weight: 700;
  color: var(--ink);
}
.mini {
  display: flex;
  gap: 11px;
  padding: 11px 18px;
  cursor: pointer;
}
.mini:hover {
  background: var(--bg-page);
}
.mini .cover {
  width: 40px;
  height: 56px;
  border-radius: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 9.5px;
  font-weight: 600;
  text-align: center;
  padding: 4px;
  line-height: 1.3;
  overflow: hidden;
}
.mini .cover.round {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: var(--fs-sm);
}
.mini .info {
  min-width: 0;
  flex: 1;
}
.mini .info b {
  font-size: var(--fs-sm);
  font-weight: 600;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink);
}
.mini .info span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini .sc {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--ink-2);
  flex-shrink: 0;
}
.mini-note {
  padding: 16px 18px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
  line-height: 1.8;
}

/* ===== 三态 ===== */
.state-box {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 60px 20px;
  text-align: center;
  color: var(--ink-3);
  margin-top: 20px;
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

/* 封面上传控件（对话框里用，沿用原来的样式）*/
.cover-uploader {
  width: 120px;
}
.cover-uploader-preview {
  width: 120px;
  height: 160px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  display: block;
}
.cover-uploader-icon {
  width: 120px;
  height: 160px;
  border: 1px dashed var(--ink-4);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-3);
}
.upload-tip {
  margin-top: 8px;
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.full-width {
  width: 100%;
}

/* ===== 窄屏 ===== */
@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .container {
    padding: 0 16px;
  }
  .bh {
    flex-direction: column;
  }
  .bh .cover {
    width: 120px;
    height: 170px;
    font-size: 15px;
  }
  .bh-info h1 {
    font-size: var(--fs-2xl);
  }
  .bh-score {
    width: 100%;
    text-align: left;
    display: flex;
    align-items: baseline;
    gap: 12px;
  }
  .bh-score .big {
    font-size: 38px;
  }
  .split {
    grid-template-columns: 1fr;
  }
}
</style>
