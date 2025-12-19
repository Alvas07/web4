// Сервис для работы с точками
import { API_BASE_URL } from '../utils/api';
import { useAuthStore } from '../stores/authStore';

// Преобразование LocalDateTime массива в ISO строку
// LocalDateTime приходит как массив [year, month, day, hour, minute, second, nano]
function parseLocalDateTime(dateTime) {
  if (!dateTime) return null
  
  // Если это уже строка, возвращаем как есть
  if (typeof dateTime === 'string') {
    return dateTime
  }
  
  // Если это массив (формат LocalDateTime от Jackson)
  if (Array.isArray(dateTime) && dateTime.length >= 6) {
    const [year, month, day, hour, minute, second, nano = 0] = dateTime
    // Месяц в LocalDateTime 1-based, в Date 0-based
    const date = new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1000000))
    return date.toISOString()
  }
  
  // Если это Date объект
  if (dateTime instanceof Date) {
    return dateTime.toISOString()
  }
  
  return String(dateTime)
}

// Проверка точек через форму
export async function checkPointsFromForm(points, token) {
  try {
    let response = await fetch(`${API_BASE_URL}/points/form`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ points })
    })
    
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
      }
      throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
    }
    
    // Если токен недействителен, попробуем обновить его
    if (response.status === 401 || response.status === 403) {
      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        // Повторяем запрос с новым токеном
        response = await fetch(`${API_BASE_URL}/points/form`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${newToken}`
          },
          body: JSON.stringify({ points })
        })
        
        // Проверяем rate limit после повторного запроса
        if (response.status === 429) {
          const errorText = await response.text()
          // Показываем модальное окно вместо обычной ошибки
          if (window.showRateLimitModal) {
            window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
          }
          throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
        }
      } catch (refreshError) {
        throw new Error('Требуется повторная авторизация')
      }
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Ошибка проверки точек')
    }
    
    const data = await response.json()
    console.log('Received data from form:', data)
    
    // Преобразуем данные из формата DTO в нужный формат
    const transformed = Array.isArray(data) ? data.map(entry => {
      // Обрабатываем разные форматы данных (может быть entry.point или прямые поля)
      const pointData = entry.point || entry
      const x = pointData.x !== undefined ? pointData.x : entry.x
      const y = pointData.y !== undefined ? pointData.y : entry.y
      const r = pointData.r !== undefined ? pointData.r : entry.r
      
      const createdAtStr = parseLocalDateTime(entry.createdAt)
      const idStr = x + '_' + y + '_' + r + '_' + createdAtStr
      
      return {
        id: idStr,
        x: x,
        y: y,
        r: r,
        hit: entry.hit,
        username: entry.username,
        timestamp: createdAtStr,
        executionTime: entry.execTime,
        // Сохраняем оригинальную структуру для графика
        point: entry.point || { x, y, r },
        createdAt: createdAtStr,
        execTime: entry.execTime
      }
    }) : []
    console.log('Transformed data from form:', transformed)
    return transformed
  } catch (error) {
    console.error('Ошибка проверки точек:', error)
    throw error
  }
}

// Проверка точек через график
export async function checkPointsFromGraph(points, graphXMin, graphXMax, graphYMin, graphYMax, token) {
  try {
    let response = await fetch(`${API_BASE_URL}/points/graph`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ points, graphXMin, graphXMax, graphYMin, graphYMax })
    })
    
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
      }
      throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
    }
    
    // Если токен недействителен, попробуем обновить его
    if (response.status === 401 || response.status === 403) {
      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        // Повторяем запрос с новым токеном
        response = await fetch(`${API_BASE_URL}/points/graph`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${newToken}`
          },
          body: JSON.stringify({ points, graphXMin, graphXMax, graphYMin, graphYMax })
        })
        
        // Проверяем rate limit после повторного запроса
        if (response.status === 429) {
          const errorText = await response.text()
          // Показываем модальное окно вместо обычной ошибки
          if (window.showRateLimitModal) {
            window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
          }
          throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
        }
      } catch (refreshError) {
        throw new Error('Требуется повторная авторизация')
      }
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Ошибка проверки точек через график')
    }
    
    const data = await response.json()
    console.log('Received data from graph:', data)
    
    // Преобразуем данные из формата DTO в нужный формат
    const transformed = Array.isArray(data) ? data.map(entry => {
      // Обрабатываем разные форматы данных (может быть entry.point или прямые поля)
      const pointData = entry.point || entry
      const x = pointData.x !== undefined ? pointData.x : entry.x
      const y = pointData.y !== undefined ? pointData.y : entry.y
      const r = pointData.r !== undefined ? pointData.r : entry.r
      
      const createdAtStr = parseLocalDateTime(entry.createdAt)
      const idStr = x + '_' + y + '_' + r + '_' + createdAtStr
      
      return {
        id: idStr,
        x: x,
        y: y,
        r: r,
        hit: entry.hit,
        username: entry.username,
        timestamp: createdAtStr,
        executionTime: entry.execTime,
        // Сохраняем оригинальную структуру для графика
        point: entry.point || { x, y, r },
        createdAt: createdAtStr,
        execTime: entry.execTime
      }
    }) : []
    console.log('Transformed data from graph:', transformed)
    return transformed
  } catch (error) {
    console.error('Ошибка проверки точек через график:', error)
    throw error
  }
}

