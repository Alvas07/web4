<template>
  <Header />
  <div class="welcome-card">
    <h1 class="welcome-title">Добро пожаловать!</h1>
    <p class="welcome-text">Пожалуйста, войдите в систему или зарегистрируйтесь</p>
    <button @click="showAuthModal = true" class="btn btn-primary register-button">
      Войти / Зарегистрироваться
    </button>
  </div>
  
  <AuthModal
    v-if="showAuthModal"
    @close="showAuthModal = false"
    @login-success="handleLoginSuccess"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import AuthModal from '../components/AuthModal.vue'
import Header from '../components/Header.vue'

const router = useRouter()
const authStore = useAuthStore()

const showAuthModal = ref(false)

// Проверяем аутентификацию при монтировании
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/main')
  }
})

const handleLoginSuccess = () => {
  showAuthModal.value = false
  router.push('/main')
}
</script>

<style scoped>
.welcome-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  padding: 2rem;
  width: 100%;
  max-width: 400px;
  text-align: center;
  margin: 2rem auto;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.register-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.welcome-title {
  color: #2c3e50;
  font-size: 2rem;
  margin-bottom: 1rem;
}

.welcome-text {
  color: #666;
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

.register-button {
  padding: 12px 24px;
  font-size: 1.1rem;
  border-radius: 12px;
}
</style>