<template>
  <div class="page">
    <SiteHeader show-search @search="onSearch" />

    <!-- ===== 查无此人 =====
         直接改地址栏乱输一个 id 就会走到这儿。
         不留这个分支的话，下面用 author.name 会直接报错、白屏。 -->
    <div v-if="!author" class="container">
      <div class="empty">
        <div class="empty-emoji">🍓</div>
        <p>没找到这个作者</p>
        <p class="empty-sub">地址可能输错了，或者 TA 已经注销</p>
        <button class="empty-btn" @click="router.push('/')">回发现页</button>
      </div>
    </div>

    <template v-else>
      <!-- ===== 主页头 ===== -->
      <section class="profile">
        <div class="profile-inner">
          <div class="av-big" :class="coverColor(author.name)">{{ author.name[0] }}</div>

          <div class="pinfo">
            <div class="pname">
              {{ author.name }}
              <!-- ★ 身份标记：设计稿刻意做得很轻（描边小标），
                   是"彩蛋"不是"标签" —— 首页和内容流里都不显示它 -->
              <span v-if="author.ai" class="idtag">AI</span>
            </div>
            <div class="pbio">{{ author.bio }}</div>
            <div class="areas">
              <span v-for="a in author.areas" :key="a">{{ a }}</span>
            </div>
            <div class="pmeta">
              <b>{{ author.follows }}</b> 关注 · <b>{{ author.posts }}</b> 篇内容 ·
              被收藏 <b>{{ author.saved }}</b> 次
            </div>
          </div>

          <button class="follow" @click="onFollow">关注</button>
        </div>

        <!-- 内容分栏 -->
        <div class="ptabs">
          <a
            v-for="tab in TABS"
            :key="tab.key"
            :class="{ on: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }} {{ countOf(tab.key) }}
          </a>
        </div>
      </section>

      <div class="container">
        <div class="layout">
          <!-- ===== 内容 ===== -->
          <div>
            <div
              v-for="post in shownPosts"
              :key="post.id"
              class="post"
              @click="router.push(`/novels/${post.bookId}`)"
            >
              <span class="pol" :class="POLARITY[post.polarity].cls">
                {{ POLARITY[post.polarity].text }}
              </span>
              <h3>{{ post.title }}</h3>
              <p class="txt">{{ post.text }}</p>
              <div class="pfoot">
                <span class="bk">《{{ bookOf(post).title }}》· {{ bookOf(post).author }}</span>
                <span>赞 {{ post.likes.toLocaleString() }}</span>
                <span>评论 {{ post.comments }}</span>
              </div>
            </div>

            <!-- 空态：这个分栏下 TA 还没写过东西 -->
            <div v-if="shownPosts.length === 0" class="empty">
              <div class="empty-emoji">🍓</div>
              <p>这个分栏下 TA 还没写过</p>
              <p class="empty-sub">换个分栏看看</p>
              <button class="empty-btn" @click="activeTab = 'all'">看全部</button>
            </div>
          </div>

          <!-- ===== 右栏 ===== -->
          <aside class="side">
            <div class="card">
              <div class="card-hd">相似的作者</div>
              <div
                v-for="a in similar"
                :key="a.id"
                class="who-row"
                @click="router.push(`/author/${a.id}`)"
              >
                <span class="av" :class="coverColor(a.name)">{{ a.name[0] }}</span>
                <div class="info">
                  <b>{{ a.name }}</b>
                  <span>{{ a.bio }}</span>
                </div>
                <!-- 人写的内容没有 AI 小标 —— 人和 AI 混在一起才是这个产品的样子 -->
                <span v-if="a.ai" class="mini-tag">AI</span>
              </div>
            </div>

            <div class="card">
              <div class="card-hd">TA 常提的书</div>
              <div
                v-for="b in mentionedBooks"
                :key="b.id"
                class="who-row"
                @click="router.push(`/novels/${b.id}`)"
              >
                <span class="av cover-sm" :class="coverColor(b.title)">{{ b.title[0] }}</span>
                <div class="info">
                  <b>{{ b.title }}</b>
                  <span>{{ b.author }} · {{ b.score }} 分</span>
                </div>
              </div>
              <div v-if="mentionedBooks.length === 0" class="who-empty">
                TA 还没提过哪本书
              </div>
            </div>
          </aside>
        </div>

        <p class="demo-note">
          本页作者 / 关注数 / 内容都是<strong>演示数据</strong>：
          后端的作者体系和关注关系还没做（现在只有 user 表）。
          接口需求写在 docs/设计/前端改版-设计方案.txt
        </p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SiteHeader from '../components/SiteHeader.vue'
import {
  authorById,
  otherAuthors,
  postsOfAuthor,
  bookById,
  coverColor
} from '../mock/data'

const route = useRoute()
const router = useRouter()

// ★ 用 computed：从"相似作者"点来点去时，路由变了这里会自动重算，
//   不用监听路由手动刷新（那种写法很容易漏掉一次）
const author = computed(() => authorById(route.params.id))

// ===== 三个极性 =====
const POLARITY = {
  rec: { text: '推荐', cls: 'rec' },
  warn: { text: '避雷', cls: 'warn' },
  note: { text: '长评', cls: 'note' }
}

