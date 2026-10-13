<template>
  <div class="static-page">
    <header class="static-header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <span class="brand-icon">📚</span>
          <span class="brand-name">夏辰义的书城</span>
        </div>
        <el-button size="small" @click="$router.push('/')">← 返回首页</el-button>
      </div>
    </header>

    <main class="static-main">
      <div class="content-card">
        <h1 class="page-title">{{ title }}</h1>
        <p class="page-subtitle" v-if="subtitle">{{ subtitle }}</p>
        <p class="page-update" v-if="updatedAt">最近更新：{{ updatedAt }}</p>
        <div class="page-body">
          <slot />
        </div>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import SiteFooter from './SiteFooter.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  updatedAt: { type: String, default: '' }
})
</script>

<style scoped>
.static-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.static-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 60px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.brand-icon { font-size: 24px; }
.brand-name {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.static-main {
  flex: 1;
  padding: 40px 24px;
  display: flex;
  justify-content: center;
}
.content-card {
  width: 100%;
  max-width: 800px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 48px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}
.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 0 0 6px;
}
.page-update {
  font-size: 12px;
  color: #bbb;
  margin: 0 0 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
}

.page-body {
  font-size: 14px;
  line-height: 1.9;
  color: #444;
}
.page-body :deep(h2) {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 28px 0 10px;
}
.page-body :deep(h2:first-child) {
  margin-top: 0;
}
.page-body :deep(p) {
  margin: 0 0 12px;
}
.page-body :deep(ul) {
  margin: 0 0 14px;
  padding-left: 22px;
}
.page-body :deep(li) {
  margin-bottom: 6px;
}
.page-body :deep(a) {
  color: #7c3aed;
  text-decoration: none;
}
.page-body :deep(a:hover) {
  text-decoration: underline;
}
.page-body :deep(strong) {
  color: #1a1a2e;
}

@media (max-width: 640px) {
  .static-main { padding: 20px 12px; }
  .content-card { padding: 24px 20px; }
  .page-title { font-size: 22px; }
}
</style>