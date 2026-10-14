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
  line-height: 1.5; /* 正文行高统一 1.5 */
  color: var(--text-2);
  background: var(--bg-page);
}

#app {
  min-height: 100vh;
}
</style>