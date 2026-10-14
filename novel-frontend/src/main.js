import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

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
app.use(ElementPlus)

app.mount('#app')