<template>
  <footer class="site-footer">
    <div class="footer-links">
      <router-link to="/about">关于我们</router-link>
      <span class="divider">|</span>
      <router-link to="/contact">联系方式</router-link>
      <span class="divider">|</span>
      <router-link to="/terms">用户协议</router-link>
      <span class="divider">|</span>
      <router-link to="/privacy">隐私政策</router-link>
      <span class="divider">|</span>
      <router-link to="/feedback">意见反馈</router-link>
    </div>
    <p class="copyright">
      Copyright © 2026 草莓 · 保留所有权利
    </p>
    <p class="copyright-sub">
      本站为个人学习项目，所有内容仅用于技术演示，不存储任何第三方作品正文
    </p>
  </footer>
</template>

<style scoped>
.site-footer {
  background: #fff;
  border-top: 1px solid #ececea;
  padding: 24px 20px 20px;
  text-align: center;
  flex-shrink: 0;
}
.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.footer-links a {
  color: #888;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: #6d4bd0;
}
.footer-links .divider {
  color: #ddd;
  font-size: 12px;
}
.copyright {
  color: #999;
  font-size: 12px;
  margin: 0 0 4px;
}
.copyright-sub {
  color: #bbb;
  font-size: 12px;
  margin: 0;
}
</style>