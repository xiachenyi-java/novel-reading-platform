<template>
  <div class="page">
    <!-- ===== 顶部导航（全站统一，菜单在里面）===== -->
    <SiteHeader show-search search-placeholder="搜书名、作者" @search="handleSearch">
      <template #right>
        <!-- 右侧：登录态相关（这块是"这个页面自己的东西"，所以留在本文件里） -->
        <span class="welcome" v-if="!isLogin">游客模式 · 浏览全站</span>
        <span class="welcome" v-else>欢迎回来，{{ username }}</span>

        <el-button v-if="isAdmin" type="success" size="small" @click="goWrite">
          发布新书
        </el-button>

        <el-button v-if="!isLogin" type="primary" size="small" @click="goLogin">登录</el-button>
        <template v-else>
          <el-button size="small" @click="$router.push('/shelf')">我的书架</el-button>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </template>
      </template>
    </SiteHeader>

    <div class="container">
      <!-- ================= 顶部三栏：分类树 / 推荐位 / 人气榜 ================= -->
      <div class="hero">
        <!-- 左：分类树 -->
        <aside class="tree">
          <div class="hd">作品分类</div>
          <a
            v-for="cat in categoryTree"
            :key="cat"
            :class="{ on: selectedCategory === cat }"
            @click="filterByCategory(cat)"
          >
            <span>{{ cat === '全部' ? '全部作品' : cat }}</span>
            <!-- ★ 只有"全部"能显示真实总数 —— 每个分类各有多少本，
                 需要后端在 /novels/categories 里带上数量。
                 编不出来就不显示（见 docs/设计/前端改版-设计方案.txt）-->
            <span v-if="cat === '全部' && total">{{ total }}</span>
          </a>
        </aside>

        <!-- 中：推荐位（用列表里第一本真实的小说，不编）-->
        <div v-if="featureNovel" class="feature" @click="goDetail(featureNovel.id)">
          <div class="info">
            <span class="tag">本周强推</span>
            <h2>{{ featureNovel.title }}</h2>
            <p>{{ featureNovel.summary || '这本书还没有简介' }}</p>
            <div class="meta">
              {{ featureNovel.author || '佚名' }} ·
              {{ featureNovel.category || '未分类' }} ·
              {{ formatWords(featureNovel.totalWords) || '字数未填' }} ·
              {{ featureNovel.status === 'FINISHED' ? '已完结' : '连载中' }}
            </div>
            <button class="btn" @click.stop="goDetail(featureNovel.id)">开始阅读</button>
          </div>
          <div class="big-cover" :class="coverColor(featureNovel.title)">
            <img v-if="coverOf(featureNovel)" :src="coverOf(featureNovel)" alt="封面" />
            <template v-else>{{ featureNovel.title }}</template>
          </div>
        </div>

        <!-- 右：人气榜（真实接口 GET /novels/ranking）-->
        <aside class="rank">
          <div class="hd">
            <b>人气榜</b>
            <a @click="$router.push('/ranking')">完整 ›</a>
          </div>
          <div
            v-for="(n, i) in ranking"
            :key="n.id || i"
            class="rk"
            @click="goDetail(n.id)"
          >
            <span class="no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="t">{{ n.title }}</span>
            <!-- ★ 榜接口没有字数，只有热度（NovelRankVO 就 4 个字段）-->
            <span class="h">{{ n.heat.toLocaleString() }}</span>
          </div>
          <div v-if="ranking.length === 0" class="rk-empty">榜单还没数据</div>
        </aside>
      </div>

      <!-- ================= 本周强推 ================= -->
      <template v-if="picks.length > 0">
        <div class="sec-hd">
          <h3>本周强推</h3>
        </div>
        <div class="rec-row">
          <div
            v-for="n in picks"
            :key="n.id"
            class="rec"
            @click="goDetail(n.id)"
          >
            <div class="cover" :class="coverColor(n.title)">
              <img v-if="coverOf(n)" :src="coverOf(n)" alt="封面" />
              <template v-else>{{ n.title }}</template>
            </div>
            <div class="info">
              <span class="cat">{{ n.category || '未分类' }}</span>
              <h4>{{ n.title }}</h4>
              <div class="au">{{ n.author || '佚名' }}</div>
              <div class="d">{{ n.summary || '暂无简介' }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- ================= 全部作品 ================= -->
      <div class="sec-hd">
        <h3>{{ listTitle }}</h3>
        <span class="total">共 {{ total }} 本</span>
      </div>

      <!-- 状态筛选（★ 这个筛选是**真的管用**的，走后端 status 参数）
           ★ 排序（按热度/最新/字数）没做：后端没有 sort 参数，
             摆一排点了没反应的下拉框，比不摆更糟 -->
      <div class="filters">
        <a
          v-for="s in STATUS_TABS"
          :key="s.value"
          :class="{ on: status === s.value }"
          @click="switchStatus(s.value)"
        >
          {{ s.label }}
        </a>
      </div>

      <!-- ===== 加载中 ===== -->
      <div v-if="loading" class="state-box">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在找书…</p>
      </div>

      <!-- ===== 报错态 =====
           ★ 原来没有这个分支：接口一挂就走到空态显示"暂无小说"，
             用户分不清是"真没书"还是"服务挂了"（前端说明第十章记着这个已知问题）-->
      <div v-else-if="loadError" class="state-box">
        <div class="state-emoji">🔌</div>
        <p>书城没打开</p>
        <p class="state-sub">{{ errorMsg }}</p>
        <el-button type="primary" class="state-btn" @click="loadNovels">重试</el-button>
      </div>

      <!-- ===== 书城有数据 ===== -->
      <template v-else-if="novels.length > 0">
        <div class="grid">
          <div
            v-for="n in novels"
            :key="n.id"
            class="book"
            @click="goDetail(n.id)"
          >
            <div class="cover" :class="coverColor(n.title)">
              <img v-if="coverOf(n)" :src="coverOf(n)" alt="封面" />
              <template v-else>{{ n.title }}</template>
              <span class="st" :class="n.status === 'FINISHED' ? 'fin' : 'ing'">
                {{ n.status === 'FINISHED' ? '完结' : '连载' }}
              </span>
            </div>
            <h4>{{ n.title }}</h4>
            <div class="au">{{ n.author || '佚名' }}</div>
            <div class="au">{{ formatWords(n.totalWords) }}</div>
            <!-- 管理员删除：@click.stop 挡住外层"进详情页" -->
            <div v-if="isAdmin" class="admin" @click.stop>
              <el-button type="danger" size="small" plain @click="handleDelete(n.id)">
                删除
              </el-button>
            </div>
          </div>
        </div>

        <div class="pager">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadNovels"
            @size-change="loadNovels"
          />
        </div>
      </template>

      <!-- ===== 空态 ===== -->
      <div v-else class="state-box">
        <div class="state-emoji">🍓</div>
        <p>{{ emptyTitle }}</p>
        <p class="state-sub">{{ emptySub }}</p>
        <el-button
          v-if="isAdmin && !keyword && selectedCategory === '全部'"
          type="primary"
          class="state-btn"
          @click="goWrite"
        >
          发布第一本书
        </el-button>
        <el-button
          v-else
          type="primary"
          class="state-btn"
          @click="resetFilter"
        >
          看全部作品
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getNovelList, getCategories, getRanking, deleteNovel } from '../api/novel'
import { useAuth } from '../composables/useAuth'
import SiteHeader from '../components/SiteHeader.vue'
import { coverColor } from '../mock/data'
import { formatWords } from '../utils/format'
import { rankList } from '../utils/normalize'

