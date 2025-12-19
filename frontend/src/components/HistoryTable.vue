<template>
  <div class="history-table">
    <div class="table-header">
      <h2 class="table-title">История проверок</h2>
      <div class="table-actions">
        <button 
          v-if="isDevelopment"
          class="btn btn-test"
          @click="testRateLimit"
          :disabled="testingRateLimit"
        >
          {{ testingRateLimit ? 'Тестирование...' : 'Тест Rate Limit' }}
        </button>
        <button 
          class="btn btn-clear-history"
          @click="clearHistory"
          :disabled="loading || clearing"
        >
          Очистить историю
        </button>
      </div>
    </div>
    
    <div class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>Время</th>
            <th>Результат</th>
            <th>X</th>
            <th>Y</th>
            <th>R</th>
            <th>Владелец</th>
            <th>Время выполнения (мс)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in paginatedPoints" :key="entry.id">
            <td>{{ formatDate(entry.timestamp) }}</td>
            <td>
              <span
                class="result-badge"
                :class="{ 'hit': entry.hit, 'miss': !entry.hit }"
              >
                <span class="result-emoji">{{ entry.hit ? '🎯' : '❌' }}</span>
                {{ entry.hit ? 'Попал' : 'Мимо' }}
              </span>
            </td>
            <td>{{ formatNumber(entry.x) }}</td>
            <td>{{ formatNumber(entry.y) }}</td>
            <td>{{ formatNumber(entry.r) }}</td>
            <td>{{ entry.username }}</td>
            <td>{{ entry.executionTime }}</td>
          </tr>
          <tr v-if="paginatedPoints.length === 0 && !loading">
            <td colspan="7" class="empty-state">
              История проверок пуста
            </td>
          </tr>
          <tr v-if="loading">
            <td colspan="7" class="empty-state">
              Загрузка...
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- Пагинация -->
    <div v-if="totalPages > 1" class="pagination">
      <button 
        class="pagination-btn"
        @click="goToPage(currentPage - 1)"
        :disabled="currentPage === 1 || loading"
      >
        ← Назад
      </button>
      <span class="pagination-info">
        Страница {{ currentPage }} из {{ totalPages }} (всего записей: {{ totalRecords }})
      </span>
      <button 
        class="pagination-btn"
        @click="goToPage(currentPage + 1)"
        :disabled="currentPage === totalPages || loading"
      >
        Вперед →
      </button>
    </div>
    
    <ConfirmModal
      :show="showConfirmModal"
      title="Подтверждение очистки"
      message="Вы уверены, что хотите очистить историю? Это действие нельзя отменить."
      confirm-text="Очистить"
      :loading="clearing"
      @confirm="handleConfirmClear"
      @cancel="showConfirmModal = false"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { usePointsStore } from '../stores/pointsStore'
import { useAuthStore } from '../stores/authStore'
import { showError, showSuccess, showInfo } from '../utils/notifications'
import * as pointsService from '../services/pointsService'
import ConfirmModal from './ConfirmModal.vue'

const pointsStore = usePointsStore()
const authStore = useAuthStore()

const loading = ref(false)
const testingRateLimit = ref(false)
const currentPage = ref(1)
const pageSize = 15
const totalRecords = ref(0)
const allHistoryPoints = ref([])
const showConfirmModal = ref(false)
const clearing = ref(false)

// Проверяем, находимся ли мы в режиме разработки
const isDevelopment = computed(() => {
  return import.meta.env.DEV || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
})

// Загружаем историю с пагинацией
const loadHistoryPage = async (page = 1) => {
  loading.value = true
  try {
    const token = authStore.getToken()
    if (!token) {
      throw new Error('Не авторизован')
    }
    
    // Используем пагинацию на бэкенде
    const result = await pointsService.getHistory(null, null, page, pageSize, token, true)
    
    // Проверяем формат ответа (может быть объект с entries и totalCount или просто массив)
    if (result && typeof result === 'object' && 'entries' in result) {
      allHistoryPoints.value = result.entries || []
      totalRecords.value = result.totalCount || 0
    } else {
      // Обратная совместимость - если пришел просто массив
      allHistoryPoints.value = Array.isArray(result) ? result : []
      totalRecords.value = allHistoryPoints.value.length
    }
    
    // НЕ обновляем store.points здесь, чтобы избежать бесконечного цикла
    // Store обновляется только при добавлении новых точек через checkPointFromForm/checkPointFromGraph
  } catch (error) {
    showError(error.message || 'Ошибка при загрузке истории')
  } finally {
    loading.value = false
  }
}

