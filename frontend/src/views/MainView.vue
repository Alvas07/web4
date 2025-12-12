<template>
  <div v-if="authStore.isAuthenticated">
    <Header :show-logout="true" @logout="handleLogout" />
    <div class="content">
      <div class="top-section">
        <PointForm
          class="controls"
          @point-checked="handlePointChecked"
          @r-changed="handleRChanged"
        />
        <Graph
          ref="graphRef"
          class="graph"
          :r="pointsStore.selectedR"
          @point-clicked="handleGraphPointClick"
        />
      </div>
      <HistoryTable
        class="history"
        :points="pointsStore.allPointsCombined"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { usePointsStore } from '../stores/pointsStore'
import Header from '../components/Header.vue'
import PointForm from '../components/PointForm.vue'
import Graph from '../components/Graph.vue'
import HistoryTable from '../components/HistoryTable.vue'
import { showError } from '../utils/notifications'

const router = useRouter()
const authStore = useAuthStore()
const pointsStore = usePointsStore()

const graphRef = ref(null)

// Обработчик изменения размера окна
const handleResize = () => {
  // Перерисовываем график при изменении размера
  if (graphRef.value) {
    setTimeout(() => {
      graphRef.value.drawCanvas()
    }, 100)
  }
}

// Проверяем аутентификацию при монтировании
onMounted(async () => {
  authStore.checkAuth()
  
  if (!authStore.isAuthenticated) {
    router.push('/')
    return
  }
  
  // Загружаем историю точек
  try {
    await pointsStore.loadHistory()
  } catch (error) {
    showError(error.message || 'Ошибка при загрузке истории')
  }
  
  // Запускаем long-polling для получения новых точек
  pointsStore.startPolling()
  
  // Добавляем обработчик изменения размера
  window.addEventListener('resize', handleResize)
  // Также обрабатываем изменение ориентации на мобильных устройствах
  window.addEventListener('orientationchange', handleResize)
})

onUnmounted(() => {
  // Останавливаем long-polling
  pointsStore.stopPolling()
  
  // Удаляем обработчики при размонтировании
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('orientationchange', handleResize)
})

const handleLogout = () => {
  // Просто удаляем токен и перекидываем на стартовую страницу
  localStorage.removeItem('accessToken')
  // Очищаем хранилище точек
  pointsStore.points = []
  pointsStore.allPoints = []
  pointsStore.selectedR = null
  // Сбрасываем состояние аутентификации напрямую через store
  authStore.user = null
  authStore.isAuthenticated = false
  authStore.error = null
  // Перекидываем на стартовую страницу
  router.push('/').catch(() => {
    // Игнорируем ошибки навигации
  })
}

const handlePointChecked = (newPoints) => {
  // Точки уже добавлены в хранилище, принудительно перерисовываем график
  if (graphRef.value) {
    setTimeout(() => {
      graphRef.value.drawCanvas()
    }, 50)
  }
}

const handleRChanged = (r) => {
  pointsStore.setSelectedR(r)
}

const handleGraphPointClick = (point) => {
  // Точка уже проверена в компоненте Graph
  console.log('Point clicked:', point)
}
</script>

<style scoped>

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1rem;
  gap: 1rem;
  width: 100%;
  box-sizing: border-box;
}

.top-section {
  display: flex;
  gap: 1rem;
  flex: 0 0 auto;
  max-height: 60vh;
  min-height: 0;
}

.controls {
  flex: 0 0 320px;
  width: 320px;
  box-sizing: border-box;
}

.graph {
  flex: 1;
  min-width: 0;
}

.history {
  flex: 0 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* Мобильная верстка - все элементы друг под другом */
@media (max-width: 1037px) {
  .content {
    flex-direction: column;
  }
  
  .top-section {
    flex-direction: column;
    max-height: none;
    width: 100%;
  }
  
  .controls {
    flex: 0 0 auto;
    width: 100%;
    max-width: 100%;
  }
  
  .graph {
    width: 100%;
    min-height: 350px;
    flex: 0 0 auto;
  }
  
  .history {
    width: 100%;
  }
}

@media (max-width: 702px) {
  .content {
    padding: 0.5rem;
    gap: 0.75rem;
  }
  
  .top-section {
    gap: 0.75rem;
  }
  
  .controls {
    width: 100%;
  }
  
  .graph {
    min-height: 300px;
  }
}

/* Обработка очень маленьких экранов */
@media (max-width: 480px) {
  .content {
    padding: 0.25rem;
    gap: 0.5rem;
  }
  
  .top-section {
    gap: 0.5rem;
  }
  
  .graph {
    min-height: 250px;
  }
}

/* Возврат к исходному виду при увеличении размера */
@media (min-width: 1038px) {
  .content {
    flex-direction: column;
  }
  
  .top-section {
    flex-direction: row;
    max-height: 60vh;
  }
  
  .controls {
    flex: 0 0 320px;
    width: 320px;
  }
  
  .graph {
    flex: 1;
    min-width: 0;
  }
  
  .history {
    width: 100%;
  }
}
</style>