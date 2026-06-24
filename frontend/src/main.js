// src/main.js 
import { createApp } from 'vue'
import App from './App.vue'

// 1. Element Plus UI库及相关样式 
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 2. Element Plus 图标库（全量注册，方便后续使用） 
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 3. 路由模块（实验2开始创建 router/index.js，此处先引入占位） 
import router from './router'  // 若尚未创建，需提前创建空路由文件，避免报错 

// 4. 状态管理（可选，若后续使用 Pinia，需先安装并创建 store） 
// import { createPinia } from 'pinia' 
// const pinia = createPinia() 

// 创建 Vue 应用实例 
const app = createApp(App)

// 注册 Element Plus 组件库 
app.use(ElementPlus)

// 注册所有 Element Plus 图标组件 
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    // 全量注册让路由 meta.icon 里的字符串组件名可以直接渲染。
    app.component(key, component)
}

// 使用路由 
app.use(router)

// 使用状态管理（若有） 
// app.use(pinia) 

// 挂载应用 
app.mount('#app') 