// Вычисляем пагинированные точки (теперь данные уже приходят с бэкенда пагинированными)
const paginatedPoints = computed(() => {
  return allHistoryPoints.value
})

const totalPages = computed(() => {
  return Math.ceil(totalRecords.value / pageSize)
})

const goToPage = async (page) => {
  if (page < 1 || page > totalPages.value || loading.value) return
  currentPage.value = page
  // Загружаем данные для новой страницы с бэкенда
  await loadHistoryPage(page)
}

// Загружаем историю при монтировании
onMounted(async () => {
  await loadHistoryPage(1)
})

// Следим за изменениями в store только для новых точек (не для всех изменений)
// Используем флаг, чтобы избежать бесконечных циклов
let isUpdatingFromStore = false
watch(() => pointsStore.allPointsCombined, async (newPoints, oldPoints) => {
  // Пропускаем обновление, если мы сами обновляем store
  if (isUpdatingFromStore) return
  
  // Проверяем, действительно ли появились новые точки
  if (Array.isArray(newPoints) && Array.isArray(oldPoints)) {
    const newIds = new Set(newPoints.map(p => p.id))
    const oldIds = new Set(oldPoints.map(p => p.id))
    const hasNewPoints = newPoints.some(p => !oldIds.has(p.id))
    
    // Если есть новые точки и мы на первой странице, обновляем
    if (hasNewPoints && currentPage.value === 1 && !loading.value) {
      await loadHistoryPage(1)
    }
  }
}, { deep: true })

const formatDate = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString('ru-RU')
}

const formatNumber = (value) => {
  if (value === null || value === undefined) return ''
  const num = typeof value === 'number' ? value : parseFloat(value)
  if (isNaN(num)) return value
  // Всегда показываем 3 знака после запятой
  return num.toFixed(3)
}

const clearHistory = () => {
  showConfirmModal.value = true
}

const handleConfirmClear = async () => {
  clearing.value = true
  
  try {
    const currentUsername = authStore.user?.username
    if (!currentUsername) {
      showError('Не авторизован')
      showConfirmModal.value = false
      clearing.value = false
      return
    }
    
    // Сначала удаляем на бэкенде
    await pointsStore.clearHistory()
    
    // Удаляем только свои точки из локального массива истории
    const beforeCount = allHistoryPoints.value.length
    allHistoryPoints.value = allHistoryPoints.value.filter(point => point.username !== currentUsername)
    const removedCount = beforeCount - allHistoryPoints.value.length
    
    // Обновляем общее количество записей
    if (totalRecords.value > 0) {
      totalRecords.value = Math.max(0, totalRecords.value - removedCount)
    }
    
    // Если текущая страница пуста и есть другие страницы, переходим на первую
    if (allHistoryPoints.value.length === 0 && currentPage.value > 1) {
      currentPage.value = 1
      await loadHistoryPage(1)
    } else if (allHistoryPoints.value.length === 0 && totalRecords.value > 0) {
      // Если все точки на текущей странице удалены, но есть еще записи, перезагружаем
      await loadHistoryPage(1)
    }
    
    showSuccess('История очищена')
    showConfirmModal.value = false
  } catch (error) {
    showError(error.message || 'Ошибка при очистке истории')
  } finally {
    clearing.value = false
  }
}

