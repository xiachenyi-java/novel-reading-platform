<template>
  <!-- ============================================================
       全站统一的顶部导航栏
       ------------------------------------------------------------
       【为什么要有这个组件】
         改之前：9 个页面各自写了一遍顶部导航（.header + .header-inner），
                内容区宽度还有 1200 / 1300 两种，品牌名也有两套写法。
                结果：从首页点进榜单，导航栏会轻微跳一下。

       【2026-10-15 起：主菜单也搬进来了】
         发现 / 书城 / 书单 / 排行榜 这四个入口，5 个页面都要有。
         放在各个页面里 = 抄 5 遍，改一次要动 5 个文件。
         所以导航直接做进本组件，页面只管自己那块内容。

       【怎么用】
         1) 只要导航栏：
              <SiteHeader />

         2) 还要搜索框（发现 / 书城 / 书单用）：
              <SiteHeader show-search @search="onSearch" />

         3) 右边还要塞东西（用户区 / 按钮）：
              <SiteHeader show-search @search="onSearch">
                <template #right>
                  <el-button size="small">退出</el-button>
                </template>
              </SiteHeader>

       ★ 插槽内容的样式写在"用它的那个页面"里（就近放），不要写在这里。
         这是 Vue 的规矩：插槽内容由父组件管。本文件只管导航栏这个壳。
       ============================================================ -->
  <header class="site-header">
    <div class="header-inner">
      <!-- 左：品牌名 -->
      <div class="header-left">
        <!-- ★ 品牌名写法照设计稿：第一个字用主色，剩下的用近黑。
             不是渐变、不带 emoji —— 整行彩色是"学生作业"的典型特征 -->
        <span class="brand" @click="goHome"><b>草</b>莓</span>

        <!-- 主菜单 -->
        <nav class="nav">
          <router-link
            v-for="item in NAV"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ on: isOn(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <!-- 默认插槽：留给页面塞自己的东西（目前没人用，保留是为了不破坏老页面）-->
        <slot />
      </div>

      <!-- 右：搜索框（可选） + 页面自己塞的内容 -->
      <div class="header-right">
        <label v-if="showSearch" class="search">
          <input
            v-model="keyword"
            :placeholder="searchPlaceholder"
            @keyup.enter="onSearch"
          />
          <button type="button" @click="onSearch">搜索</button>
        </label>

        <slot name="right" />
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

// ===== 主菜单（顺序照设计稿）=====
const NAV = [
  { path: '/', label: '发现' },
  { path: '/store', label: '书城' },
  { path: '/booklists', label: '书单' },
  { path: '/ranking', label: '排行榜' }
]

defineProps({
  /** 要不要显示搜索框 */
  showSearch: { type: Boolean, default: false },
  searchPlaceholder: { type: String, default: '搜书、搜书评、搜人' }
})

const emit = defineEmits(['search'])

const route = useRoute()
const router = useRouter()

// 点品牌名回首页
const goHome = () => router.push('/')

/**
 * 当前页是不是这个入口
 * ★ 首页要"完全相等"才算：不然任何路径都以 / 开头，发现会永远亮着
 */
const isOn = (path) => (path === '/' ? route.path === '/' : route.path.startsWith(path))

// ===== 搜索 =====
const keyword = ref('')
const onSearch = () => emit('search', keyword.value.trim())
</script>

<style scoped>
/* 吸顶导航：往下滚动时始终贴在顶部 */
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

/* 内容区：居中 + 左右留白，最大宽度全站统一 */
.header-inner {
  max-width: var(--container);
  min-height: var(--header-h);
  margin: 0 auto;
  padding: var(--sp-2) var(--sp-5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-5);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 34px;
  min-width: 0; /* 让里面的文字能正常省略，不被撑破 */
}

/* 品牌名：首字用主色，其余近黑 */
.brand {
  font-size: var(--fs-xl);
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
  cursor: pointer;
  color: var(--ink);
}
.brand b {
  color: var(--brand);
}

/* 主菜单 */
.nav {
  display: flex;
  gap: 26px;
}
.nav-link {
  font-size: var(--fs-base);
  color: var(--ink-2);
  text-decoration: none;
  white-space: nowrap;
  transition: color var(--t-base);
}
.nav-link:hover {
  color: var(--brand);
}
.nav-link.on {
  color: var(--ink);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  flex-shrink: 0; /* 窗口变窄时优先保右边，不压缩按钮 */
}

/* 搜索框（模仿设计稿：一条描边 + 右侧灰色按钮）*/
.search {
  display: flex;
  width: 230px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.search input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  padding: 8px 12px;
  font-size: var(--fs-sm);
  background: transparent;
  color: var(--ink);
  font-family: inherit;
}
.search input::placeholder {
  color: var(--ink-4);
}
.search button {
  border: none;
  border-left: 1px solid var(--line);
  background: var(--bg-page);
  color: var(--ink-2);
  padding: 0 14px;
  cursor: pointer;
  font-size: var(--fs-sm);
  font-family: inherit;
  transition: background var(--t-base);
}
.search button:hover {
  background: var(--line-2);
}

/* ===== 窄屏（手机）=====
   ★ 手机屏放不下一整排内容，允许折行：第一行品牌名 + 菜单，第二行右边那组，
     整体居中。不折行的话右边会被挤到屏幕外，直接横向溢出。 */
@media (max-width: 900px) {
  .search {
    display: none; /* 搜索框最占地方，手机上先撤掉 */
  }
}

@media (max-width: 768px) {
  .header-inner {
    flex-wrap: wrap;
    justify-content: center;
    row-gap: var(--sp-2);
    padding: var(--sp-2) var(--sp-4);
  }
  .header-left {
    flex-wrap: wrap;
    justify-content: center;
    gap: var(--sp-4);
  }
  .nav {
    gap: 18px;
  }
  .header-right {
    flex-wrap: wrap;
    justify-content: center;
  }
  .brand {
    font-size: var(--fs-lg);
  }
}
</style>
