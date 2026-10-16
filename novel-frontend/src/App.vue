<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
/* ============================================================
 * 全局基础样式
 *   具体颜色/字号的值都在 src/styles/tokens.css 里，这里只引用变量名。
 * ============================================================ */

/* 所有元素去掉浏览器默认边距；box-sizing 让宽度把 padding 算进去，
   否则写了 width:100% 再加 padding 就会溢出 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: var(--fs-base);
  /* 正文行高：1.5 → 1.7（2026-10-15 跟设计稿对齐）。
     中文比英文更需要行距，1.5 会显得字挤在一起；
     1.7 是这批设计稿统一用的值，别再单独调。 */
  line-height: 1.7;
  color: var(--ink-2);
  background: var(--bg-page);
}

#app {
  min-height: 100vh;
}
</style>