// Тест rate limiter - отправляем много запросов одновременно
const testRateLimit = async () => {
  testingRateLimit.value = true
  const token = authStore.getToken()
  
  if (!token) {
    showError('Необходима авторизация для теста')
    testingRateLimit.value = false
    return
  }
  
  try {
    showInfo('Отправляю 200 запросов быстро для теста rate limiter...')
    
    // Отправляем запросы пакетами без ожидания всех сразу
    let rateLimitCount = 0
    let successCount = 0
    let errorCount = 0
    
    // Отправляем запросы пакетами по 50 штук
    const batchSize = 50
    const totalRequests = 200
    
    for (let batch = 0; batch < totalRequests / batchSize; batch++) {
      const promises = []
      for (let i = 0; i < batchSize; i++) {
        promises.push(
          pointsService.checkPointsFromForm([{
            x: -2,
            y: 1,
            r: 1
          }], token).then(() => {
            successCount++
          }).catch(error => {
            errorCount++
            if (error.message && error.message.includes('Слишком много запросов')) {
              rateLimitCount++
              throw error // Пробрасываем дальше, чтобы остановить дальнейшие запросы
            }
            return { error: error.message }
          })
        )
      }
      
      // Ожидаем только текущий пакет
      try {
        await Promise.all(promises)
      } catch (error) {
        if (error.message && error.message.includes('Слишком много запросов')) {
          rateLimitCount++
          break // Останавливаем, если получили rate limit
        }
      }
      
      // Небольшая задержка между пакетами
      if (batch < totalRequests / batchSize - 1) {
        await new Promise(resolve => setTimeout(resolve, 10))
      }
    }
    
    const results = { rateLimitCount, successCount, errorCount }
    
    // Проверяем результаты
    if (rateLimitCount > 0) {
      showSuccess(`Rate limiter работает! Получено ${rateLimitCount} ошибок "Too Many Requests". Успешно: ${successCount}, Ошибок: ${errorCount}`)
    } else if (errorCount > 0) {
      showInfo(`Получено ${errorCount} ошибок, но не rate limit. Успешно: ${successCount}`)
    } else {
      showInfo(`Все ${successCount} запросов прошли успешно. Возможно, rate limiter еще не сработал или лимит слишком высокий.`)
    }
  } catch (error) {
    if (error.message && error.message.includes('Слишком много запросов')) {
      showSuccess('Rate limiter работает! Получена ошибка 429')
    } else {
      showError(error.message || 'Ошибка при тестировании rate limiter')
    }
  } finally {
    testingRateLimit.value = false
  }
}
</script>

<style scoped>
.history-table {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
  box-sizing: border-box;
}

.table-title {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.table-controls {
  margin-bottom: 1rem;
  display: flex;
  justify-content: flex-end;
}

.table-container {
  flex: 1;
  overflow-x: auto;
  overflow-y: auto;
  border: 1px solid #eaeaea;
  border-radius: 4px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.table {
  width: 100%;
  max-width: 100%;
  border-collapse: collapse;
  background: white;
  table-layout: auto;
}

.table th,
.table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #eaeaea;
}

.table th {
  background-color: #f8f9fa;
  font-weight: bold;
  color: #2c3e50;
  position: sticky;
  top: 0;
}

.table tbody tr:hover {
  background-color: #f5f5f5;
}

.result-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.result-emoji {
  font-size: 1rem;
}

.result-badge.hit {
  background-color: #67c23a;
  color: white;
}

.result-badge.miss {
  background-color: #f56c6c;
  color: white;
}

.empty-state {
  padding: 2rem !important;
  text-align: center !important;
  color: #909399;
  font-style: italic;
  height: 200px !important;
}

@media (max-width: 768px) {
  .table {
    font-size: 0.875rem;
    min-width: 600px; /* Минимальная ширина для таблицы */
  }
  
  .table th,
  .table td {
    padding: 0.5rem;
    white-space: nowrap;
  }
  
  .result-badge {
    padding: 0.125rem 0.25rem;
    font-size: 0.75rem;
  }
}

@media (max-width: 600px) {
  .history-table {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
  }
  
  .table-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  .table {
    font-size: 0.75rem;
    min-width: 500px;
  }
  
  .table th,
  .table td {
    padding: 0.375rem;
  }
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.table-title {
  margin: 0;
  color: #2c3e50;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn-test {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
  color: white;
}

.btn-test:hover {
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
  transform: translateY(-2px);
}

@media (max-width: 1037px) {
  .table-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .table-title {
    width: 100%;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding: 1rem;
  flex-wrap: wrap;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.3s ease;
}

.pagination-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.pagination-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
}

.pagination-info {
  font-size: 0.875rem;
  color: #2c3e50;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .pagination {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .pagination-info {
    order: -1;
    text-align: center;
  }
}

.btn-clear-history {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-clear-history:hover:not(:disabled) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%) !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.btn-clear-history:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>