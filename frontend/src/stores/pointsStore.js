// Хранилище для управления точками (Pinia)

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as pointsService from '../services/pointsService'
import { useAuthStore } from './authStore'
import { showInfo } from '../utils/notifications'

export const usePointsStore = defineStore('points', () => {
  const points = ref([])
  const allPoints = ref([])
  const selectedR = ref(null)
  const loading = ref(false)
  const error = ref(null)
  
  // Вычисляемые свойства
  const ownPoints = computed(() => {
    const authStore = useAuthStore()
    if (!authStore.user) return []
    return points.value.filter(point => point.username === authStore.user.username)
  })
  
  const otherPoints = computed(() => {
    const authStore = useAuthStore()
    if (!authStore.user) return []
    return allPoints.value.filter(point => point.username !== authStore.user.username)
  })
  
  const normalizePoint = (point) => {
    if (!point) return null
    const createdAt = point.createdAt || point.timestamp || new Date().toISOString()
    const id = point.id || `${point.x}_${point.y}_${point.r}_${createdAt}`
    return { ...point, createdAt, timestamp: point.timestamp || createdAt, id }
  }

  const addPoints = (newPoints) => {
    if (!Array.isArray(newPoints) || newPoints.length === 0) return

    const normalized = newPoints.map(normalizePoint).filter(Boolean)
    const existingIds = new Set(points.value.map(p => p.id))
    const uniqueNewPoints = normalized.filter(point => !existingIds.has(point.id))

    if (uniqueNewPoints.length > 0) {
      const updatedPoints = [...points.value, ...uniqueNewPoints]
      updatedPoints.sort((a, b) => {
        const timeA = new Date(a.createdAt || a.timestamp || 0).getTime()
        const timeB = new Date(b.createdAt || b.timestamp || 0).getTime()
        return timeB - timeA
      })

      points.value = updatedPoints
    }
  }
  
  const allPointsCombined = computed(() => {
    const result = Array.isArray(points.value) ? [...points.value] : []
    return result
  })
  
  const checkPointFromForm = async (point) => {
    loading.value = true
    error.value = null
    
    try {
      const authStore = useAuthStore()
      const token = authStore.getToken()
      
      if (!token) {
        throw new Error('Не авторизован')
      }
      
      const result = await pointsService.checkPointsFromForm([point], token)
      addPoints(Array.isArray(result) ? result : [])
      return result
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }
  
  const checkPointFromGraph = async (point) => {
    loading.value = true
    error.value = null
    
    try {
      const authStore = useAuthStore()
      const token = authStore.getToken()
      
      if (!token) {
        throw new Error('Не авторизован')
      }
      
      const result = await pointsService.checkPointsFromGraph([point], -6, 6, -6, 6, token)
      addPoints(Array.isArray(result) ? result : [])
      return result
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }
  
  const loadHistory = async () => {
    loading.value = true
    error.value = null
    
    try {
      const authStore = useAuthStore()
      const token = authStore.getToken()
      
      if (!token) {
        throw new Error('Не авторизован')
      }
      
      const history = await pointsService.getHistory(null, null, 50, token)
      points.value = []
      addPoints(Array.isArray(history) ? history : [])
      return history
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }
  
  const clearHistory = async () => {
    loading.value = true
    error.value = null
    
    try {
      const authStore = useAuthStore()
      const token = authStore.getToken()
      
      if (!token || !authStore.user) {
        throw new Error('Не авторизован')
      }
      
      await pointsService.clearHistory(authStore.user.username, token)
      
      // Очищаем только точки текущего пользователя
      const currentUsername = authStore.user.username
      points.value = points.value.filter(point => point.username !== currentUsername)
      
      return true
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }
  
  // Установка выбранного R
  const setSelectedR = (r) => {
    selectedR.value = r
  }
  
  // Добавление точки от другого пользователя
  const addOtherPoint = (point) => {
    allPoints.value = [point, ...allPoints.value]
    
    // Показываем уведомление о новой точке
    const authStore = useAuthStore()
    if (authStore.user && point.username !== authStore.user.username) {
      showInfo(`Поступили точки от пользователя ${point.username}`)
    }
  }
  
  // Long-polling для получения новых точек
  let pollingInterval = null
  let isPolling = false
  
  const startPolling = () => {
    if (isPolling) return
    isPolling = true
    
    const poll = async () => {
      try {
        const authStore = useAuthStore()
        const token = authStore.getToken()
        
        if (!token || !authStore.user) {
          stopPolling()
          return
        }
        
        // Получаем последнюю точку от другого пользователя для определения времени последнего обновления
        const otherUserPoints = points.value.filter(p => p.username !== authStore.user?.username)
        // Сортируем по времени создания (самые новые первыми)
        const sortedOtherPoints = [...otherUserPoints].sort((a, b) => {
          const timeA = new Date(a.createdAt || a.timestamp).getTime()
          const timeB = new Date(b.createdAt || b.timestamp).getTime()
          return timeB - timeA
        })
        const lastPoint = sortedOtherPoints.length > 0 ? sortedOtherPoints[0] : null
        const lastCreatedAt = lastPoint?.createdAt || lastPoint?.timestamp || null
        
        // Выполняем long-polling (таймаут 25 секунд)
        const newPoints = await pointsService.pollNewPoints(lastCreatedAt, null, 25, token)
        
        if (newPoints && newPoints.length > 0) {
          // Добавляем новые точки напрямую (функция сама отфильтрует дубликаты и отсортирует)
          addPoints(newPoints)
          
          // Показываем уведомление о новых точках
          const uniqueUsers = [...new Set(newPoints.map(p => p.username))]
          uniqueUsers.forEach(username => {
            if (username !== authStore.user?.username) {
              showInfo(`Новые точки от пользователя ${username}`)
            }
          })
        }
      } catch (err) {
        // Игнорируем ошибки polling, но логируем их
        console.error('Ошибка при polling:', err)
        // Если ошибка авторизации, останавливаем polling
        if (err.message && err.message.includes('авторизация')) {
          stopPolling()
        }
      }
    }
    
    // Запускаем polling сразу и затем каждые 30 секунд
    poll()
    pollingInterval = setInterval(poll, 30000)
  }
  
  const stopPolling = () => {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      pollingInterval = null
    }
    isPolling = false
  }
  
  return {
    points,
    allPoints,
    selectedR,
    loading,
    error,
    ownPoints,
    otherPoints,
    checkPointFromForm,
    checkPointFromGraph,
    loadHistory,
    clearHistory,
    setSelectedR,
    addOtherPoint,
    startPolling,
    stopPolling
  }
})