<template>
  <div class="page">
    <SiteHeader show-search search-placeholder="搜书单、书、人" @search="onSearch" />

    <div class="container">
      <!-- ===== 页头 ===== -->
      <div class="head">
        <div>
          <h1>书单</h1>
          <p>有人替你读过、挑过、编好了顺序 —— 照着读就行</p>
        </div>
        <button class="btn-new" @click="onCreate">＋ 创建书单</button>
      </div>

      <!-- ===== 筛选项 ===== -->
      <div class="chips">
        <a
          v-for="tab in BOOKLIST_TABS"
          :key="tab"
          :class="{ on: activeTab === tab }"
          @click="activeTab = tab"
        >
          {{ tab }}
        </a>
      </div>

      <!-- ===== 本周精选（大专题）===== -->
      <!-- ★ 只在"全部"标签下显示：切到某个分类还顶着一个精选，会很乱 -->
      <div v-if="activeTab === '全部' && featured" class="featured" @click="openList(featured)">
        <div class="ftxt">
          <span class="ftag">{{ featured.tag }}</span>
          <h2>{{ featured.title }}</h2>
          <p>{{ featured.desc }}</p>
          <div class="fmeta">
            <span class="av" :class="coverColor(curatorName(featured))">
              {{ curatorName(featured)[0] }}
            </span>
            <span>
              {{ curatorName(featured) }} 整理 · 收录 {{ featured.count }} 本 ·
              被收藏 {{ featured.saved }} 次
            </span>
          </div>
        </div>
        <div class="band">
          <div
            v-for="b in featuredBooks"
            :key="b.id"
            class="cover"
            :class="coverColor(b.title)"
          >{{ b.title }}</div>
        </div>
      </div>

      <!-- ===== 书单墙 ===== -->
      <!-- ★ 封面带放在最上面：先让你看到"这个书单里装了哪几本书"，
           再决定要不要看标题 —— 这是书单跟书城货架的区别 -->
      <div v-if="shownLists.length > 0" class="wall">
        <div v-for="list in shownLists" :key="list.id" class="bl" @click="openList(list)">
          <div class="band">
            <div
              v-for="b in booksOf(list).slice(0, 4)"
              :key="b.id"
              class="cover"
              :class="coverColor(b.title)"
            >{{ b.title }}</div>
            <div v-if="list.count > 4" class="more">+{{ list.count - 4 }}</div>
          </div>

          <h3>{{ list.title }}</h3>
          <div class="desc">{{ list.desc }}</div>

          <div class="meta">
            <span class="av" :class="coverColor(curatorName(list))">
              {{ curatorName(list)[0] }}
            </span>
            <span>{{ curatorName(list) }}</span>
            <span>·</span>
            <span>收录 {{ list.count }} 本</span>
            <span class="pol" :class="list.polarity">
              {{ list.polarity === 'warn' ? '避雷单' : '推荐单' }}
            </span>
          </div>
        </div>
      </div>

      <!-- ===== 空态 =====
           ★ 真能走到：点"入门必读 / 完结不烂尾 / 冷门佳作"这些标签就是空的。
             以前这种时候页面会一片白，用户不知道是加载失败还是真没有。 -->
      <div v-else class="empty">
        <div class="empty-emoji">🍓</div>
        <p>这个分类下还没有书单</p>
        <p class="empty-sub">换一个标签，或者自己整一个</p>
        <button class="empty-btn" @click="activeTab = '全部'">看全部书单</button>
      </div>

      <p class="demo-note">
        本页书单是<strong>演示数据</strong>：后端的书单接口还没做。
        接口需求写在 docs/设计/前端改版-设计方案.txt
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SiteHeader from '../components/SiteHeader.vue'
import { BOOKLISTS, BOOKLIST_TABS, bookById, authorById, coverColor } from '../mock/data'

const router = useRouter()

// ===== 筛选 =====
const activeTab = ref('全部')

/** 本周精选（大专题）单独拎出来，别混在墙里 */
const featured = computed(() => BOOKLISTS.find((l) => l.featured) || null)

/** 墙里展示的书单：精选那块已经在上面了，这里就不再重复 */
const shownLists = computed(() => {
  const base = BOOKLISTS.filter((l) => !l.featured)
  const t = activeTab.value
  if (t === '全部') return base
  if (t === '推荐单') return base.filter((l) => l.polarity === 'rec')
  if (t === '避雷单') return base.filter((l) => l.polarity === 'warn')
  // 剩下三个标签（入门必读 / 完结不烂尾 / 冷门佳作）目前确实没有内容 -->
  // 走空态。等后端有书单表了，这里按标签筛。
  return base.filter((l) => (l.tags || []).includes(t))
})

