import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import { setHttpErrorToast } from './api/http'
import { installPermissionDirective, setPermissions } from './directives/permission'
import { pushToast } from './composables/toast'
import 'element-plus/dist/index.css'

// —— 设计 Token 与全部样式（按原型加载顺序）——
import './styles/tokens.css'
import './styles/components.css'
import './styles/layout.css'
import './styles/login.css'
import './styles/dashboard.css'
import './styles/manage.css'
import './styles/pages.css'

// 初始化外观（写入强调色 CSS 变量等）
import './composables/appearance'

// 权限初始化：默认开放全部权限；后续对接登录接口后替换
setPermissions(['*'])
setHttpErrorToast(pushToast)

const app = createApp(App)
installPermissionDirective(app)
app.use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
