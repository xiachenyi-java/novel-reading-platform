<template>
  <StaticPageLayout
    title="联系方式"
    subtitle="有问题、有建议，欢迎随时联系"
    updated-at="2026-10-11"
  >
    <h2>作者联系方式</h2>
    <p>
      本站是个人项目，所有消息由我本人查看，回复可能不及时，请见谅。
    </p>

    <div class="contact-list">
      <div class="contact-item">
        <div class="contact-icon">📧</div>
        <div class="contact-body">
          <div class="contact-label">邮箱</div>
          <a href="mailto:3215154491@qq.com" class="contact-value">
            3215154491@qq.com
          </a>
          <div class="contact-tip">推荐使用，最常查看</div>
        </div>
      </div>

      <div class="contact-item">
        <div class="contact-icon">💻</div>
        <div class="contact-body">
          <div class="contact-label">GitHub</div>
          <a href="https://github.com/xiachenyi-java" target="_blank" rel="noopener" class="contact-value">
            github.com/xiachenyi-java
          </a>
          <div class="contact-tip">项目已开源，欢迎提 Issue 和 Star</div>
        </div>
      </div>

      <div class="contact-item">
        <div class="contact-icon">📱</div>
        <div class="contact-body">
          <div class="contact-label">手机 / 微信</div>
          <span class="contact-value">15972043858</span>
          <div class="contact-tip">工作时间外请优先邮箱联系</div>
        </div>
      </div>
    </div>

    <h2>什么情况欢迎联系我</h2>
    <ul>
      <li>发现了 Bug，或者页面有明显异常；</li>
      <li>对功能有建议，比如想加书单、评论、好友等；</li>
      <li>想一起交流技术，或者基于这个项目做二次开发；</li>
      <li>发现任何涉及版权问题的内容，我会第一时间处理。</li>
    </ul>

    <h2>什么情况不必联系我</h2>
    <ul>
      <li>想找某本小说的 TXT 下载 —— 本站不提供；</li>
      <li>投诉某本小说的内容 —— 请联系作品所在的正版平台。</li>
    </ul>

    <p class="back-hint">
      👉 如果你只是想反馈使用体验，可以直接去
      <router-link to="/feedback">意见反馈</router-link> 页面。
    </p>
  </StaticPageLayout>
</template>

<script setup>
import StaticPageLayout from '../../components/StaticPageLayout.vue'
</script>

<style scoped>
.contact-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 16px 0 8px;
}
.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: #fafaff;
  border: 1px solid #f0edff;
  border-radius: 10px;
  transition: all 0.2s;
}
.contact-item:hover {
  border-color: #c4b5fd;
  background: #f5f2ff;
}
.contact-icon {
  font-size: 24px;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 2px;
}
.contact-body {
  flex: 1;
  min-width: 0;
}
.contact-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}
.contact-value {
  font-size: 15px;
  font-weight: 600;
  color: #7c3aed;
  text-decoration: none;
  word-break: break-all;
}
a.contact-value:hover {
  text-decoration: underline;
}
.contact-tip {
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
}
.back-hint {
  margin-top: 24px;
  padding: 14px 18px;
  background: #f5f2ff;
  border-radius: 8px;
  color: #6d28d9;
  font-size: 13px;
}
</style>