// Хранилище для управления аутентификацией (Pinia)

import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authService from '../services/authService'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const isAuthenticated = ref(false)
  const loading = ref(false)
  const error = ref(null)

  // Регистрация
  const register = async (username, password) => {
    loading.value = true
    error.value = null
    
    try {
      const data = await authService.register(username, password)
      
      // Сохраняем токен в localStorage
      localStorage.setItem('accessToken', data.accessToken)
      
      // Устанавливаем пользователя
      user.value = { username }
      isAuthenticated.value = true
      
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // Вход
  const login = async (username, password) => {
    loading.value = true
    error.value = null
    
    try {
      const data = await authService.login(username, password)
      
      // Сохраняем токен в localStorage
      localStorage.setItem('accessToken', data.accessToken)
      
      // Устанавливаем пользователя
      user.value = { username }
      isAuthenticated.value = true
      
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // Выход
  const logout = async () => {
    try {
      await authService.logout()
      
      // Удаляем токен из localStorage
      localStorage.removeItem('accessToken')
      
      // Сбрасываем состояние
      user.value = null
      isAuthenticated.value = false
      error.value = null
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  // Проверка аутентификации
  const checkAuth = () => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      isAuthenticated.value = true
      // Здесь можно добавить декодирование токена для получения данных пользователя
    }
  }

  // Обновление токена
  const refreshAccessToken = async () => {
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include' // Важно для отправки cookies
      })
      
      if (!response.ok) {
        throw new Error('Не удалось обновить токен')
      }
      
      const data = await response.json()
      localStorage.setItem('accessToken', data.accessToken)
      return data.accessToken
    } catch (error) {
      console.error('Ошибка обновления токена:', error)
      // Если refresh token не действителен, выходим из системы
      await logout()
      throw error
    }
  }

  // Получение токена
  const getToken = () => {
    return localStorage.getItem('accessToken')
  }

  return {
    user,
    isAuthenticated,
    loading,
    error,
    register,
    login,
    logout,
    checkAuth,
    getToken
  }
})