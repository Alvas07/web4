<template>
  <div class="modal-overlay" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2>{{ isLogin ? 'Вход' : 'Регистрация' }}</h2>
        <button @click="handleClose" class="close-button">&times;</button>
      </div>
      
      <div class="modal-body">
        <div class="form-toggle">
          <button 
            @click="isLogin = true" 
            :class="{ active: isLogin }"
            class="toggle-button"
          >
            Вход
          </button>
          <button 
            @click="isLogin = false" 
            :class="{ active: !isLogin }"
            class="toggle-button"
          >
            Регистрация
          </button>
          <div class="toggle-slider" :class="{ 'slide-right': !isLogin }"></div>
        </div>
        
        <form @submit.prevent="handleSubmit" class="auth-form">
          <div class="form-group">
            <label for="username">Логин:</label>
            <input
              id="username"
              v-model="form.username"
              type="text"
              required
              :disabled="loading"
              :class="{ 'invalid': errors.username }"
              @input="validateUsername"
              @blur="validateUsername"
            />
            <div v-if="errors.username" class="error-message">
              {{ errors.username }}
            </div>
          </div>
          
          <div class="form-group">
            <label for="password">Пароль:</label>
            <div class="password-input">
              <input
                id="password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                required
                :disabled="loading"
                :class="{ 'invalid': errors.password }"
                @input="validatePassword"
                @blur="validatePassword"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="password-toggle"
                :aria-label="showPassword ? 'Скрыть пароль' : 'Показать пароль'"
              >
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
              </button>
            </div>
            <div v-if="errors.password" class="error-message">
              {{ errors.password }}
            </div>
          </div>
          
          <div v-if="!isLogin" class="form-group">
            <label for="confirmPassword">Подтверждение пароля:</label>
            <div class="password-input">
              <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                required
                :disabled="loading"
                :class="{ 'invalid': errors.confirmPassword }"
                @input="validateConfirmPassword"
                @blur="validateConfirmPassword"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="password-toggle"
                :aria-label="showPassword ? 'Скрыть пароль' : 'Показать пароль'"
              >
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
              </button>
            </div>
            <div v-if="errors.confirmPassword" class="error-message">
              {{ errors.confirmPassword }}
            </div>
          </div>
          
          <button
            type="submit"
            :disabled="loading"
            class="submit-button"
          >
            {{ loading ? 'Загрузка...' : (isLogin ? 'Войти' : 'Зарегистрироваться') }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { showError } from '../utils/notifications'

const emit = defineEmits(['close', 'login-success'])

const authStore = useAuthStore()

const isLogin = ref(true)
const showPassword = ref(false)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validateUsername = () => {
  const username = form.username.trim()
  
  if (!username) {
    errors.username = ''
    return false
  }
  
  if (username.length < 3 || username.length > 20) {
    errors.username = 'Логин должен быть от 3 до 20 символов'
    return false
  }
  
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    errors.username = 'Логин может содержать только буквы, цифры и подчеркивание'
    return false
  }
  
  // Логин не может состоять только из цифр
  if (/^[0-9]+$/.test(username)) {
    errors.username = 'Логин не может состоять только из цифр'
    return false
  }
  
  // Логин должен содержать хотя бы одну букву
  if (!/[a-zA-Z]/.test(username)) {
    errors.username = 'Логин должен содержать хотя бы одну букву'
    return false
  }
  
  // Логин не должен начинаться с цифры
  if (/^[0-9]/.test(username)) {
    errors.username = 'Логин не может начинаться с цифры'
    return false
  }
  
  // Логин не должен начинаться или заканчиваться подчеркиванием
  if (username.startsWith('_') || username.endsWith('_')) {
    errors.username = 'Логин не может начинаться или заканчиваться подчеркиванием'
    return false
  }
  
  // Логин не должен содержать подряд более одного подчеркивания
  if (username.includes('__')) {
    errors.username = 'Логин не может содержать подряд более одного подчеркивания'
    return false
  }
  
  errors.username = ''
  return true
}

const validatePassword = () => {
  const password = form.password
  
  if (!password) {
    errors.password = ''
    return false
  }
  
  if (password.length < 6 || password.length > 50) {
    errors.password = 'Пароль должен быть от 6 до 50 символов'
    return false
  }
  
  if (!/^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':",./<>?]+$/.test(password)) {
    errors.password = 'Пароль содержит недопустимые символы'
    return false
  }
  
  if (!/[a-zA-Z]/.test(password) || !/[0-9]/.test(password)) {
    errors.password = 'Пароль должен содержать хотя бы одну букву и одну цифру'
    return false
  }
  
  errors.password = ''
  
  // Если это регистрация, проверяем подтверждение пароля
  if (!isLogin.value && form.confirmPassword) {
    validateConfirmPassword()
  }
  
  return true
}

const validateConfirmPassword = () => {
  if (isLogin.value) {
    return true
  }
  
  if (!form.confirmPassword) {
    errors.confirmPassword = ''
    return false
  }
  
  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Пароли не совпадают'
    return false
  }
  
  errors.confirmPassword = ''
  return true
}

const handleClose = () => {
  emit('close')
  // Сбрасываем форму и ошибки
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  errors.username = ''
  errors.password = ''
  errors.confirmPassword = ''
}

const handleSubmit = async () => {
  // Валидация
  const isUsernameValid = validateUsername()
  const isPasswordValid = validatePassword()
  const isConfirmPasswordValid = isLogin.value ? true : validateConfirmPassword()
  
  if (!isUsernameValid || !isPasswordValid || !isConfirmPasswordValid) {
    return
  }
  
  loading.value = true
  
  try {
    if (isLogin.value) {
      // Вход
      await authStore.login(form.username, form.password)
    } else {
      // Регистрация
      await authStore.register(form.username, form.password)
    }
    
    // Эмитируем успех
    emit('login-success')
    handleClose()
  } catch (err) {
    showError(err.message || 'Произошла ошибка. Пожалуйста, попробуйте снова.')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 0;
  max-width: 400px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: #333;
}

.form-toggle {
  display: flex;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 5px;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: relative;
}

.toggle-button {
  flex: 1;
  padding: 0.75rem;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: #666;
  border-radius: 8px;
  transition: all 0.3s ease;
  z-index: 2;
  position: relative;
}

.toggle-button.active {
  color: white;
}

.toggle-slider {
  position: absolute;
  top: 5px;
  left: 5px;
  width: calc(50% - 5px);
  height: calc(100% - 10px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  z-index: 1;
}

.toggle-slider.slide-right {
  transform: translateX(calc(100% + 10px));
}

.modal-body {
  padding: 1.5rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: #333;
}

.form-group input {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.form-group input.invalid {
  border-color: #e74c3c;
  border-width: 2px;
}

.password-input {
  position: relative;
}

.password-input input {
  padding-right: 2.5rem;
}

.password-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: color 0.3s ease;
}

.password-toggle:hover {
  color: #333;
}

.password-toggle:focus {
  outline: none;
}

.password-toggle svg {
  display: block;
}

.error-message {
  color: #e74c3c;
  font-size: 0.875rem;
  margin-top: 0.25rem;
  min-height: 1.2em;
}

.submit-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  padding: 12px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 0.5rem;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>