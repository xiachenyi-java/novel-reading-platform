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

      <!-- ===== 筛选项 ===== -->
      <div class="chips">
        <a
          v-for="tab in FEED_TABS"
          :key="tab"
          :class="{ on: activeTab === tab }"
          @click="activeTab = tab"
        >
          {{ tab }}
        </a>
      </div>

      <div class="layout">
        <!-- ============ 内容流 ============ -->
        <div class="feed">
          <div
            v-for="post in filteredPosts"
            :key="post.id"
            class="post"
            @click="goBook(post.bookId)"
          >
            <!-- 头：谁写的 + 什么时候 + 这条是什么性质 -->
            <div class="ph">
              <!-- ★ 点头像去作者页，别让整张卡跳走（.stop 就是干这个的）
                   ★ 头像底色沿用封面配色：同一个人在他自己的主页、在书评里，
                     颜色都一样 -->
              <span
                class="av"
                :class="coverColor(authorOf(post).name)"
                @click.stop="goAuthor(post.authorId)"
              >{{ authorOf(post).name[0] }}</span>
              <div class="who">
                <span class="nm">{{ authorOf(post).name }}</span>
                <span class="tm">{{ timeAgo(post.ago) }}</span>
              </div>
              <span class="pol" :class="POLARITY[post.polarity].cls">
                {{ POLARITY[post.polarity].text }}
              </span>
            </div>

            <h3>{{ post.title }}</h3>
            <p class="txt">{{ post.text }}</p>

            <!-- 卡片里引用的那本书 -->
            <div class="ref">
              <div class="cover" :class="coverColor(bookOf(post).title)">
                {{ bookOf(post).title }}
              </div>
              <div class="info">
                <b>{{ bookOf(post).title }}</b>
                <span>
                  {{ bookOf(post).author }} · {{ bookOf(post).category }} ·
                  {{ bookOf(post).words }} 万字
                </span>
              </div>
              <div class="score">
                {{ bookOf(post).score }}
                <small>综合评分</small>
              </div>
            </div>

            <!-- 雷点 / 亮点标签 -->
            <div class="marks">
              <span v-for="m in post.marks" :key="m.t" :class="m.k === 'warn' ? 'w' : 'g'">
                {{ m.t }}
              </span>
            </div>

            <div class="act">
              <span>赞 {{ post.likes.toLocaleString() }}</span>
              <span>评论 {{ post.comments }}</span>
              <span>收藏</span>
            </div>
          </div>

          <!-- ===== 空态 =====
               ★ 这个状态是**真能走到的**：点"历史""仙侠"这些标签，
                 内容流里暂时没有对应分类的内容，就会落到这里。
                 不是摆着好看的。 -->
          <div v-if="filteredPosts.length === 0" class="empty">
            <div class="empty-emoji">🍓</div>
            <p>这个标签下还没人写过</p>
            <p class="empty-sub">换个标签看看，或者你来写第一条</p>
            <button class="empty-btn" @click="activeTab = '全部'">看全部</button>
          </div>
        </div>

        <!-- ============ 右栏 ============ -->
        <aside class="side">
          <div class="card">
            <div class="card-hd"><span>本月避雷榜</span></div>
            <div v-for="(r, i) in AVOID_RANK" :key="r.title" class="rk">
              <span class="no">{{ i + 1 }}</span>
              <span class="t">{{ r.title }}</span>
              <span class="m">{{ r.mark }}</span>
            </div>
          </div>

          <div class="card">
            <div class="card-hd"><span>口碑推荐</span></div>
            <div v-for="(r, i) in PRAISE_RANK" :key="r.title" class="rk">
              <span class="no">{{ i + 1 }}</span>
              <span class="t">{{ r.title }}</span>
              <span class="m rec">{{ r.score }}</span>
            </div>
          </div>

          <div class="card">
            <div class="card-hd"><span>热门雷点</span></div>
            <div class="chips-side">
              <span v-for="p in HOT_PITS" :key="p">{{ p }}</span>
            </div>
          </div>
        </aside>
      </div>

      <!-- 假数据说明 —— ★ 不能让演示数据看起来像真数据 -->
      <p class="demo-note">
        本页的推荐 / 书评 / 避雷是<strong>演示数据</strong>：后端的"内容流"接口还没做。
        接口需求写在 docs/设计/前端改版-设计方案.txt
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import SiteHeader from '../components/SiteHeader.vue'
import {
  POSTS,
  FEED_TABS,
  AVOID_RANK,
  PRAISE_RANK,
  HOT_PITS,
  bookById,
  authorById,
  coverColor,
  timeAgo
} from '../mock/data'

const router = useRouter()

// ===== 三个极性：推荐 / 避雷 / 长评（配色见 tokens.css 的 --rec / --warn）=====
const POLARITY = {
  rec: { text: '推荐', cls: 'rec' },
  warn: { text: '避雷', cls: 'warn' },
  note: { text: '长评', cls: 'note' }
}

// ===== 筛选 =====
const activeTab = ref('全部')

/**
 * 筛选后的内容流。
 * 前四个标签按"性质"筛，后面几个按"分类"筛（看这条内容讲的书属于哪一类）。
 */
