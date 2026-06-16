import Mock from 'mockjs'

Mock.setup({
  timeout: '200-600'
})

// 模拟登录接口：仅在 main.js 手动开启 mock 时使用，返回结构与真实 Session 登录后的用户信息保持一致。
Mock.mock('/api/user/login', 'post', (options) => {
  const body = typeof options.body === 'string' && options.body.includes('username=')
    ? Object.fromEntries(new URLSearchParams(options.body).entries())
    : JSON.parse(options.body)
  const { username, password } = body
  if (username === 'admin' && password === '123456') {
    return {
      code: 200,
      data: {
        id: 'user_admin',
        username: 'admin',
        role: 'ROLE_ADMIN'
      },
      message: '登录成功'
    }
  } else if (username === 'farmer' && password === '123456') {
    return {
      code: 200,
      data: {
        id: 'user_farmer',
        username: 'farmer',
        role: 'ROLE_FARMER'
      },
      message: '登录成功'
    }
  } else if (username === 'inspector' && password === '123456') {
    return {
      code: 200,
      data: {
        id: 'user_inspector',
        username: 'inspector',
        role: 'ROLE_INSPECTOR'
      },
      message: '登录成功'
    }
  } else {
    return {
      code: 401,
      message: '用户名或密码错误'
    }
  }
})

// 模拟产品数据 
let productList = Mock.mock({
  'list|20': [{
    'id|+1': 1,
    'name': '@cword(2,5)',
    'category': '@cword(2,4)',
    'origin': '@city',
    'price|10-200': 1,
    'createTime': '@datetime'
  }]
}).list

// 获取产品列表：解析分页和 keyword 参数，返回与真实后端一致的分页结构。
Mock.mock(/\/api\/product\/list/, 'get', (options) => {
  const url = new URL(options.url, 'http://localhost')
  const page = parseInt(url.searchParams.get('page')) || 1
  const pageSize = parseInt(url.searchParams.get('pageSize')) || 10
  const keyword = url.searchParams.get('keyword') || ''
  let filtered = productList
  if (keyword) {
    filtered = productList.filter(item => item.name.includes(keyword))
  }
  const start = (page - 1) * pageSize
  const end = start + pageSize
  const records = filtered.slice(start, end)
  return {
    code: 200,
    data: {
      records,
      total: filtered.length,
      page,
      pageSize
    }
  }
})

// 新增产品：追加到内存数组并返回新对象。
Mock.mock('/api/product', 'post', (options) => {
  const body = JSON.parse(options.body)
  const newId = productList.length + 1
  const newProduct = { ...body, id: newId, createTime: new Date() }
  productList.push(newProduct)
  return { code: 200, data: newProduct, message: '新增成功' }
})

// 修改产品：按 id 合并更新内存数组中的产品。
Mock.mock('/api/product', 'put', (options) => {
  const body = JSON.parse(options.body)
  const index = productList.findIndex(p => p.id === body.id)
  if (index !== -1) {
    productList[index] = { ...productList[index], ...body }
    return { code: 200, data: productList[index], message: '修改成功' }
  }
  return { code: 404, message: '产品不存在' }
})

// 产品详情：与产品列表共用内存数据，这里简单按 id 返回。
Mock.mock(/\/api\/product\/\d+/, 'get', (options) => {
    const id = parseInt(options.url.match(/\/api\/product\/(\d+)/)[1])
    const product = productList.find(p => p.id === id)
    if (product) {
        return { code: 200, data: product }
    }
    return { code: 404, message: '产品不存在' }
})

// 删除产品：从内存数组中移除指定 id。
Mock.mock(/\/api\/product\/\d+/, 'delete', (options) => {
  const id = parseInt(options.url.match(/\/api\/product\/(\d+)/)[1])
  const index = productList.findIndex(p => p.id === id)
  if (index !== -1) {
    productList.splice(index, 1)
    return { code: 200, message: '删除成功' }
  }
  return { code: 404, message: '产品不存在' }
})

// 模拟批次数据 
let batchList = Mock.mock({
  'list|30': [{
    'id|+1': 1,
    // 生成演示批次号，形如 BATCH2026001。
    'batchNo': () => `BATCH${Mock.mock('@integer(2026001, 2026050)')}`,
    'productId|1-3': 1,
    // 根据 productId 反查演示产品名，模拟后端连表结果。
    'productName': function () {
      const map = { 1: '有机苹果', 2: '五常大米', 3: '日照绿茶' }
      return map[this.productId]
    },
    'productionDate': '@date("yyyy-MM-dd")',
    'remark': '@cparagraph(1)'
  }]
}).list

// 获取批次列表：支持 productId 和 batchNo 过滤，并返回分页结构。
Mock.mock(/\/api\/batch\/list/, 'get', (options) => {
  const url = new URL(options.url, 'http://localhost')
  const page = parseInt(url.searchParams.get('page')) || 1
  const pageSize = parseInt(url.searchParams.get('pageSize')) || 10
  const productId = url.searchParams.get('productId')
  const batchNo = url.searchParams.get('batchNo')
  let filtered = batchList
  if (productId) {
    filtered = filtered.filter(item => item.productId == productId)
  }
  if (batchNo) {
    filtered = filtered.filter(item => item.batchNo.toLowerCase().includes(batchNo.toLowerCase()))
  }
  const start = (page - 1) * pageSize
  const end = start + pageSize
  const records = filtered.slice(start, end)
  return {
    code: 200,
    data: {
      records,
      total: filtered.length,
      page,
      pageSize
    }
  }
})