const route = useRoute()
const router = useRouter()

// ★ 从 useAuth 拿登录态 / 权限 / 用户名 / 退出
const { isLogin, isAdmin, username, logout } = useAuth()

// ===== 列表状态 =====
const pageNum = ref(1)
const pageSize = ref(12) // ★ 从 8 改成 12：设计稿的网格一行 6 本，12 刚好两行
const total = ref(0)
const novels = ref([])
const loading = ref(false)
const selectedCategory = ref('全部')
const keyword = ref('')
const categories = ref([])

// 状态筛选（对应后端 status 参数）
const STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '连载中', value: 'ONGOING' },
  { label: '已完结', value: 'FINISHED' }
]
const status = ref('')

// ===== 报错态（原来没有，见模板里的说明）=====
const loadError = ref(false)
const errorMsg = ref('')

// ===== 榜（真实接口）=====
const ranking = ref([])

// ===== 派生 =====
const categoryTree = computed(() => ['全部', ...categories.value])

/** 推荐位用列表里第一本（真数据，不编）*/
const featureNovel = computed(() => novels.value[0] || null)

/** 本周强推：紧跟推荐位往后取 4 本（真数据）*/
const picks = computed(() => novels.value.slice(1, 5))

const listTitle = computed(() => {
  if (keyword.value) return `"${keyword.value}" 的搜索结果`
  if (selectedCategory.value !== '全部') return selectedCategory.value
  return '全部作品'
})

