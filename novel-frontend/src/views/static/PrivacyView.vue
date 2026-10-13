<template>
  <StaticPageLayout
    title="隐私政策"
    subtitle="我们如何收集、使用和保护你的信息"
    updated-at="2026-10-11"
  >
    <h2>一、引言</h2>
    <p>
      本站是个人学习项目，作者非常重视用户隐私。
      本政策说明本站会收集哪些信息、如何使用、如何保护。
      请在使用本站前仔细阅读。
    </p>

    <h2>二、我们收集的信息</h2>

    <h3>1. 你主动提供的信息</h3>
    <ul>
      <li><strong>注册信息</strong>：注册时填写的用户名、密码（密码经加密后存储）；</li>
      <li><strong>内容信息</strong>：你未来可能创建的书单、评论、笔记等内容。</li>
    </ul>

    <h3>2. 系统自动生成的信息</h3>
    <ul>
      <li><strong>登录凭证</strong>：登录后由服务器签发的 Token，用于识别你的登录状态；</li>
      <li><strong>访问日志</strong>：服务器会记录请求时间、IP 地址和浏览器信息，
          仅用于排查故障、防止滥用和保障服务安全。</li>
    </ul>

    <h2>三、我们不收集的信息</h2>
    <ul>
      <li>不收集你的真实姓名、身份证号、银行卡号；</li>
      <li>不收集你的通讯录、短信、相册、麦克风、摄像头、位置信息；</li>
      <li>不接入任何第三方广告 SDK；</li>
      <li>不追踪你在其他网站的行为。</li>
    </ul>

    <h2>四、信息如何存储</h2>
    <p>
      <strong>登录凭证</strong>保存在你浏览器的本地存储（localStorage）中，
      仅用于向服务器证明你的登录状态。
      清除浏览器数据或点击「退出登录」后，该凭证会立即失效。
    </p>
    <p>
      <strong>账号信息和用户内容</strong>保存在服务器数据库中，
      仅用于实现本站功能。本站不会将这些数据提供给任何第三方。
    </p>

    <h2>五、信息如何保护</h2>
    <ul>
      <li>用户密码经过加密后存储，服务器不会保存明文密码；</li>
      <li>登录采用 Token 机制，Token 具有有效期，到期自动失效；</li>
      <li>退出登录后，Token 会立即被加入黑名单，防止被继续使用；</li>
      <li>关键接口采用限流机制，防止暴力破解。</li>
    </ul>

    <h2>六、你的权利</h2>
    <p>你可以随时：</p>
    <ul>
      <li>修改或删除自己发布的内容（书单、评论、笔记等）；</li>
      <li>清理浏览器数据或点击「退出登录」，停止使用本站服务；</li>
      <li>通过邮箱联系我，申请删除你的全部账号数据。</li>
    </ul>

    <h2>七、Cookie 与本地存储</h2>
    <p>
      本站不使用第三方 Cookie，只在浏览器本地存储中保存
      <strong>登录 Token、用户名和角色信息</strong>。
      这些数据不会上传到任何第三方服务器。
    </p>

    <h2>八、未成年人的隐私</h2>
    <p>
      本站为个人学习项目，并非针对未成年人设计。
      如果你是未成年人，请在监护人指导下使用本站。
    </p>

    <h2>九、政策更新</h2>
    <p>
      本政策如有更新，将在本页面公布并更新「最近更新」日期。
      如有任何疑问，请通过 <router-link to="/contact">联系方式</router-link> 与我联系。
    </p>

    <p class="footer-note">
      本政策最后更新于 2026 年 10 月 11 日。
    </p>
  </StaticPageLayout>
</template>

<script setup>
import StaticPageLayout from '../../components/StaticPageLayout.vue'
</script>

<style scoped>
:deep(h3) {
  font-size: 15px;
  font-weight: 600;
  color: #444;
  margin: 18px 0 8px;
}
.footer-note {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  color: #999;
  font-size: 12px;
  text-align: center;
}
</style>