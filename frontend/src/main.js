import './assets/main.css'
import './assets/global.css'
import './assets/notifications.css'

import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from './App.vue'

// Импортируем компоненты страниц
import LoginView from './views/LoginView.vue'
import MainView from './views/MainView.vue'

// Создаем маршруты
const routes = [
  { path: '/', component: LoginView },
  { path: '/main', component: MainView }
]

// Создаем роутер
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Добавляем navigation guard для защиты маршрутов
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  
  // Если пытается зайти на /main без токена
  if (to.path === '/main' && !token) {
    next('/')
    return
  }
  
  // Если токен есть и пытается зайти на главную, редиректим на /main
  if (to.path === '/' && token) {
    next('/main')
    return
  }
  
  next()
})

// Создаем Pinia
const pinia = createPinia()

// Создаем и монтируем приложение
createApp(App).use(router).use(pinia).mount('#app')
