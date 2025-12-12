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
            />
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
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="password-toggle"
              >
                👁
              </button>
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
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="password-toggle"
              >
                👁
              </button>
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

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  // Валидация
  if (!form.username || !form.password) {
    showError('Пожалуйста, заполните все поля')
    return
  }
  
  if (!isLogin.value && form.password !== form.confirmPassword) {
    showError('Пароли не совпадают')
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
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.password-input {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
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