// ===== 取关联数据 =====
const booksOf = (list) => list.bookIds.map((id) => bookById(id)).filter(Boolean)
const curatorName = (list) => (authorById(list.curatorId) || { name: '佚名' }).name

const featuredBooks = computed(() => (featured.value ? booksOf(featured.value) : []))

// ===== 动作 =====
// 点书单 → 目前没有书单详情页，先提示一下（后端没有书单表）
const openList = (list) => {
  ElMessage.info(`「${list.title}」的详情页还没做 —— 后端还没有书单接口`)
}

const onCreate = () => {
  ElMessage.info('创建书单还没做 —— 后端还没有书单表')
}

// 搜索：书单页自己不搜，带着关键词跳到书城（那边接的是真接口）
const onSearch = (keyword) => {
  router.push(keyword ? { path: '/store', query: { keyword } } : '/store')
}
</script>

<style scoped>
/* ============================================================
   书单页 · 策展版
   ------------------------------------------------------------
   书城已经是"货架"（按分类找），所以书单不再做成货架 —— 它是**策展**：
   卡片顶部先给一条封面带，让你一眼看到里面装了哪几本书。
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
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 38px 0 18px;
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
.btn-new {
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 11px 24px;
  font-size: var(--fs-base);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
  transition: opacity var(--t-base);
}
.btn-new:hover {
  opacity: 0.85;
}

/* ===== 筛选 ===== */
.chips {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  padding-bottom: 22px;
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
.chips a.on {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
  font-weight: 600;
}

/* ===== 本周精选 ===== */
.featured {
  display: flex;
  gap: 26px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 26px;
  margin: 26px 0 8px;
  cursor: pointer;
  transition: box-shadow var(--t-base);
  align-items: center;
}
.featured:hover {
  box-shadow: var(--shadow-hover);
}
.featured .ftxt {
  flex: 1;
  min-width: 0;
}
.featured .ftag {
  display: inline-block;
  font-size: var(--fs-xs);
  letter-spacing: 1px;
  color: var(--warn);
  background: var(--warn-bg);
  padding: 3px 11px;
  border-radius: var(--radius-full);
  margin-bottom: 12px;
}
.featured h2 {
  font-size: var(--fs-2xl);
  font-weight: 700;
  letter-spacing: 0.3px;
  line-height: 1.45;
  margin-bottom: 10px;
  color: var(--ink);
}
.featured p {
  font-size: var(--fs-base);
  color: var(--ink-2);
  margin-bottom: 14px;
}
.featured .fmeta {
  font-size: 12.5px;
  color: var(--ink-3);
  display: flex;
  align-items: center;
  gap: 8px;
}
.featured .fmeta .av {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.featured .band {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.featured .band .cover {
  width: 68px;
  height: 96px;
}

/* ===== 书单墙 ===== */
.wall {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 18px 0 0;
}
.bl {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 18px 18px 16px;
  cursor: pointer;
  transition: box-shadow var(--t-base);
}
.bl:hover {
  box-shadow: var(--shadow-hover);
}
.bl:hover h3 {
  color: var(--brand);
}

/* ★ 封面带 */
.band {
  display: flex;
  gap: 7px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.band .cover {
  width: 46px;
  height: 64px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: 0.4px;
  padding: 5px;
  overflow: hidden;
}
.band .more {
  width: 46px;
  height: 64px;
  border-radius: 4px;
  border: 1px dashed var(--ink-4);
  color: var(--ink-3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-xs);
  font-weight: 600;
}

.bl h3 {
  font-size: var(--fs-md);
  font-weight: 700;
  letter-spacing: 0.2px;
  margin-bottom: 7px;
  line-height: 1.5;
  transition: color var(--t-base);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--ink);
}
.bl .desc {
  font-size: var(--fs-sm);
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}
.bl .meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-xs);
  color: var(--ink-3);
}
.bl .meta .av {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}
.bl .meta .pol {
  margin-left: auto;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
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

/* ===== 空态 ===== */
.empty {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 60px 20px;
  text-align: center;
  margin-top: 18px;
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
  padding: 24px 0 60px;
  line-height: 1.8;
}
.demo-note strong {
  color: var(--ink-3);
}

/* ===== 窄屏 ===== */
@media (max-width: 900px) {
  .wall {
    grid-template-columns: 1fr;
  }
  .featured {
    flex-direction: column;
    align-items: flex-start;
  }
}
@media (max-width: 600px) {
  .container {
    padding: 0 16px;
  }
  .head {
    flex-direction: column;
    align-items: flex-start;
  }
  .head h1 {
    font-size: var(--fs-2xl);
  }
  .featured .band .cover {
    width: 56px;
    height: 80px;
  }
}
</style>