// Получение истории
export async function getHistory(lastCreatedAt = null, lastId = null, page = 1, pageSize = 20, token, needTotalCount = false) {
  try {
    const requestBody = { page, pageSize, needTotalCount }
    // Преобразуем дату в Unix timestamp (миллисекунды)
    if (lastCreatedAt) {
      if (lastCreatedAt instanceof Date) {
        requestBody.lastCreatedAt = lastCreatedAt.getTime()
      } else if (typeof lastCreatedAt === 'string') {
        // Если это строка, пытаемся распарсить как дату
        requestBody.lastCreatedAt = new Date(lastCreatedAt).getTime()
      } else if (typeof lastCreatedAt === 'number') {
        // Если это уже число, используем как есть
        requestBody.lastCreatedAt = lastCreatedAt
      }
    }
    if (lastId) requestBody.lastId = lastId
    
    let response = await fetch(`${API_BASE_URL}/history`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestBody)
    })
    
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
      }
      throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
    }
    
    // Если токен недействителен, попробуем обновить его
    if (response.status === 401 || response.status === 403) {
      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        // Повторяем запрос с новым токеном
        response = await fetch(`${API_BASE_URL}/history`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${newToken}`
          },
          body: JSON.stringify(requestBody)
        })
        
        // Проверяем rate limit после повторного запроса
        if (response.status === 429) {
          const errorText = await response.text()
          // Показываем модальное окно вместо обычной ошибки
          if (window.showRateLimitModal) {
            window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
          }
          throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
        }
      } catch (refreshError) {
        throw new Error('Требуется повторная авторизация')
      }
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Ошибка получения истории')
    }
    
    const data = await response.json()
    
    // Проверяем, пришел ли ответ с пагинацией (HistoryResponseDTO) или просто массив (для обратной совместимости)
    let entries = Array.isArray(data) ? data : (data.entries || [])
    const totalCount = data.totalCount !== undefined ? data.totalCount : null
    
    // Преобразуем данные из формата DTO в нужный формат
    const transformed = entries.map(entry => {
      // Обрабатываем разные форматы данных (может быть entry.point или прямые поля)
      const pointData = entry.point || entry
      const x = pointData.x !== undefined ? pointData.x : entry.x
      const y = pointData.y !== undefined ? pointData.y : entry.y
      const r = pointData.r !== undefined ? pointData.r : entry.r
      
      const createdAtStr = parseLocalDateTime(entry.createdAt)
      const idStr = x + '_' + y + '_' + r + '_' + createdAtStr
      
      return {
        id: idStr,
        x: x,
        y: y,
        r: r,
        hit: entry.hit,
        username: entry.username,
        timestamp: createdAtStr,
        executionTime: entry.execTime,
        // Сохраняем оригинальную структуру для графика
        point: entry.point || { x, y, r },
        createdAt: createdAtStr,
        execTime: entry.execTime
      }
    })
    
    // Если есть totalCount, возвращаем объект с записями и общим количеством
    if (totalCount !== null) {
      return { entries: transformed, totalCount }
    }
    
    // Иначе возвращаем просто массив для обратной совместимости
    return transformed
  } catch (error) {
    console.error('Ошибка получения истории:', error)
    throw error
  }
}

// Очистка истории
export async function clearHistory(username, token) {
  try {
    let response = await fetch(`${API_BASE_URL}/history/${username}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
      }
      throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
    }
    
    // Если токен недействителен, попробуем обновить его
    if (response.status === 401 || response.status === 403) {
      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        // Повторяем запрос с новым токеном
        response = await fetch(`${API_BASE_URL}/history/${username}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${newToken}`
          }
        })
        
        // Проверяем rate limit после повторного запроса
        if (response.status === 429) {
          const errorText = await response.text()
          // Показываем модальное окно вместо обычной ошибки
          if (window.showRateLimitModal) {
            window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 5 запросов в секунду.')
          }
          throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
        }
      } catch (refreshError) {
        throw new Error('Требуется повторная авторизация')
      }
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Ошибка очистки истории')
    }
    
    return true
  } catch (error) {
    console.error('Ошибка очистки истории:', error)
    throw error
  }
}