const filteredPosts = computed(() => {
  const t = activeTab.value
  if (t === '全部') return POSTS
  if (t === '推荐') return POSTS.filter((p) => p.polarity === 'rec')
  if (t === '书评') return POSTS.filter((p) => p.polarity === 'note')
  if (t === '避雷') return POSTS.filter((p) => p.polarity === 'warn')
  return POSTS.filter((p) => bookById(p.bookId)?.category === t)
})

// ===== 取关联数据 =====
const authorOf = (post) => authorById(post.authorId) || { name: '佚名', id: 0 }
const bookOf = (post) => bookById(post.bookId) || { title: '未知', author: '', category: '', words: 0, score: '-' }

// ===== 跳转 =====
const goBook = (bookId) => router.push(`/novels/${bookId}`)
const goAuthor = (authorId) => router.push(`/author/${authorId}`)

// 搜索：发现页自己不搜，直接带着关键词跳到书城页（那边接的是真接口）
const onSearch = (keyword) => {
  router.push(keyword ? { path: '/store', query: { keyword } } : '/store')
}
</script>

<style scoped>
/* ============================================================
   发现页（首页）
   ------------------------------------------------------------
   设定：人和 AI 写的内容**混在一起**，这里不区分身份（点进作者页才知道）。
   颜色收着来：近黑字 + 米白底 + 一个主色 + 两个低饱和语义色。
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

/* ===== 两栏 ===== */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  padding: 24px 0 40px;
  align-items: start;
}

/* ===== 内容卡 ===== */
.post {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  box-shadow: var(--shadow);
  margin-bottom: 16px;
  transition: box-shadow var(--t-base);
  cursor: pointer;
}
.post:hover {
  box-shadow: var(--shadow-hover);
}

.ph {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.av {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--fs-base);
  font-weight: 700;
  flex-shrink: 0;
  cursor: pointer;
}
.ph .who {
  min-width: 0;
}
.ph .nm {
  font-size: var(--fs-base);
  font-weight: 600;
  display: block;
  line-height: 1.3;
  color: var(--ink);
}
.ph .tm {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* 极性标签：推荐 / 避雷 / 长评 */
.pol {
  margin-left: auto;
  font-size: var(--fs-xs);
  font-weight: 600;
  padding: 3px 11px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
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

.post h3 {
  font-size: var(--fs-lg);
  font-weight: 700;
  letter-spacing: 0.2px;
  margin-bottom: 8px;
  line-height: 1.5;
  color: var(--ink);
}
.post .txt {
  font-size: var(--fs-base);
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
}

/* 卡片里引用的那本书 */
.ref {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
.ref .cover {
  width: 44px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  text-align: center;
  padding: 4px;
  line-height: 1.3;
  letter-spacing: 0.5px;
}
.ref .info {
  min-width: 0;
  flex: 1;
}
.ref .info b {
  font-size: var(--fs-base);
  font-weight: 600;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink);
}
.ref .info span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.ref .score {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  flex-shrink: 0;
}
.ref .score small {
  display: block;
  font-size: 10px;
  color: var(--ink-3);
  font-weight: 400;
  text-align: right;
}

/* 雷点 / 亮点 标签 */
.marks {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.marks span {
  font-size: var(--fs-xs);
  padding: 3px 10px;
  border-radius: 4px;
}
.marks .w {
  color: var(--warn);
  background: var(--warn-bg);
}
.marks .g {
  color: var(--rec);
  background: var(--rec-bg);
}

/* 互动行 */
.act {
  display: flex;
  gap: 26px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line-2);
  font-size: var(--fs-sm);
  color: var(--ink-3);
}

/* ===== 空态 ===== */
.empty {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 60px 20px;
  text-align: center;
}
.empty-emoji {
  font-size: 56px;
  line-height: 1;
  opacity: 0.3;
}
.empty p {
  margin-top: 12px;
  font-size: var(--fs-md);
  color: var(--ink-3);
}
.empty .empty-sub {
  margin-top: 8px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
}
.empty-btn {
  margin-top: 16px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 24px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.empty-btn:hover {
  opacity: 0.86;
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
.rk {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  font-size: var(--fs-sm);
}
.rk:hover {
  background: var(--bg-page);
}
.rk .no {
  width: 18px;
  color: var(--ink-3);
  font-weight: 700;
  font-size: var(--fs-xs);
  flex-shrink: 0;
}
.rk .t {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--ink-2);
}
.rk .m {
  font-size: var(--fs-xs);
  color: var(--warn);
  flex-shrink: 0;
}
.rk .m.rec {
  color: var(--rec);
  font-weight: 600;
}
.chips-side {
  padding: 14px 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chips-side span {
  font-size: var(--fs-xs);
  padding: 4px 11px;
  border-radius: var(--radius-full);
  background: var(--bg-page);
  color: var(--ink-2);
  cursor: pointer;
}
.chips-side span:hover {
  background: var(--brand);
  color: #fff;
}

/* 演示数据说明 */
.demo-note {
  font-size: var(--fs-xs);
  color: var(--ink-4);
  padding-bottom: 60px;
  line-height: 1.8;
}
.demo-note strong {
  color: var(--ink-3);
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
@media (max-width: 600px) {
  .container {
    padding: 0 16px;
  }
  .head h1 {
    font-size: var(--fs-2xl);
  }
  .post {
    padding: 16px;
  }
}
</style>
