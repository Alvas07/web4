// Сервис для работы с аутентификацией
import { API_BASE_URL } from '../utils/api';

// Регистрация
export const register = async (username, password) => {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  })
  
  if (!response.ok) {
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 3 запроса в 30 секунд. Пожалуйста, подождите.')
      }
      throw new Error(errorText || 'Слишком много запросов. Максимум 3 запроса в 30 секунд. Пожалуйста, подождите.')
    }
    const errorData = await response.json().catch(() => ({ message: 'Ошибка регистрации' }))
    throw new Error(errorData.message || 'Ошибка регистрации')
  }
  
  return await response.json()
}

// Вход
export const login = async (username, password) => {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  })
  
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Invalid credentials')
    }
    // Обработка rate limit (429 Too Many Requests)
    if (response.status === 429) {
      const errorText = await response.text()
      // Показываем модальное окно вместо обычной ошибки
      if (window.showRateLimitModal) {
        window.showRateLimitModal(errorText || 'Слишком много запросов. Максимум 3 запроса в 30 секунд. Пожалуйста, подождите.')
      }
      throw new Error(errorText || 'Слишком много запросов. Максимум 3 запроса в 30 секунд. Пожалуйста, подождите.')
    }
    const errorData = await response.json().catch(() => ({ message: 'Ошибка входа' }))
    throw new Error(errorData.message || 'Ошибка входа')
  }
  
  return await response.json()
}

// Выход
export const logout = async () => {
  const token = localStorage.getItem('accessToken')
  
  if (token) {
    try {
      // Отправляем запрос на сервер для выхода
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
    } catch (error) {
      // Игнорируем ошибки при выходе
      console.error('Ошибка при выходе:', error)
    }
  }
  
  // Удаляем токен из localStorage
  localStorage.removeItem('accessToken')
  
  return { success: true }
}