const emptyTitle = computed(() =>
  keyword.value ? `没有找到"${keyword.value}"` : '这里还没有书'
)
const emptySub = computed(() =>
  keyword.value ? '换个关键词试试，或者看看别的分类' : '换个分类看看'
)

// ===== 小工具 =====
/** 封面地址：★ 兼容两个字段名 —— 后端历史上有过 coverUrl / coverUr1 两种写法（l 打成 1）*/
const coverOf = (n) => n.coverUrl || n.coverUr1 || ''

// ===== 加载分类 =====
const loadCategories = async () => {
  try {
    const res = await getCategories()
    // 业务异常是 HTTP 200 + code=500，axios 不会 reject，所以这里要显式判
    if (res.data.code !== 200) {
      console.warn('[书城] 分类接口返回失败:', res.data.msg)
      return
    }
    categories.value = res.data.data || []
  } catch (e) {
    // ★ 分类拉不到不算致命：树里只剩"全部"，书还是能看。
    //   但必须留日志 —— 只吞不报就是静默失败（前端说明第十章记过这条）
    console.error('[书城] 加载分类失败:', e)
  }
}

// ===== 加载榜 =====
const loadRanking = async () => {
  try {
    const res = await getRanking(10, 'total')
    if (res.data.code !== 200) {
      console.warn('[书城] 榜单接口返回失败:', res.data.msg)
      return
    }
    // ★ 必须过 rankList() 转一道：排行榜接口返回的字段名是**大写开头**的
    //   （NovelId / Title / Heat），直接读 n.title 会永远 undefined ——
    //   书名空白、点了没反应，而且不报错。原因见 utils/normalize.js
    ranking.value = rankList(res.data.data)
  } catch (e) {
    console.error('[书城] 加载榜单失败:', e)
  }
}

