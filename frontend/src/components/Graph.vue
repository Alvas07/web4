<template>
  <div class="graph-container">
    <canvas 
      ref="canvas" 
      class="graph-canvas"
      @click="handleCanvasClick"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    ></canvas>
    <div 
      v-if="tooltip.show && tooltip.x !== null && tooltip.y !== null"
      class="tooltip"
      :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
    >
      <div class="tooltip-content">
        <div><strong>X:</strong> {{ formatNumber(tooltip.point?.x) }}</div>
        <div><strong>Y:</strong> {{ formatNumber(tooltip.point?.y) }}</div>
        <div><strong>R:</strong> {{ formatNumber(tooltip.point?.r) }}</div>
        <div><strong>Владелец:</strong> {{ tooltip.point?.username }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { usePointsStore } from '../stores/pointsStore'
import { useAuthStore } from '../stores/authStore'
import { showError } from '../utils/notifications'

const emit = defineEmits(['point-clicked'])

// Tooltip для отображения информации о точке
const tooltip = ref({
  show: false,
  x: null,
  y: null,
  point: null
})

// Форматирование числа
const formatNumber = (value) => {
  if (value === null || value === undefined) return ''
  const num = typeof value === 'number' ? value : parseFloat(value)
  if (isNaN(num)) return value
  return num.toFixed(3).replace(/\.?0+$/, '')
}

const props = defineProps({
  r: {
    type: [Number, null],
    default: null
  }
})

const pointsStore = usePointsStore()
const authStore = useAuthStore()

const canvas = ref(null)
const ctx = ref(null)

// Обработчик изменения размера окна
const handleResize = () => {
  // Небольшая задержка для уверенности, что размеры обновились
  setTimeout(drawCanvas, 100)
}

// Параметры графика
const SCALE_MIN = -6
const SCALE_MAX = 6
const LABEL_MIN = -5
const LABEL_MAX = 5

// Вычисляемые точки для отображения - используем точки напрямую из store
const displayPoints = computed(() => {
  // Если R не выбран, не показываем точки на графике (только в таблице)
  if (props.r === null || props.r === undefined) {
    return []
  }
  
  // Берем напрямую из points.value - это основной массив с точками
  const allPoints = Array.isArray(pointsStore.points) ? pointsStore.points : []
  console.log('displayPoints computed - allPoints from pointsStore.points:', allPoints, 'length:', allPoints.length, 'props.r:', props.r)
  
  // Если R выбран и не равен нулю, показываем только точки с этим R
  if (props.r !== 0) {
    const filtered = allPoints.filter(p => {
      // Обрабатываем обе структуры данных
      const pointR = p.r !== undefined ? p.r : (p.point && p.point.r)
      return pointR === props.r
    })
    console.log('displayPoints filtered by R:', filtered, 'length:', filtered.length)
    return filtered
  }
  
  // Если R = 0, показываем все точки
  return allPoints
})

// Наблюдаем за изменением точек
watch(displayPoints, () => {
  drawCanvas()
}, { deep: true, immediate: false })

// Наблюдаем за изменением точек в store напрямую
watch(() => pointsStore.points, (newPoints, oldPoints) => {
  console.log('pointsStore.points changed:', newPoints, 'old:', oldPoints)
  console.log('pointsStore.points length:', newPoints?.length)
  // Небольшая задержка чтобы Vue успел обновить все computed
  setTimeout(() => {
    drawCanvas()
  }, 10)
}, { deep: true, immediate: true })

// Наблюдаем за изменением R
watch(() => props.r, () => {
  // Перерисовываем при изменении R
  setTimeout(() => {
    drawCanvas()
  }, 50)
}, { immediate: false })

// Получение размера canvas (учитывая pixel ratio)
const getCanvasSize = () => {
  if (!canvas.value) return 0
  // Используем реальный размер canvas (уже с учетом pixel ratio)
  // Но для расчетов нужно делить на pixel ratio
  const dpr = window.devicePixelRatio || 1
  return canvas.value.width / dpr
}

// Преобразование координат
const scaleX = (x) => {
  if (!canvas.value) return 0
  const size = getCanvasSize()
  const range = SCALE_MAX - SCALE_MIN
  return size * ((x - SCALE_MIN) / range)
}

const scaleY = (y) => {
  if (!canvas.value) return 0
  const size = getCanvasSize()
  const range = SCALE_MAX - SCALE_MIN
  return size * (1 - ((y - SCALE_MIN) / range))
}

const inverseScaleX = (pageX) => {
  if (!canvas.value) return 0
  const rect = canvas.value.getBoundingClientRect()
  const px = pageX - rect.left
  const size = getCanvasSize()
  const range = SCALE_MAX - SCALE_MIN
  return SCALE_MIN + (px / size) * range
}

const inverseScaleY = (pageY) => {
  if (!canvas.value) return 0
  const rect = canvas.value.getBoundingClientRect()
  const py = pageY - rect.top
  const size = getCanvasSize()
  const range = SCALE_MAX - SCALE_MIN
  return SCALE_MIN + (1 - (py / size)) * range
}

// Отрисовка осей
const drawAxes = () => {
  if (!ctx.value || !canvas.value) return
  
  const size = getCanvasSize()
  
  // Очищаем canvas
  ctx.value.clearRect(0, 0, canvas.value.width, canvas.value.height)
  
  // Рисуем оси
  ctx.value.strokeStyle = '#000'
  ctx.value.lineWidth = 1
  
  // Горизонтальная ось (Y = 0)
  ctx.value.beginPath()
  ctx.value.moveTo(0, scaleY(0))
  ctx.value.lineTo(size, scaleY(0))
  ctx.value.stroke()
  
  // Вертикальная ось (X = 0)
  ctx.value.beginPath()
  ctx.value.moveTo(scaleX(0), 0)
  ctx.value.lineTo(scaleX(0), size)
  ctx.value.stroke()
  
  // Рисуем метки
  ctx.value.fillStyle = '#000'
  ctx.value.font = '12px Arial'
  ctx.value.textAlign = 'center'
  ctx.value.textBaseline = 'top'
  
  // Метки по оси X
  for (let i = LABEL_MIN; i <= LABEL_MAX; i++) {
    if (i === 0) continue
    const x = scaleX(i)
    ctx.value.fillText(i.toString(), x, scaleY(0) + 5)
  }
  
  // Метки по оси Y
  ctx.value.textAlign = 'right'
  ctx.value.textBaseline = 'middle'
  for (let i = LABEL_MIN; i <= LABEL_MAX; i++) {
    if (i === 0) continue
    const y = scaleY(i)
    ctx.value.fillText(i.toString(), scaleX(0) - 5, y)
  }
}

// Отрисовка области
const drawArea = (rValue) => {
  if (!ctx.value || rValue === 0 || !rValue) return
  
  ctx.value.fillStyle = 'rgba(0, 128, 255, 0.3)'
  
  // 2-й квадрант: квадрат со стороной R
  const x1 = scaleX(-rValue)
  const y1 = scaleY(rValue)
  const width = scaleX(0) - scaleX(-rValue)
  const height = scaleY(0) - scaleY(rValue)
  ctx.value.fillRect(x1, y1, width, height)
  
  // 3-й квадрант: треугольник с катетами R и R
  ctx.value.beginPath()
  ctx.value.moveTo(scaleX(0), scaleY(0))
  ctx.value.lineTo(scaleX(-rValue), scaleY(0))
  ctx.value.lineTo(scaleX(0), scaleY(-rValue))
  ctx.value.closePath()
  ctx.value.fill()
  
  // 4-й квадрант: четверть круга с радиусом R
  ctx.value.beginPath()
  ctx.value.moveTo(scaleX(0), scaleY(0))
  const steps = 36
  for (let i = 0; i <= steps; i++) {
    const t = 0 + (i / steps) * (-Math.PI / 2)
    const px = rValue * Math.cos(t)
    const py = rValue * Math.sin(t)
    ctx.value.lineTo(scaleX(px), scaleY(py))
  }
  ctx.value.closePath()
  ctx.value.fill()
}

// Храним координаты точек для определения hover
const pointCoords = ref([])

// Отрисовка точек
const drawPoints = () => {
  if (!ctx.value) return
  
  console.log('drawPoints called with displayPoints:', displayPoints.value)
  
  // Очищаем массив координат
  pointCoords.value = []
  
  displayPoints.value.forEach(point => {
    // Обрабатываем обе структуры данных
    const px = point.x !== undefined ? point.x : (point.point && point.point.x)
    const py = point.y !== undefined ? point.y : (point.point && point.point.y)
    const pr = point.r !== undefined ? point.r : (point.point && point.point.r)
    const hit = point.hit !== undefined ? point.hit : false
    const username = point.username
    
    console.log('Drawing point:', { px, py, hit, username, point })
    
    const x = scaleX(px)
    const y = scaleY(py)
    
    // Сохраняем координаты точки для определения hover
    pointCoords.value.push({
      x: x,
      y: y,
      point: {
        x: px,
        y: py,
        r: pr,
        username: username,
        hit: hit
      },
      radius: 5
    })
    
    // Рисуем точку
    ctx.value.beginPath()
    
    // Цвет зависит от результата и владельца
    let fillColor, strokeColor
    if (hit) {
      fillColor = username === authStore.user?.username ? '#2ecc71' : 'rgba(46, 204, 113, 0.5)'
      strokeColor = '#000'
    } else {
      fillColor = username === authStore.user?.username ? '#e74c3c' : 'rgba(231, 76, 60, 0.5)'
      strokeColor = '#000'
    }
    
    // Рисуем квадратик для чужих точек, кружок для своих
    if (username === authStore.user?.username) {
      // Кружок для своих точек
      ctx.value.arc(x, y, 5, 0, 2 * Math.PI)
    } else {
      // Квадратик для чужих точек
      const size = 5
      ctx.value.rect(x - size, y - size, size * 2, size * 2)
    }
    
    ctx.value.fillStyle = fillColor
    ctx.value.fill()
    ctx.value.strokeStyle = strokeColor
    ctx.value.lineWidth = 1
    ctx.value.stroke()
  })
}

// Обработка движения мыши для tooltip
const handleMouseMove = (event) => {
  if (!canvas.value || !ctx.value) return
  
  const rect = canvas.value.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  const size = Math.min(rect.width, rect.height)
  
  // Получаем координаты мыши относительно canvas (в пикселях canvas)
  const mouseX = (event.clientX - rect.left) * (canvas.value.width / size) / dpr
  const mouseY = (event.clientY - rect.top) * (canvas.value.height / size) / dpr
  
  // Проверяем, находится ли курсор над точкой
  const hoveredPoint = pointCoords.value.find(coord => {
    const dx = mouseX - coord.x
    const dy = mouseY - coord.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    return distance <= coord.radius + 5 // Небольшой отступ для удобства
  })
  
  if (hoveredPoint) {
    tooltip.value = {
      show: true,
      x: event.clientX + 10,
      y: event.clientY - 10,
      point: hoveredPoint.point
    }
  } else {
    tooltip.value.show = false
  }
}

// Скрытие tooltip при уходе мыши с canvas
const handleMouseLeave = () => {
  tooltip.value.show = false
}

// Основная функция отрисовки
const drawCanvas = () => {
  if (!ctx.value || !canvas.value || !canvas.value.parentElement) return
  
  // Устанавливаем размер canvas
  const dpr = window.devicePixelRatio || 1
  const container = canvas.value.parentElement
  const containerRect = container.getBoundingClientRect()
  
  // Проверяем что размеры есть
  if (containerRect.width <= 0 || containerRect.height <= 0) return
  
  // Получаем computed стили контейнера для точного учета padding
  const containerStyles = window.getComputedStyle(container)
  const paddingLeft = parseFloat(containerStyles.paddingLeft) || 0
  const paddingRight = parseFloat(containerStyles.paddingRight) || 0
  const paddingTop = parseFloat(containerStyles.paddingTop) || 0
  const paddingBottom = parseFloat(containerStyles.paddingBottom) || 0
  
  // Вычисляем доступное пространство с учетом padding
  const availableWidth = containerRect.width - paddingLeft - paddingRight
  const availableHeight = containerRect.height - paddingTop - paddingBottom
  
  // Делаем canvas квадратным - используем минимальное значение для обеих сторон
  const size = Math.min(availableWidth, availableHeight)
  
  // Устанавливаем CSS размеры для квадрата
  canvas.value.style.width = size + 'px'
  canvas.value.style.height = size + 'px'
  
  // Устанавливаем реальные размеры с учетом pixel ratio
  canvas.value.width = size * dpr
  canvas.value.height = size * dpr
  
  ctx.value.setTransform(dpr, 0, 0, dpr, 0, 0)
  
  // Отрисовываем элементы
  drawAxes()
  // Рисуем область только если R выбран
  if (props.r !== null && props.r !== undefined) {
    drawArea(props.r)
  }
  // Рисуем точки всегда
  drawPoints()
}

// Обработчик клика по canvas
const handleCanvasClick = async (event) => {
  // Проверяем, что R выбран
  if (props.r === null || props.r === undefined) {
    showError('Сначала выберите значение R')
    return
  }
  
  const x = inverseScaleX(event.clientX)
  const y = inverseScaleY(event.clientY)
  
  // Проверяем, что координаты в допустимом диапазоне
  if (x < SCALE_MIN || x > SCALE_MAX || y < SCALE_MIN || y > SCALE_MAX) {
    showError('Координаты вне допустимого диапазона (-6..6)')
    return
  }
  
  try {
    // Проверяем точку через график
    const point = {
      x: x,
      y: y,
      r: props.r
    }
    
    const result = await pointsStore.checkPointFromGraph(point)
    // Принудительно перерисовываем после добавления точки
    setTimeout(() => {
      drawCanvas()
    }, 50)
    
  } catch (error) {
    showError(error.message || 'Ошибка при проверке точки')
  }
}

// Инициализация canvas
onMounted(() => {
  if (canvas.value) {
    ctx.value = canvas.value.getContext('2d')
    // Небольшая задержка для уверенности, что размеры контейнера готовы
    setTimeout(() => {
      drawCanvas()
    }, 100)
    
    // Перерисовываем при изменении размера окна
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// Метод для установки точек
const setPoints = (newPoints) => {
  pointsStore.points = newPoints
}

// Экспортируем методы для родительского компонента
defineExpose({
  setPoints,
  drawCanvas
})
</script>

<style scoped>
.graph-container {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: 1rem;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  height: 100%;
  min-height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
  aspect-ratio: 1 / 1;
}

.empty-graph {
  color: #909399;
  font-size: 1rem;
  text-align: center;
  padding: 2rem;
}

.graph-canvas {
  width: 100%;
  height: 100%;
  aspect-ratio: 1 / 1;
  max-width: calc(100% - 2px);
  max-height: calc(100% - 2px);
  border-radius: 12px;
  cursor: crosshair;
  display: block;
  object-fit: contain;
  flex-shrink: 0;
}

.tooltip {
  position: fixed;
  z-index: 1000;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  color: #1a1a1a;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 0.875rem;
  box-shadow: 0 4px 20px rgba(0, 122, 255, 0.2);
  border: 1px solid rgba(0, 122, 255, 0.2);
  white-space: nowrap;
}

.tooltip-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tooltip-content div {
  display: flex;
  gap: 6px;
}

.tooltip-content strong {
  font-weight: 600;
  color: #007aff;
}

/* Планшетный: 703px - 1037px */
@media (min-width: 703px) and (max-width: 1037px) {
  .graph-container {
    padding: 1rem;
    min-height: 400px;
    aspect-ratio: 1 / 1;
  }
}

/* Мобильный: < 703px */
@media (max-width: 702px) {
  .graph-container {
    padding: 0.75rem;
    min-height: 350px;
    aspect-ratio: 1 / 1;
  }
}

/* Адаптивные стили по высоте */
@media (max-height: 900px) {
  .graph-container {
    padding: 0.75rem;
  }
}

@media (max-height: 700px) {
  .graph-container {
    padding: 0.5rem;
  }
}

@media (max-height: 600px) {
  .graph-container {
    padding: 0.5rem;
  }
}
</style>