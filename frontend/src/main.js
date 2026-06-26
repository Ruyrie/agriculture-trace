/**
 * src/main.js — Vue 应用入口文件。
 *
 * 负责：
 *   1. 创建 Vue 应用实例（createApp）。
 *   2. 注册 Element Plus 组件库和图标库（全量注册，供模板中直接使用组件名字符串）。
 *   3. 注册路由（router/index.js）。
 *   4. 挂载应用到 index.html 中 id="app" 的 DOM 节点。
 *
 * 关联文件：
 *   - App.vue：根组件，包含 <router-view>
 *   - router/index.js：路由配置，含导航守卫和权限判断
 *   - Element Plus 图标库：全量注册后可在任何组件模板中用 <el-icon><IconName /></el-icon>
 */
import { createApp } from 'vue'
import App from './App.vue'

// Element Plus UI 组件库及默认样式（含按钮、表单、弹窗、表格等所有组件）。
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// Element Plus 官方图标库，全量导入后逐个注册为全局组件。
// 全量注册是为了让 router/index.js 的 meta.icon 字段（字符串组件名）可以动态渲染，
// 而无需在每个视图文件中单独 import 对应图标。
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 路由模块：含路由表、导航守卫（beforeEach）、权限校验逻辑。
// 守卫从 localStorage 读取 sessionActive / userRole，无权限时跳转 /login。
import router from './router'

// 创建 Vue 3 应用实例，以 App.vue 作为根组件（包含 <router-view>）。
const app = createApp(App)

// 注册 Element Plus：使所有 el-* 组件（el-button、el-table 等）在全局可用。
app.use(ElementPlus)

// 逐个注册所有 Element Plus 图标为全局 Vue 组件。
// key 为组件名（如 "EditPen"），component 为对应的 Vue 组件对象。
// 注册后可在任意模板中直接使用 <EditPen />，无需局部导入。
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 注册路由：使应用支持 <router-view>、<router-link> 以及 useRouter/useRoute。
app.use(router)

// 将应用挂载到 index.html 的 <div id="app"> 节点，从此 Vue 接管该节点的渲染。
app.mount('#app')
