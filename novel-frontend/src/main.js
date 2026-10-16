import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// ★ 中文语言包：不配这个的话，Element Plus 内置文案全是英文 ——
//   确认框的按钮是 OK / Cancel、分页器是 "Go to / items/page"、
//   下拉和表格的空状态是 "No Data"。注意用的是 es/locale 下的子路径，
//   不是额外装什么包（element-plus 自带）。
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// ★ 设计变量（颜色/字号/间距/圆角/阴影），全站唯一的"样式字典"
//   必须放在 element-plus 的 css 之后，这样以后想改 Element Plus 的默认样式也压得住
import './styles/tokens.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia())
app.use(router)
// ★ 传 locale 进去，全站的 Element Plus 内置文案才会是中文
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')