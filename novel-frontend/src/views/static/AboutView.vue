<template>
  <StaticPageLayout
    title="关于我们"
    subtitle="一个专注于阅读与分享的个人项目"
    updated-at="2026-10-11"
  >
    <h2>关于作者</h2>
    <p>
      你好，我是 <strong>夏辰义</strong>，文华学院物联网工程专业在读学生，
      目前主攻 Java 后端开发方向，正在准备实习。
      这个项目是我独立完成的全栈练习，从前端页面、后端接口到数据库设计，
      都由我一个人从零搭建，用来把课堂上学到的技术真正落到一个完整的产品里。
    </p>

    <h2>关于本站</h2>
    <p>
      「<strong>草莓</strong>」是一个个人学习项目，
      定位是「小说推荐 + 私人阅读空间」的交流平台。
      我们不提供小说正文的在线阅读服务，而是把重点放在
      <strong>发现好书、整理书单、记录阅读</strong> 这三件事上。
    </p>
    <p>
      在这里，每一本小说只是一个「条目」——包含书名、作者、简介、封面等信息。
      你可以把它加入自己的私人书房，记录阅读进度、写下读书笔记，
      也可以整理成一份公开书单，把好书推荐给其他读者。
    </p>

    <h2>为什么做这个</h2>
    <p>
      身边很多朋友都有「书荒」的困扰：打开阅读 App，
      排行榜翻来覆去总是那几本，真正对胃口的好书反而不好找。
      我希望能做一个地方，让「<strong>人的推荐</strong>」比算法更管用——
      你信得过的人推荐的书，往往比首页推荐更靠谱。
    </p>

    <h2>项目技术栈</h2>
    <ul>
      <li><strong>前端</strong>：Vue 3 · Vite · Element Plus · Pinia · Vue Router</li>
      <li><strong>后端</strong>：Spring Boot · MySQL · Redis · JWT · MyBatis</li>
      <li><strong>部署</strong>：Docker · Nginx</li>
    </ul>

    <h2>项目状态</h2>
    <p>
      项目目前仍处于<strong>开发迭代阶段</strong>，尚未正式上线。
      所有功能、页面和数据仅用于个人学习和演示。
      后续会逐步补齐书单、评论、好友等社区功能，欢迎持续关注。
    </p>

    <h2>联系我</h2>
    <p>
      如果你对项目有任何建议、想法，或者想一起交流技术，
      欢迎通过 <router-link to="/contact">联系方式</router-link> 页面找到我。
    </p>

    <p class="footer-note">
      📌 本站为个人学习项目，不存储任何第三方作品的正文内容。
    </p>
  </StaticPageLayout>
</template>

<script setup>
import StaticPageLayout from '../../components/StaticPageLayout.vue'
</script>

<style scoped>
.footer-note {
  margin-top: 28px;
  padding: 14px 18px;
  background: #f2eefb;
  border-radius: 8px;
  color: #5b3cb8;
  font-size: 13px;
  text-align: center;
}
</style>