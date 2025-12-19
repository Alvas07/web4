<template>
  <div class="clock">
    <div class="clock-date">{{ currentDate }}</div>
    <div class="clock-time">{{ currentTime }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const currentTime = ref(new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', second: '2-digit' }))
const currentDate = ref(new Date().toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' }))

let timer = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

onMounted(() => {
  timer = setInterval(updateTime, 1000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.clock-date {
  font-size: 0.9rem;
  font-weight: 400;
  color: var(--text);
  opacity: 0.8;
}

.clock-time {
  font-size: 1.2rem;
  font-weight: 500;
  color: var(--text);
}
</style>