// ===== 加载列表 =====
const loadNovels = async () => {
  loading.value = true
  loadError.value = false
  errorMsg.value = ''

  try {
    const categoryParam = selectedCategory.value === '全部' ? '' : selectedCategory.value

    const res = await getNovelList(
      pageNum.value,
      pageSize.value,
      categoryParam,
      status.value,
      keyword.value
    )

    // ★ 业务失败（HTTP 200 但 code != 200）也要走报错态 —— 不能落到空态
    if (res.data.code !== 200) {
      loadError.value = true
      errorMsg.value = res.data.msg || '后端返回了错误'
      novels.value = []
      total.value = 0
      return
    }

    const data = res.data.data || {}
    novels.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e) {
    console.error('[书城] 加载小说列表失败:', e)
    loadError.value = true
    errorMsg.value = '没连上服务器，检查一下后端起了没'
    novels.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ===== 筛选动作 =====
const filterByCategory = (cat) => {
  if (selectedCategory.value === cat) return
  selectedCategory.value = cat
  pageNum.value = 1
  loadNovels()
}

const switchStatus = (val) => {
  if (status.value === val) return
  status.value = val
  pageNum.value = 1
  loadNovels()
}

/** 搜索框在导航栏里（SiteHeader 的 search 事件）*/
const handleSearch = (kw) => {
  keyword.value = kw || ''
  pageNum.value = 1
  loadNovels()
}

const resetFilter = () => {
  selectedCategory.value = '全部'
  status.value = ''
  keyword.value = ''
  pageNum.value = 1
  loadNovels()
}

// ===== 跳转 =====
const goLogin = () => router.push('/login')
const goWrite = () => router.push('/write')
const goDetail = (id) => router.push(`/novels/${id}`)

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

// ===== 管理员删除 =====
const handleDelete = (id) => {
  ElMessageBox.confirm('删除后无法恢复，确定删掉这本小说？', '危险操作', {
    confirmButtonText: '删除',
    cancelButtonText: '再想想',
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteNovel(id)
        if (res.data.code === 200) {
          ElMessage.success('已删除')
          loadNovels()
        } else {
          ElMessage.error(res.data.msg || '删除失败')
        }
      } catch (e) {
        console.error('[书城] 删除失败:', e)
        ElMessage.error('删除失败，检查一下权限或网络')
      }
    })
    .catch(() => {})
}

// ★ 从发现页 / 书单页的搜索框跳过来会带 ?keyword=xxx
//   （那边搜索不了，统一把关键词甩到书城，因为只有这儿接的是真搜索）
watch(
  () => route.query.keyword,
  (kw) => {
    if (!kw) return
    keyword.value = String(kw)
    pageNum.value = 1
    loadNovels()
  },
  { immediate: true }
)

onMounted(() => {
  loadCategories()
  loadRanking()
  // ★ 如果 URL 上带了 keyword，上面那个 watch 已经先加载过一次了，别重复请求
  if (!route.query.keyword) loadNovels()
})
</script>

<style scoped>
/* ============================================================
   书城（原首页）
   ------------------------------------------------------------
   骨架照起点：左分类树 + 中间推荐位 + 右人气榜，往下是"本周强推 / 全部作品"。
   但**配色沿用草莓这套**（近黑字 + 米白底 + 细线 + 低饱和），不是起点那种红黑。
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

/* 导航栏右侧那两行小字（塞在 SiteHeader 插槽里，所以样式留在本页）*/
.welcome {
  color: var(--ink-3);
  font-size: var(--fs-sm);
  white-space: nowrap;
}

/* ===== 顶部三栏 ===== */
.hero {
  display: grid;
  grid-template-columns: 196px 1fr 250px;
  gap: 16px;
  padding: 24px 0 0;
}

/* 分类树 */
.tree {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 6px 0;
  overflow: hidden;
  align-self: start;
}
.tree .hd {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--ink-3);
  padding: 10px 16px 12px;
  border-bottom: 1px solid var(--line-2);
  letter-spacing: 1px;
}
.tree a {
  display: flex;
  justify-content: space-between;
  padding: 9px 16px;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all var(--t-fast);
}
.tree a span:last-child {
  color: var(--ink-4);
  font-size: var(--fs-xs);
}
.tree a:hover {
  background: var(--bg-page);
  color: var(--brand);
}
.tree a.on {
  background: var(--bg-page);
  color: var(--brand);
  font-weight: 600;
}

