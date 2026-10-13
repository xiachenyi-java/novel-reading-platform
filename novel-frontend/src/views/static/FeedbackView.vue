<template>
  <StaticPageLayout
    title="意见反馈"
    subtitle="你的建议，是项目迭代的最大动力"
    updated-at="2026-10-11"
  >
    <h2>反馈方式</h2>
    <p>
      本站是个人项目，没有客服团队，所有反馈由我本人查看。
      你可以通过以下方式联系我：
    </p>

    <div class="feedback-ways">
      <a
        class="feedback-card"
        href="mailto:3215154491@qq.com?subject=【书城反馈】&body=问题类型（Bug / 功能建议 / 体验问题）：%0D%0A%0D%0A使用的浏览器：%0D%0A%0D%0A问题描述：%0D%0A%0D%0A复现步骤：%0D%0A1. %0D%0A2. %0D%0A3. %0D%0A%0D%0A截图（可选）："
      >
        <div class="card-icon">📧</div>
        <div class="card-body">
          <div class="card-title">发送邮件</div>
          <div class="card-desc">点击后自动打开邮件客户端，已附带反馈模板</div>
        </div>
        <div class="card-arrow">→</div>
      </a>

      <a
        class="feedback-card"
        href="https://github.com/xiachenyi-java"
        target="_blank"
        rel="noopener"
      >
        <div class="card-icon">💻</div>
        <div class="card-body">
          <div class="card-title">提交 GitHub Issue</div>
          <div class="card-desc">适合技术问题、Bug 报告、功能建议</div>
        </div>
        <div class="card-arrow">→</div>
      </a>

      <router-link class="feedback-card" to="/contact">
        <div class="card-icon">💬</div>
        <div class="card-body">
          <div class="card-title">查看其他联系方式</div>
          <div class="card-desc">邮箱、GitHub 等完整联系方式</div>
        </div>
        <div class="card-arrow">→</div>
      </router-link>
    </div>

    <h2>反馈模板</h2>
    <p>为了让反馈更高效，建议按以下格式描述：</p>
    <div class="template-box">
      <div class="template-line"><strong>问题类型：</strong>Bug / 功能建议 / 体验问题 / 其他</div>
      <div class="template-line"><strong>使用的浏览器：</strong>Chrome 120 / Edge 120 / Firefox 121 ...</div>
      <div class="template-line"><strong>问题描述：</strong>发生了什么，预期应该是什么</div>
      <div class="template-line"><strong>复现步骤：</strong>1. ... 2. ... 3. ...</div>
      <div class="template-line"><strong>截图（可选）：</strong>能附上截图会更快定位</div>
    </div>

    <h2>最关心的反馈</h2>
    <ul>
      <li><strong>Bug 报告</strong>：页面打不开、数据不显示、按钮没反应；</li>
      <li><strong>功能建议</strong>：希望增加书单、评论、好友、AI 推荐等；</li>
      <li><strong>体验问题</strong>：阅读器不舒服、书架不好用、样式不好看；</li>
      <li><strong>版权问题</strong>：如发现可能侵犯他人权益的内容，请务必告知，我会立即处理。</li>
    </ul>

    <h2>暂不受理的反馈</h2>
    <ul>
      <li>求小说 TXT 下载 —— 本站不提供；</li>
      <li>投诉某本小说的内容 —— 请联系作品所在的正版平台。</li>
    </ul>

    <p class="thanks">
      🌟 感谢你愿意花时间反馈，每一条建议我都会认真看。
    </p>
  </StaticPageLayout>
</template>

<script setup>
import StaticPageLayout from '../../components/StaticPageLayout.vue'
</script>

<style scoped>
.feedback-ways {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0 8px;
}
.feedback-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fafaff;
  border: 1px solid #f0edff;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
  cursor: pointer;
}
.feedback-card:hover {
  border-color: #c4b5fd;
  background: #f5f2ff;
  transform: translateX(2px);
}
.card-icon {
  font-size: 26px;
  flex-shrink: 0;
}
.card-body {
  flex: 1;
  min-width: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 2px;
}
.card-desc {
  font-size: 12px;
  color: #999;
}
.card-arrow {
  font-size: 18px;
  color: #c4b5fd;
  flex-shrink: 0;
}

.template-box {
  background: #fafafa;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
  padding: 16px 20px;
  margin: 12px 0 20px;
  font-size: 13px;
  color: #555;
}
.template-line {
  margin-bottom: 8px;
  line-height: 1.7;
}
.template-line:last-child {
  margin-bottom: 0;
}
.template-line strong {
  color: #7c3aed;
}

.thanks {
  margin-top: 28px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f5f2ff, #ede9fe);
  border-radius: 8px;
  color: #6d28d9;
  font-size: 13px;
  text-align: center;
}
</style>