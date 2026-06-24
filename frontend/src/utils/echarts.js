// 统一的 ECharts 按需引入模块。
//
// 直接 `import * as echarts from 'echarts'` 会把整个图表库（约 1MB）打进首屏包。
// 这里改用 echarts/core + 按需注册图表与组件，只把项目真正用到的部分编译进来，
// 再通过 vite.config.js 的 manualChunks 单独切成 echarts chunk，显著降低首屏体积。
//
// 用法：各页面统一 `import echarts from '@/utils/echarts'`，
// 既能 echarts.init / setOption，也能继续使用 echarts.graphic.LinearGradient（core 已导出 graphic）。
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

// 注册当前所有统计/概览页用到的图表类型、交互组件与渲染器。
// 新增图表类型（如 ScatterChart）时，只需在此追加引入并注册即可。
echarts.use([
  BarChart,
  LineChart,
  PieChart,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
])

export default echarts
