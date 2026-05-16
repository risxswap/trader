import { createRouter, createWebHistory, RouterView, type RouteRecordRaw } from 'vue-router'
import { defineComponent, h, type Component } from 'vue'
import Login from '../pages/Login.vue'
import Dashboard from '../pages/dashboard/Index.vue'
import Settings from '../pages/settings/Index.vue'
import ExchangeList from '../pages/exchange/List.vue'
import InvestmentList from '../pages/investment/List.vue'
import InvestmentDetail from '../pages/investment/Detail.vue'
import InvestmentLog from '../pages/investment/LogList.vue'
import InvestmentTrading from '../pages/investment/TradingList.vue'
import BrokerList from '../pages/investment/BrokerList.vue'
import BrokerDetail from '../pages/investment/BrokerDetail.vue'
import CalendarList from '../pages/exchange/CalendarList.vue'
import PublicList from '../pages/funds/List.vue'
import EtfList from '../pages/etf/List.vue'
import EtfDetail from '../pages/etf/Detail.vue'
import EtfMarket from '../pages/etf/MarketList.vue'
import EtfAdj from '../pages/etf/AdjList.vue'
import PublicDetail from '../pages/funds/Detail.vue'
import PublicNav from '../pages/funds/NavList.vue'
import CorrelationList from '../pages/analysis/CorrelationList.vue'
import CorrelationDetail from '../pages/analysis/CorrelationDetail.vue'
import NodeList from '../pages/node/List.vue'
import NodeGroupList from '../pages/node/Group.vue'
import TaskList from '../pages/logs/TaskList.vue'
import TaskManage from '../pages/task/Manage.vue'
import TaskDetail from '../pages/task/Detail.vue'

const wrapRouteComponent = (name: string, component: Component) =>
  defineComponent({
    name,
    render: () => h(component)
  })

const wrapRouteGroup = (name: string) =>
  defineComponent({
    name,
    render: () => h(RouterView)
  })

type AppRouteMeta = {
  title?: string
  affix?: boolean
  menuKind?: 'item' | 'hidden'
  menuIcon?: string
  menuTitle?: string
}

export type SideMenuLeaf = {
  index: string
  title: string
}

export type SideMenuEntry =
  | { type: 'item'; key: string; index: string; title: string; icon: string }
  | { type: 'group'; key: string; index: string; title: string; icon: string; items: SideMenuLeaf[] }

const buildMenuLeaves = (routes: RouteRecordRaw[]): SideMenuLeaf[] => {
  const items: SideMenuLeaf[] = []

  routes.forEach((route) => {
    if (!route.path) return
    const meta = (route.meta || {}) as AppRouteMeta
    if (meta.menuKind === 'hidden') return

    if (route.children?.length) {
      items.push(...buildMenuLeaves(route.children))
      return
    }

    if (route.redirect) return

    const title = String(meta.menuTitle || meta.title || '')
    if (!title) return

    items.push({
      index: route.path,
      title
    })
  })

  return items
}

export const buildSideMenus = (routes: RouteRecordRaw[]): SideMenuEntry[] => {
  const items: SideMenuEntry[] = []

  routes.forEach((route) => {
    if (!route.path) return
    const meta = (route.meta || {}) as AppRouteMeta
    if (meta.menuKind === 'hidden') return

    if (meta.menuKind === 'item') {
      items.push({
        type: 'item',
        key: `item:${route.path}`,
        index: route.path,
        title: String(meta.menuTitle || meta.title || ''),
        icon: String(meta.menuIcon || 'DataBoard')
      })
      return
    }

    if (route.children?.length) {
      const children = buildMenuLeaves(route.children)
      if (!children.length) return

      items.push({
        type: 'group',
        key: `group:${route.path}`,
        index: children[0].index,
        title: String(meta.menuTitle || meta.title || ''),
        icon: String(meta.menuIcon || 'DataBoard'),
        items: children
      })
      return
    }

    if (route.redirect) return
  })

  return items
}

