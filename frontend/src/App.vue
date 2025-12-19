<template>
  <router-view :key="routerKey" />
  <RateLimitModal 
    v-if="showRateLimitModal" 
    :show="showRateLimitModal"
    :message="rateLimitMessage"
    @close="closeRateLimitModal"
  />
</template>

<script setup>
import { watch, computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'
import { showError } from './utils/notifications'
import RateLimitModal from './components/RateLimitModal.vue'

const showRateLimitModal = ref(false)
const rateLimitMessage = ref('')

// Функция для показа модального окна rate limit
window.showRateLimitModal = (message) => {
  rateLimitMessage.value = message || 'Слишком много запросов. Максимум 5 запросов в секунду. Пожалуйста, подождите.'
  showRateLimitModal.value = true
}

const closeRateLimitModal = () => {
  showRateLimitModal.value = false
}

const router = useRouter()
const authStore = useAuthStore()

// Проверяем аутентификацию при запуске приложения
authStore.checkAuth()

// Ключ для пересоздания компонента при логауте
const logoutKey = ref(0)
const routerKey = computed(() => {
  return `${router.currentRoute.value.path}-${authStore.isAuthenticated}-${logoutKey.value}`
})

// Сохраняем предыдущее состояние аутентификации
let previousAuthState = authStore.isAuthenticated

// Следим за изменениями состояния аутентификации
watch(
  () => authStore.isAuthenticated,
  (isAuthenticated) => {
    const wasAuthenticated = previousAuthState
    previousAuthState = isAuthenticated
    
    // Если пользователь вышел (не аутентифицирован) и находится на защищенной странице
    if (!isAuthenticated && wasAuthenticated && router.currentRoute.value.path === '/main') {
      // Увеличиваем ключ для принудительного пересоздания компонента
      logoutKey.value++
      router.push('/')
    }
    // Если пользователь вошел и находится на странице логина
    if (isAuthenticated && !wasAuthenticated && router.currentRoute.value.path === '/') {
      router.push('/main')
    }
  }
)

// Следим за изменениями маршрута
watch(
  () => router.currentRoute.value,
  (to) => {
    // Проверяем аутентификацию при каждом переходе
    authStore.checkAuth()
    
    // Если пользователь не аутентифицирован и пытается попасть на защищенную страницу
    if (!authStore.isAuthenticated && to.path === '/main') {
      showError('Вы не авторизованы. Пожалуйста, войдите в систему.')
      router.push('/')
    }
    
    // Если пользователь аутентифицирован и пытается попасть на страницу логина
    if (authStore.isAuthenticated && to.path === '/') {
      router.push('/main')
    }
  },
  { immediate: true }
)
</script>

<style scoped>
/* Глобальные стили для всего приложения */
</style>
