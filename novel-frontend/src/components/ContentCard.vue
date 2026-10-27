<template>
  <!-- 整张卡点一下进书详情；头像 / 展开按钮 / 关联书各自拦一下（.stop） -->
  <article class="post" @click="goNovel">
    <!-- 第一行：谁写的 + 什么时候 + 态度 -->
    <div class="ph">
      <span class="av" :class="coverColor(authorName)" @click.stop="goAuthor">
        {{ authorName[0] || '佚' }}
      </span>
      <div class="who">
        <span class="nm">
          {{ authorName }}
          <!-- ★ D2：AI 生成的内容必须显式标注身份，卡片上就要看得见 -->
          <span v-if="isAi" class="ai-tag">AI</span>
        </span>
        <span class="tm">{{ timeText }}</span>
      </div>
      <span class="pol" :class="polarity.cls">{{ polarity.text }}</span>
    </div>

    <h3>{{ content.title }}</h3>

    <!-- 正文默认 3 行；真被截断了才出现「展开全文」（用高度判断，不靠字数猜） -->
    <p ref="textEl" class="txt" :class="{ open: expanded }">{{ content.text }}</p>
    <button v-if="canExpand" class="expand" @click.stop="toggleExpand">
      {{ expanded ? '收起' : '展开全文' }}
    </button>

    <!-- 关联书：这是用户从书评走到书详情的路，不能省 -->
    <div class="ref" @click.stop="goNovel">
      <div v-if="coverUrl" class="cover">
        <img :src="coverUrl" alt="封面" />
      </div>
      <div v-else class="cover" :class="coverColor(novelTitle)">{{ novelTitle }}</div>
      <div class="info">
        <b>{{ novelTitle }}</b>
        <span>{{ novelMeta }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { relativeTime } from '../utils/format'
import { coverColor } from '../utils/cover'

const props = defineProps({
  content: { type: Object, required: true }
})

const router = useRouter()

// ===== 后端枚举 → 页面文案 =====
// Polarity：REC 推荐 | WARN 避雷 | NOTE 长评
const POLARITY = {
  REC: { text: '推荐', cls: 'rec' },
  WARN: { text: '避雷', cls: 'warn' },
  NOTE: { text: '长评', cls: 'note' }
}
// Grade：GOD 仙草 | GOOD 粮草 | OK 干草 | BAD 毒草（可空）
const GRADES = {
  GOD: '仙草',
  GOOD: '粮草',
  OK: '干草',
  BAD: '毒草'
}

const polarity = computed(
  () => POLARITY[props.content?.polarity] || { text: '书评', cls: 'note' }
)
const authorName = computed(() => props.content?.author?.username || '佚名')
const isAi = computed(() => props.content?.author?.isAi === true)
const timeText = computed(() => relativeTime(props.content?.createTime))

// 后端可能查不到作者 / 书（数据不完整），这里全部兜底，别让页面白屏
const novel = computed(() => props.content?.novel || {})
const novelTitle = computed(() => novel.value.title || '未知作品')
const coverUrl = computed(() => novel.value.coverUrl || novel.value.coverUr1 || '')
const novelMeta = computed(() => {
  const grade = GRADES[props.content?.grade]
  return [novel.value.author, novel.value.category, grade].filter(Boolean).join(' · ')
})

// ===== 展开全文 =====
// ★ 判断依据是「真的被裁掉了」：scrollHeight > clientHeight。
//   按字数猜（比如超过 100 字就显示按钮）在窄屏上会出错。
const expanded = ref(false)
const canExpand = ref(false)
const textEl = ref(null)

const measure = () => {
  // 展开状态下不测量：这时已经不被裁切，测量会把按钮误判成"不需要了"
  if (!textEl.value || expanded.value) return
  canExpand.value = textEl.value.scrollHeight > textEl.value.clientHeight + 1
}

onMounted(async () => {
  await nextTick()
  measure()
  window.addEventListener('resize', measure)
})
onBeforeUnmount(() => window.removeEventListener('resize', measure))

// 换了一页 / 换了一条内容时，重置展开状态
watch(
  () => props.content?.id,
  async () => {
    expanded.value = false
    await nextTick()
    measure()
  }
)

const toggleExpand = async () => {
  expanded.value = !expanded.value
  if (!expanded.value) {
    await nextTick()
    measure()
  }
}

// ===== 跳转 =====
const goNovel = () => {
  if (novel.value.id) router.push(`/novels/${novel.value.id}`)
}
const goAuthor = () => {
  if (props.content?.author?.id) router.push(`/author/${props.content.author.id}`)
}
</script>

<style scoped>
/* ============================================================
   书评卡片 —— 首页 / 书详情 / 书评人主页三处复用
   ------------------------------------------------------------
   字段全部来自后端 ContentVO。后端没返回的东西（字数、评分、
   点赞数、雷点标签）这里就不显示 —— 宁可少一块，不编数据。
   ============================================================ */
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

/* ===== 第一行 ===== */
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
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.3;
  color: var(--ink);
}
.ph .tm {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* AI 身份标：中性灰底 + 小字。不抢视觉，但必须看得见 */
.ai-tag {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--line-2);
  color: var(--ink-3);
}

/* 态度徽章：推荐 / 避雷 / 长评 */
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

/* ===== 标题 / 正文 ===== */
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
  white-space: pre-wrap;
  word-break: break-word;
}
/* 展开后不再裁切（display 要一起改，否则 -webkit-line-clamp 仍然生效） */
.post .txt.open {
  display: block;
  overflow: visible;
}

.expand {
  display: block;
  margin: -8px 0 12px;
  padding: 0;
  border: none;
  background: none;
  color: var(--brand);
  font-size: var(--fs-xs);
  font-family: inherit;
  cursor: pointer;
}
.expand:hover {
  color: var(--brand-deep);
}

/* ===== 关联书 ===== */
.ref {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  cursor: pointer;
}
.ref:hover .info b {
  color: var(--brand);
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
  overflow: hidden;
}
.ref .cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  transition: color var(--t-fast);
}
.ref .info span {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* ===== 窄屏 ===== */
@media (max-width: 600px) {
  .post {
    padding: 16px;
  }
}
</style>
