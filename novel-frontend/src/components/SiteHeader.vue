<template>
  <!-- ============================================================
       全站统一的顶部导航栏
       ------------------------------------------------------------
       【为什么要这个组件】
         改之前：9 个页面各自写了一遍顶部导航（.header + .header-inner），
                内容区宽度还有 1200px 和 1300px 两种，
                品牌名有两套写法（纯文字 / 图标+文字）。
                结果：从首页点进榜单，导航栏会轻微跳一下。

         改之后：全站只有这一个导航栏，宽度、高度、阴影、字号都统一，
                不再随页面变。

       【怎么用】
         1) 只要一条普通导航栏：
              <SiteHeader />

         2) 品牌名右边要放东西（比如首页的分类切换）→ 写在标签里面：
              <SiteHeader>
                <div class="nav-links">...</div>
              </SiteHeader>

         3) 右边要放东西（用户区 / 返回按钮）→ 用 #right 插槽：
              <SiteHeader>
                <template #right>
                  <el-button size="small">返回</el-button>
                </template>
              </SiteHeader>

       ★ 插槽内容的样式写在"用它的那个页面"里（就近放），不要写在这里。
         这是 Vue 的规矩：插槽内容的样式由父组件管。
         所以本文件只管"导航栏这个壳"的样式。
       ============================================================ -->
  <header class="site-header">
    <div class="header-inner">
      <!-- 左：品牌名 + 页面自己塞的内容（默认插槽） -->
      <div class="header-left">
        <span class="brand" @click="goHome">📚 夏辰义的书城</span>
        <slot />
      </div>

      <!-- 右：页面自己塞的内容（#right 插槽），不塞就是空的 -->
      <div class="header-right">
        <slot name="right" />
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 点品牌名回首页
const goHome = () => router.push('/')
</script>

<style scoped>
/* 吸顶导航：往下滚动时始终贴在顶部 */
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  background: var(--bg-card);
  box-shadow: var(--shadow-md);
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
  gap: var(--sp-4);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--sp-6);
  min-width: 0; /* 让里面的文字能正常省略，不被撑破 */
}

/* 品牌名：紫色渐变文字 */
.brand {
  font-size: var(--fs-lg);
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  flex-shrink: 0; /* 窗口变窄时优先保右边，不压缩按钮 */
}

/* ===== 窄屏（手机）=====
   ★ 手机屏放不下一整排内容，所以允许折行：
     第一行放"品牌名 + 分类"，第二行放右侧那组按钮，整体居中。
     不折行的话右边按钮会被挤到屏幕外，直接横向溢出。
   ★ 页面里那些不重要的字（比如"游客模式"）由页面自己藏，导航栏不管。 */
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
  .header-right {
    flex-wrap: wrap;
    justify-content: center;
  }
  .brand {
    font-size: var(--fs-md);
  }
}
</style>