// ===== 分栏 =====
const TABS = [
  { key: 'all', label: '全部' },
  { key: 'rec', label: '推荐' },
  { key: 'warn', label: '避雷' }
]
const activeTab = ref('all')

const allPosts = computed(() => (author.value ? postsOfAuthor(author.value.id) : []))

const shownPosts = computed(() => {
  if (activeTab.value === 'all') return allPosts.value
  return allPosts.value.filter((p) => p.polarity === activeTab.value)
})

/** 分栏后面的数字（"全部 127"里那个 127）。★ 用假数据，别假装是真统计 */
const countOf = (key) => {
  if (key === 'all') return allPosts.value.length
  return allPosts.value.filter((p) => p.polarity === key).length
}

// ===== 右栏 =====
const similar = computed(() => (author.value ? otherAuthors(author.value.id) : []))

/** TA 提过的书（去重）*/
const mentionedBooks = computed(() => {
  const seen = new Set()
  const out = []
  allPosts.value.forEach((p) => {
    if (seen.has(p.bookId)) return
    seen.add(p.bookId)
    const b = bookById(p.bookId)
    if (b) out.push(b)
  })
  return out
})

const bookOf = (post) => bookById(post.bookId) || { title: '未知', author: '' }

// ===== 动作 =====
const onFollow = () => {
  // 后端没有关注关系（只有 user / novel / chapter / bookshelf 四张表）
  ElMessage.info('关注还没做 —— 后端还没有关注关系这张表')
}

const onSearch = (keyword) => {
  router.push(keyword ? { path: '/store', query: { keyword } } : '/store')
}
</script>

<style scoped>
/* ============================================================
   作者主页
   ------------------------------------------------------------
   首页人和 AI 混排、不标注；**点进来才知道身份**。
   身份标记做得很轻（一个描边小标），不喧宾夺主。
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

/* ===== 主页头 ===== */
.profile {
  background: #fff;
  border-bottom: 1px solid var(--line);
}
.profile-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 34px 24px 0;
  display: flex;
  gap: 22px;
  align-items: flex-start;
}
.av-big {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}
.pinfo {
  flex: 1;
  min-width: 0;
}
.pname {
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
}
/* ★ 身份标记：描边小标，很轻，不抢戏 */
.idtag {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  padding: 2px 9px;
  border-radius: var(--radius-full);
  border: 1px solid var(--ink-4);
  color: var(--ink-3);
}
.pbio {
  font-size: var(--fs-md);
  color: var(--ink-2);
  margin-top: 10px;
}
.pmeta {
  font-size: var(--fs-sm);
  color: var(--ink-3);
  margin-top: 8px;
}
.pmeta b {
  color: var(--ink);
  font-weight: 700;
}
.areas {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.areas span {
  font-size: var(--fs-xs);
  padding: 3px 11px;
  border-radius: var(--radius-full);
  background: var(--bg-page);
  color: var(--ink-2);
  border: 1px solid var(--line);
}
.follow {
  align-self: center;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 26px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  font-family: inherit;
  transition: opacity var(--t-base);
}
.follow:hover {
  opacity: 0.85;
}

/* ===== 分栏 ===== */
.ptabs {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 28px;
  border-bottom: 1px solid var(--line);
}
.ptabs a {
  padding: 16px 0;
  font-size: var(--fs-base);
  color: var(--ink-3);
  border-bottom: 2px solid transparent;
  position: relative;
  top: 1px;
  cursor: pointer;
}
.ptabs a:hover {
  color: var(--ink);
}
.ptabs a.on {
  color: var(--ink);
  font-weight: 600;
  border-bottom-color: var(--ink);
}

/* ===== 两栏 ===== */
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  padding: 24px 0 20px;
  align-items: start;
}

/* ===== 内容卡（跟首页同一套配色，但少了引用书那块）===== */
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
.pol {
  display: inline-block;
  font-size: var(--fs-xs);
  font-weight: 600;
  padding: 3px 11px;
  border-radius: var(--radius-full);
  margin-bottom: 10px;
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
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
}
.pfoot {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.pfoot .bk {
  color: var(--ink-2);
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
.who-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 18px;
  cursor: pointer;
}
.who-row:hover {
  background: var(--bg-page);
}
.who-row .av {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--fs-base);
  font-weight: 700;
}
/* "TA 常提的书"里是方角封面，不是圆头像 */
.who-row .av.cover-sm {
  border-radius: 4px;
  font-size: var(--fs-sm);
}
.who-row .info {
  min-width: 0;
  flex: 1;
}
.who-row .info b {
  font-size: 13.5px;
  font-weight: 600;
  display: block;
  color: var(--ink);
}
.who-row .info span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-tag {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  border: 1px solid var(--ink-4);
  color: var(--ink-3);
  flex-shrink: 0;
}
.who-empty {
  padding: 16px 18px;
  font-size: var(--fs-sm);
  color: var(--ink-4);
}

/* ===== 空态 ===== */
.empty {
  background: #fff;
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
  .container,
  .profile-inner,
  .ptabs {
    padding-left: 16px;
    padding-right: 16px;
  }
  .profile-inner {
    flex-direction: column;
    align-items: flex-start;
  }
  .av-big {
    width: 64px;
    height: 64px;
    font-size: 24px;
  }
  .pname {
    font-size: var(--fs-lg);
  }
}
</style>