// 新增批次：绑定产品名称后写入内存批次数组。
Mock.mock('/api/batch', 'post', (options) => {
  const body = JSON.parse(options.body)
  const newId = batchList.length + 1
  const product = productList.find(p => p.id === body.productId)
  const newBatch = {
    ...body,
    id: newId,
    productName: product ? product.name : '未知产品'
  }
  batchList.push(newBatch)
  return { code: 200, data: newBatch, message: '新增成功' }
})

// 修改批次：按 id 更新基础字段和关联产品名称。
Mock.mock('/api/batch', 'put', (options) => {
  const body = JSON.parse(options.body)
  const index = batchList.findIndex(b => b.id === body.id)
  if (index !== -1) {
    const product = productList.find(p => p.id === body.productId)
    batchList[index] = {
      ...batchList[index],
      ...body,
      productName: product ? product.name : '未知产品'
    }
    return { code: 200, data: batchList[index], message: '修改成功' }
  }
  return { code: 404, message: '批次不存在' }
})

// 删除批次：从内存批次数组中移除指定 id。
Mock.mock(/\/api\/batch\/\d+/, 'delete', (options) => {
  const id = parseInt(options.url.match(/\/api\/batch\/(\d+)/)[1])
  const index = batchList.findIndex(b => b.id === id)
  if (index !== -1) {
    batchList.splice(index, 1)
    return { code: 200, message: '删除成功' }
  }
  return { code: 404, message: '批次不存在' }
})

// 仪表盘统计数据：返回固定演示数字。
Mock.mock('/api/dashboard/statistics', 'get', () => {
  return {
    code: 200,
    data: {
      productCount: 128,
      batchCount: 356,
      traceCount: 1280
    }
  }
})

// 产品类别分布：返回饼图所需 name/value 数组。
Mock.mock('/api/dashboard/categoryDistribution', 'get', () => {
  return {
    code: 200,
    data: [
      { name: '水果', value: 45 },
      { name: '粮食', value: 30 },
      { name: '蔬菜', value: 28 },
      { name: '茶叶', value: 15 },
      { name: '其他', value: 10 }
    ]
  }
})

// 近一周溯源趋势：动态生成最近 7 天日期和随机访问次数。
Mock.mock('/api/dashboard/traceTrend', 'get', () => {
  const dates = []
  const counts = []
  for (let i = 6; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    dates.push(date.toISOString().slice(0, 10))
    counts.push(Mock.mock('@integer(20, 100)'))
  }
  return {
    code: 200,
    data: { dates, counts }
  }
})

// 溯源信息：按产品 ID 返回演示生产、质检、物流记录。
Mock.mock(/\/api\/trace\/\d+/, 'get', (options) => {
  const id = parseInt(options.url.match(/\/api\/trace\/(\d+)/)[1])
  return {
    code: 200,
    data: {
      productionRecords: Mock.mock({
        'list|3-5': [{
          batchNo: '@string("upper", 6, 10)',
          productionDate: '@date("yyyy-MM-dd")',
          operator: '@cname'
        }]
      }).list,
      inspectionReports: [
        { item: '农药残留', result: '未检出', date: '2026-01-10' },
        { item: '重金属', result: '合格', date: '2026-01-10' },
        { item: '微生物', result: '合格', date: '2026-01-11' }
      ],
      logistics: [
        { node: '出库', time: '2026-01-12 08:00', location: '山东烟台仓库' },
        { node: '运输中', time: '2026-01-13 14:30', location: '济南中转站' },
        { node: '到达', time: '2026-01-15 10:00', location: '北京新发地市场' }
      ]
    }
  }
})

// 模拟菜单数据：真实项目已改为前端本地根据角色过滤，此接口仅保留演示兼容。
Mock.mock('/api/menu', 'get', (options) => { 
  // 实际项目中可根据 token 解析用户角色返回不同菜单 
  return { 
    code: 200, 
    data: [ 
      { 
        path: '/dashboard', 
        name: 'Dashboard', 
        meta: { title: '仪表盘', icon: 'PieChart' } 
      }, 
      { 
        path: '/products', 
        name: 'ProductList', 
        meta: { title: '产品管理', icon: 'Goods' } 
      }, 
      { 
        path: '/batches', 
        name: 'BatchList', 
        meta: { title: '批次管理', icon: 'List' } 
      }, 
      { 
        path: '/users', 
        name: 'UserManagement', 
        meta: { title: '用户管理', icon: 'User', roles: ['管理员'] } 
      }, 
      { 
        path: '/statistics', 
        name: 'Statistics', 
        meta: { title: '统计分析', icon: 'DataAnalysis', roles: ['管理员', '监管员'] } 
      } 
    ] 
  } 
}) 
