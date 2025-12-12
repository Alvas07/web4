<template>
  <header class="header">
    <div class="header-left">
      <h1>Дядев Владислав Александрович • P3231 • Вариант 4824</h1>
    </div>
    <div class="header-right" v-if="showLogout">
      <div class="user-info">
        <span class="username">{{ authStore.user?.username || 'Пользователь' }}</span>
      </div>
      <button @click="handleLogout" class="logout-button">
        Выйти
      </button>
    </div>
    <div class="header-right" v-else>
      <Clock />
    </div>
  </header>
</template>

<script setup>
import Clock from './Clock.vue'
import { useAuthStore } from '../stores/authStore'

defineProps({
  showLogout: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['logout'])
const authStore = useAuthStore()

const handleLogout = () => {
  emit('logout')
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 1200px;
  margin: 1rem auto 0 auto;
  padding: 0 1rem;
  position: relative;
}

.header-left {
  flex: 1 1 50%;
}

.header-left h1 {
  font-size: 1.2rem;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--text);
  margin: 0;
}

.header-right {
  flex: 1 1 50%;
  margin-left: auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 1rem;
}

.header-right #clock {
  font-size: 1.2rem;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-weight: 600;
  color: var(--text);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.username {
  font-weight: 600;
  color: var(--text);
  background: rgba(0, 0, 0, 0.05);
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.9rem;
}

.logout-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  padding: 8px 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.3s ease;
  text-decoration: none;
  display: inline-block;
  margin: 2px;
}

.logout-button:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  text-decoration: none;
}

/* Мобильная верстка - < 703px */
@media (max-width: 702px) {
  .header {
    padding: 0 0.5rem;
    width: 50%;
    max-width: 50%;
    margin-left: auto;
    margin-right: auto;
  }
  
  .header-left h1 {
    font-size: 0.9rem;
  }
  
  .header-right {
    gap: 0.5rem;
  }
  
  .username {
    font-size: 0.75rem;
    padding: 0.2rem 0.5rem;
  }
  
  .logout-button {
    padding: 6px 12px;
    font-size: 0.85rem;
  }
}
</style>