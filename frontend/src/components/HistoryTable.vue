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
          class="btn btn-danger"
          @click="clearHistory"
          :disabled="loading"
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
          <tr v-for="entry in points" :key="entry.id">
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
            <td>{{ entry.x }}</td>
            <td>{{ entry.y }}</td>
            <td>{{ entry.r }}</td>
            <td>{{ entry.username }}</td>
            <td>{{ entry.executionTime }}</td>
          </tr>
          <tr v-if="points.length === 0">
            <td colspan="7" class="empty-state">
              История проверок пуста
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { usePointsStore } from '../stores/pointsStore'
import { useAuthStore } from '../stores/authStore'
import { showError, showSuccess, showInfo } from '../utils/notifications'
import * as pointsService from '../services/pointsService'

const pointsStore = usePointsStore()
const authStore = useAuthStore()

const loading = false
const testingRateLimit = ref(false)

// Проверяем, находимся ли мы в режиме разработки
const isDevelopment = computed(() => {
  return import.meta.env.DEV || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
})

const points = computed(() => {
  // Используем allPointsCombined, который уже отсортирован
  const pts = pointsStore.allPointsCombined || []
  console.log('HistoryTable points computed:', pts, 'length:', pts.length)
  // Возвращаем отсортированный массив (самые новые первыми)
  return pts
})

const formatDate = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString('ru-RU')
}

const clearHistory = async () => {
  if (!confirm('Вы уверены, что хотите очистить историю?')) {
    return
  }
  
  try {
    await pointsStore.clearHistory()
    showSuccess('История очищена')
  } catch (error) {
    showError(error.message || 'Ошибка при очистке истории')
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
  overflow: auto;
  border: 1px solid #eaeaea;
  border-radius: 4px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  background: white;
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
  }
  
  .table th,
  .table td {
    padding: 0.5rem;
  }
  
  .result-badge {
    padding: 0.125rem 0.25rem;
    font-size: 0.75rem;
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
</style>