/* 推荐位：深墨色大卡（不炫）*/
.feature {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  min-height: 352px;
  padding: 38px 40px;
  background: linear-gradient(135deg, #2b3242, #413a56);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  cursor: pointer;
}
.feature .info {
  position: relative;
  z-index: 1;
  max-width: 56%;
}
.feature .tag {
  display: inline-block;
  font-size: var(--fs-xs);
  padding: 3px 12px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.14);
  margin-bottom: 14px;
  letter-spacing: 1px;
}
.feature h2 {
  font-size: var(--fs-3xl);
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.feature p {
  font-size: 13.5px;
  opacity: 0.8;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 18px;
}
.feature .meta {
  font-size: 12.5px;
  opacity: 0.7;
  margin-bottom: 20px;
}
.feature .btn {
  background: #fff;
  color: #2b3242;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 26px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.big-cover {
  position: relative;
  z-index: 1;
  width: 132px;
  height: 184px;
  border-radius: 6px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: var(--fs-md);
  font-weight: 600;
  letter-spacing: 2px;
  padding: 12px;
  overflow: hidden;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
}
.big-cover img,
.rec .cover img,
.book .cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 人气榜 */
.rank {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 6px 0;
  overflow: hidden;
  align-self: start;
}
.rank .hd {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line-2);
}
.rank .hd b {
  font-size: 13.5px;
  color: var(--ink);
}
.rank .hd a {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  cursor: pointer;
}
.rank .hd a:hover {
  color: var(--brand);
}
.rk {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 16px;
  font-size: var(--fs-sm);
  cursor: pointer;
}
.rk:hover {
  background: var(--bg-page);
}
.rk .no {
  width: 16px;
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--ink-4);
  flex-shrink: 0;
}
.rk .no.top {
  color: var(--warn);
}
.rk .t {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink-2);
}
.rk .h {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.rk-empty {
  padding: 16px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
}

/* ===== 区块标题 ===== */
.sec-hd {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 34px 0 16px;
}
.sec-hd h3 {
  font-size: var(--fs-lg);
  font-weight: 700;
  letter-spacing: 0.3px;
  color: var(--ink);
}
.sec-hd .total {
  font-size: var(--fs-sm);
  color: var(--ink-3);
}

/* ===== 本周强推 ===== */
.rec-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.rec {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 12px;
  box-shadow: var(--shadow);
  cursor: pointer;
  transition: box-shadow var(--t-base);
}
.rec:hover {
  box-shadow: var(--shadow-hover);
}
.rec .cover {
  width: 62px;
  height: 86px;
  border-radius: 5px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 6px;
  line-height: 1.35;
  overflow: hidden;
}
.rec .info {
  min-width: 0;
}
.rec .cat {
  font-size: 11px;
  color: var(--brand);
  background: var(--line-2);
  padding: 1px 8px;
  border-radius: 3px;
}
.rec h4 {
  font-size: var(--fs-base);
  font-weight: 600;
  margin: 6px 0 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink);
}
.rec .au {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.rec .d {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  margin-top: 5px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 状态筛选 ===== */
.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.filters a {
  font-size: var(--fs-sm);
  padding: 6px 15px;
  border-radius: var(--radius-full);
  background: #fff;
  color: var(--ink-2);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: all var(--t-base);
}
.filters a:hover {
  border-color: var(--ink-4);
  color: var(--ink);
}
.filters a.on {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
  font-weight: 600;
}

/* ===== 作品网格 ===== */
.grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 24px 16px;
}
.book {
  cursor: pointer;
}
.book .cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: var(--fs-xs);
  font-weight: 600;
  padding: 8px;
  line-height: 1.4;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 12px rgba(20, 20, 20, 0.1);
  transition: transform var(--t-base), box-shadow var(--t-base);
  position: relative;
  overflow: hidden;
}
.book:hover .cover {
  transform: translateY(-4px);
  box-shadow: 0 12px 26px rgba(20, 20, 20, 0.18);
}
.book h4 {
  font-size: 13.5px;
  font-weight: 600;
  margin: 9px 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink);
}
.book:hover h4 {
  color: var(--brand);
}
.book .au {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 连载/完结角标 */
.book .cover .st {
  position: absolute;
  top: 6px;
  left: 6px;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  letter-spacing: 0.5px;
}
.book .cover .st.fin {
  background: rgba(47, 143, 111, 0.85);
}
.book .admin {
  margin-top: 6px;
}

/* ===== 分页 ===== */
.pager {
  display: flex;
  justify-content: center;
  padding: 36px 0 72px;
}
/* 轻量对齐设计稿的调子（不重写分页器，Element 那套逻辑已经够用）*/
.pager :deep(.el-pagination) {
  --el-pagination-font-size: var(--fs-sm);
  --el-pagination-button-color: var(--ink-2);
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

/* ===== 窄屏 ===== */
@media (max-width: 980px) {
  .hero {
    grid-template-columns: 1fr;
  }
  /* ★ 分类树和人气榜在手机上先撤掉：树占满一屏高，榜是次要信息。
     分类筛选手机上暂时没法用（后面可以在顶部做个下拉） */
  .tree,
  .rank {
    display: none;
  }
  .rec-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 600px) {
  .container {
    padding: 0 16px;
  }
  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px 14px;
  }
  .feature {
    padding: 26px 22px;
    flex-direction: column;
    align-items: flex-start;
  }
  .feature .info {
    max-width: 100%;
  }
  .big-cover {
    display: none;
  }
  .welcome {
    display: none;
  }
}
</style>