export const appRouteChildren: RouteRecordRaw[] = [
  { path: '', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: wrapRouteComponent('Dashboard', Dashboard),
    meta: {
      title: '仪表盘',
      affix: true,
      menuKind: 'item',
      menuIcon: 'DataBoard'
    }
  },
  {
    path: '/investment',
    component: wrapRouteGroup('InvestmentMenuGroup'),
    redirect: '/broker/list',
    meta: {
      title: '投资',
      menuIcon: 'Aim'
    },
    children: [
      {
        path: '/broker/list',
        name: 'BrokerList',
        component: wrapRouteComponent('BrokerList', BrokerList),
        meta: {
          title: '券商管理'
        }
      },
      {
        path: '/investment/list',
        name: 'InvestmentList',
        component: wrapRouteComponent('InvestmentList', InvestmentList),
        meta: {
          title: '投资列表'
        }
      },
      {
        path: '/investment/log',
        name: 'InvestmentLog',
        component: wrapRouteComponent('InvestmentLog', InvestmentLog),
        meta: {
          title: '投资日志'
        }
      },
      {
        path: '/investment/trading',
        name: 'InvestmentTrading',
        component: wrapRouteComponent('InvestmentTrading', InvestmentTrading),
        meta: {
          title: '投资交易'
        }
      },
      {
        path: '/investment/detail/:id',
        name: 'InvestmentDetail',
        component: wrapRouteComponent('InvestmentDetail', InvestmentDetail),
        meta: {
          title: '投资详情',
          menuKind: 'hidden'
        }
      },
      {
        path: '/broker/detail/:id',
        name: 'BrokerDetail',
        component: wrapRouteComponent('BrokerDetail', BrokerDetail),
        meta: {
          title: '券商详情',
          menuKind: 'hidden'
        }
      }
    ]
  },
  {
    path: '/task',
    component: wrapRouteGroup('TaskMenuGroup'),
    redirect: '/task/manage',
    meta: {
      title: '任务调度',
      menuIcon: 'Timer'
    },
    children: [
      {
        path: '/task/manage',
        name: 'TaskManage',
        component: wrapRouteComponent('TaskManage', TaskManage),
        meta: {
          title: '任务管理'
        }
      },
      {
        path: '/task/log',
        name: 'TaskList',
        component: wrapRouteComponent('TaskList', TaskList),
        meta: {
          title: '任务日志'
        }
      },
      {
        path: '/task/detail',
        name: 'TaskDetail',
        component: wrapRouteComponent('TaskDetail', TaskDetail),
        meta: {
          title: '任务详情',
          menuKind: 'hidden'
        }
      },
      {
        path: '/node/list',
        name: 'NodeList',
        component: wrapRouteComponent('NodeList', NodeList),
        meta: {
          title: '节点管理'
        }
      },
      {
        path: '/node/group',
        name: 'NodeGroupList',
        component: wrapRouteComponent('NodeGroupList', NodeGroupList),
        meta: {
          title: '节点分组'
        }
      }
    ]
  },
  { path: '/funds', redirect: '/etf/list' },
  {
    path: '/etf',
    component: wrapRouteGroup('EtfMenuGroup'),
    redirect: '/etf/list',
    meta: {
      title: 'ETF基金',
      menuIcon: 'Histogram'
    },
    children: [
      {
        path: '/etf/list',
        name: 'EtfList',
        component: wrapRouteComponent('EtfList', EtfList),
        meta: {
          title: '基金列表'
        }
      },
      {
        path: '/etf/market',
        name: 'EtfMarket',
        component: wrapRouteComponent('EtfMarket', EtfMarket),
        meta: {
          title: 'ETF行情'
        }
      },
      {
        path: '/etf/adj',
        name: 'EtfAdj',
        component: wrapRouteComponent('EtfAdj', EtfAdj),
        meta: {
          title: '复权因子'
        }
      },
      {
        path: '/etf/detail/:code',
        name: 'EtfDetail',
        component: wrapRouteComponent('EtfDetail', EtfDetail),
        meta: {
          title: 'ETF详情',
          menuKind: 'hidden'
        }
      }
    ]
  },
  {
    path: '/public',
    component: wrapRouteGroup('PublicMenuGroup'),
    redirect: '/public/list',
    meta: {
      title: '公募基金',
      menuIcon: 'Coin'
    },
    children: [
      {
        path: '/public/list',
        name: 'PublicList',
        component: wrapRouteComponent('PublicList', PublicList),
        meta: {
          title: '基金列表'
        }
      },
      {
        path: '/public/nav',
        name: 'PublicNav',
        component: wrapRouteComponent('PublicNav', PublicNav),
        meta: {
          title: '基金净值'
        }
      },
      {
        path: '/funds/detail/:code',
        name: 'PublicDetail',
        component: wrapRouteComponent('PublicDetail', PublicDetail),
        meta: {
          title: '公募基金详情',
          menuKind: 'hidden'
        }
      }
    ]
  },
  {
    path: '/exchange',
    component: wrapRouteGroup('ExchangeMenuGroup'),
    redirect: '/exchange/list',
    meta: {
      title: '交易所',
      menuIcon: 'OfficeBuilding'
    },
    children: [
      {
        path: '/exchange/list',
        name: 'ExchangeList',
        component: wrapRouteComponent('ExchangeList', ExchangeList),
        meta: {
          title: '交易所列表'
        }
      },
      {
        path: '/exchange/calendar',
        name: 'CalendarList',
        component: wrapRouteComponent('CalendarList', CalendarList),
        meta: {
          title: '交易日历'
        }
      }
    ]
  },
  {
    path: '/analysis',
    component: wrapRouteGroup('AnalysisMenuGroup'),
    redirect: '/analysis/correlation',
    meta: {
      title: '统计分析',
      menuIcon: 'TrendCharts'
    },
    children: [
      {
        path: '/analysis/correlation',
        name: 'CorrelationList',
        component: wrapRouteComponent('CorrelationList', CorrelationList),
        meta: {
          title: '相关统计'
        }
      },
      {
        path: '/analysis/correlation/detail/:id',
        name: 'CorrelationDetail',
        component: wrapRouteComponent('CorrelationDetail', CorrelationDetail),
        meta: {
          title: '相关统计详情',
          menuKind: 'hidden'
        }
      }
    ]
  },
  {
    path: '/settings-group',
    component: wrapRouteGroup('SettingsMenuGroup'),
    redirect: '/settings',
    meta: {
      title: '系统管理',
      menuIcon: 'Setting'
    },
    children: [
      {
        path: '/settings',
        name: 'Settings',
        component: wrapRouteComponent('Settings', Settings),
        meta: {
          title: '系统设置'
        }
      }
    ]
  }
]

export const appSideMenus = buildSideMenus(appRouteChildren)

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      children: appRouteChildren
    }
  ]
})

router.beforeEach((to) => {
  const isLogin = !!localStorage.getItem('token')
  if (!isLogin && to.path !== '/login') return '/login'
})

export default router