// Long-polling для получения новых точек от других пользователей
export async function pollNewPoints(lastCreatedAt = null, lastId = null, timeoutSeconds = 30, token) {
  try {
    const requestBody = { timeoutSeconds }
    // Преобразуем дату в Unix timestamp (миллисекунды)
    if (lastCreatedAt) {
      if (lastCreatedAt instanceof Date) {
        requestBody.lastCreatedAt = lastCreatedAt.getTime()
      } else if (typeof lastCreatedAt === 'string') {
        // Если это строка, пытаемся распарсить как дату
        requestBody.lastCreatedAt = new Date(lastCreatedAt).getTime()
      } else if (typeof lastCreatedAt === 'number') {
        // Если это уже число, используем как есть
        requestBody.lastCreatedAt = lastCreatedAt
      }
    }
    if (lastId) requestBody.lastId = lastId
    
    let response = await fetch(`${API_BASE_URL}/history/poll`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestBody)
    })
    
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 10 запросов в секунду.')
      }
      throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
    }
    
    // Если токен недействителен, попробуем обновить его
    if (response.status === 401 || response.status === 403) {
      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        // Повторяем запрос с новым токеном
        response = await fetch(`${API_BASE_URL}/history/poll`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${newToken}`
          },
          body: JSON.stringify(requestBody)
        })
        
        // Проверяем rate limit после повторного запроса
        if (response.status === 429) {
          const errorText = await response.text()
          if (window.showRateLimitModal) {
            window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 10 запросов в секунду.')
          }
          throw new Error(errorText || 'Слишком много запросов. Пожалуйста, подождите немного.')
        }
      } catch (refreshError) {
        throw new Error('Требуется повторная авторизация')
      }
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Ошибка при long-polling')
    }
    
    const data = await response.json()
    
    // Преобразуем данные из формата DTO в нужный формат
    return Array.isArray(data) ? data.map(entry => {
      // Обрабатываем разные форматы данных (может быть entry.point или прямые поля)
      const pointData = entry.point || entry
      const x = pointData.x !== undefined ? pointData.x : entry.x
      const y = pointData.y !== undefined ? pointData.y : entry.y
      const r = pointData.r !== undefined ? pointData.r : entry.r
      
      const createdAtStr = parseLocalDateTime(entry.createdAt)
      const idStr = x + '_' + y + '_' + r + '_' + createdAtStr
      
      return {
        id: idStr,
        x: x,
        y: y,
        r: r,
        hit: entry.hit,
        username: entry.username,
        timestamp: createdAtStr,
        executionTime: entry.execTime,
        // Сохраняем оригинальную структуру для графика
        point: entry.point || { x, y, r },
        createdAt: createdAtStr,
        execTime: entry.execTime
      }
    }) : []
  } catch (error) {
    console.error('Ошибка long-polling:', error)
    throw error
  }
}