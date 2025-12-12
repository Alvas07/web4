// Утилита для показа уведомлений

let notificationsContainer = null

// Создание контейнера для уведомлений
function createNotificationsContainer() {
  if (notificationsContainer) return
  
  notificationsContainer = document.createElement('div')
  notificationsContainer.id = 'notifications-container'
  document.body.appendChild(notificationsContainer)
}

// Показ уведомления
export function showNotification(message, type = 'error', timeout = 4000) {
  // Создаем контейнер, если он еще не создан
  createNotificationsContainer()
  
  const el = document.createElement('div')
  el.className = `notification ${type}`
  el.textContent = message
  notificationsContainer.appendChild(el)
  
  // Анимированное появление
  requestAnimationFrame(() => el.classList.add('show'))
  
  // Автоматическое удаление через timeout
  setTimeout(() => {
    el.classList.remove('show')
    setTimeout(() => {
      if (el.parentNode === notificationsContainer) {
        notificationsContainer.removeChild(el)
      }
    }, 300)
  }, timeout)
}

// Показ уведомления об ошибке
export function showError(message, timeout = 4000) {
  showNotification(message, 'error', timeout)
}

// Показ уведомления об успехе
export function showSuccess(message, timeout = 4000) {
  showNotification(message, 'success', timeout)
}

// Показ информационного уведомления
export function showInfo(message, timeout = 4000) {
  showNotification(message, 'info', timeout)
}