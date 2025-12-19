<template>
  <div class="point-form">
    <div class="form-group">
      <label>X:</label>
      <select v-model="x" class="form-select">
        <option value="">Выберите X</option>
        <option v-for="value in xValues" :key="value" :value="value">
          {{ value }}
        </option>
      </select>
    </div>
    
    <div class="form-group">
      <label>Y:</label>
      <input
        v-model="y"
        type="text"
        class="form-input"
        :class="{ 'invalid': yError }"
        placeholder="Введите значение от -5 до 3"
        @input="handleYInput"
      />
      <div v-if="yError" class="error-message">
        {{ yError }}
      </div>
    </div>
    
    <div class="form-group">
      <label>R:</label>
      <select v-model="r" class="form-select" @change="onRChange">
        <option value="">Выберите R</option>
        <option v-for="value in rValues" :key="value" :value="value">
          {{ value }}
        </option>
      </select>
    </div>
    
    <button @click="checkPoint" class="check-button" :disabled="checking">
      {{ checking ? 'Проверка...' : 'Проверить' }}
    </button>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { usePointsStore } from '../stores/pointsStore'
import { showError } from '../utils/notifications'

const emit = defineEmits(['point-checked'])

const pointsStore = usePointsStore()

// Значения для выбора
const xValues = ['-4', '-3', '-2', '-1', '0', '1', '2', '3', '4']
const rValues = ['-4', '-3', '-2', '-1', '0', '1', '2', '3', '4']

// Реактивные данные формы
const x = ref('')
const y = ref('')
const r = ref('')
const yError = ref('')
const checking = ref(false)

// Обработка ввода Y с валидацией
const handleYInput = (event) => {
  let inputValue = event.target.value
  
  // Запрещаем недопустимые символы
  // Разрешаем: цифры, точку, минус только в начале
  let filteredValue = ''
  let hasMinus = false
  let hasDot = false
  
  for (let i = 0; i < inputValue.length; i++) {
    const char = inputValue[i]
    
    // Минус только в начале и только один раз
    if (char === '-') {
      if (i === 0 && !hasMinus) {
        filteredValue += char
        hasMinus = true
      }
    }
    // Точка только одна и не в начале
    else if (char === '.' || char === ',') {
      if (!hasDot && filteredValue.length > 0) {
        filteredValue += '.'
        hasDot = true
      }
    }
    // Цифры
    else if (char >= '0' && char <= '9') {
      filteredValue += char
    }
    // Остальные символы игнорируем
  }
  
  // Устанавливаем отфильтрованное значение
  y.value = filteredValue
  
  // Валидируем
  validateY()
}

// Валидация значения Y
const validateY = () => {
  if (y.value === '' || y.value === '-') {
    yError.value = ''
    return
  }
  
  const value = parseFloat(y.value)
  
  if (isNaN(value)) {
    yError.value = 'Введите числовое значение'
    return
  }
  
  if (value < -5 || value > 3) {
    yError.value = 'Значение должно быть в диапазоне от -5 до 3'
    return
  }
  
  yError.value = ''
}

// Обработчик изменения R
const onRChange = () => {
  if (r.value === '') {
    // Эмитируем null для сброса графика
    emit('r-changed', null)
    return
  }
  
  const rValue = parseFloat(r.value)
  if (rValue < 0) {
    // Сбрасываем на "Выберите R" и эмитируем null для очистки графика
    r.value = ''
    emit('r-changed', null)
    showError('Выбрано отрицательное значение R. Значение сброшено.')
    return
  }
  
  emit('r-changed', rValue)
}

// Проверка точки
const checkPoint = async () => {
  // Валидация
  validateY()
  
  // Проверяем, что выбраны все значения
  if (x.value === '') {
    showError('Выберите значение X')
    return
  }
  
  if (y.value === '') {
    showError('Введите значение Y')
    return
  }
  
  if (yError.value) {
    showError(yError.value)
    return
  }
  
  if (r.value === '') {
    showError('Выберите значение R')
    return
  }
  
  const rValue = parseFloat(r.value)
  if (rValue < 0) {
    showError('Нельзя выбрать отрицательное значение R')
    return
  }
  
  const yValue = parseFloat(y.value)
  
  checking.value = true
  
  try {
    // Проверяем точку через хранилище
    const point = {
      x: parseFloat(x.value),
      y: yValue,
      r: rValue
    }
    
    const result = await pointsStore.checkPointFromForm(point)
    
    // Эмитируем событие с новыми точками
    emit('point-checked', result)
  } catch (error) {
    showError(error.message || 'Ошибка при проверке точки')
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.point-form {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  border-radius: 20px;
  padding: 1rem;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: fit-content;
}

.form-group {
  margin-bottom: 1rem;
  width: 100%;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
  font-size: 0.95rem;
}

.form-select,
.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  background: white;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
}

.form-input.invalid {
  border-color: #e74c3c;
  border-width: 2px;
}

.form-select:focus,
.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.check-button {
  width: 100%;
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

.check-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.check-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.error-message {
  color: #e74c3c;
  font-size: 0.875rem;
  margin-top: 0.25rem;
  min-height: 1.2em;
}

/* Десктоп: > 1038px */
@media (min-width: 1038px) {
  .point-form {
    padding: 1.5rem;
    min-height: 350px;
  }
  
  .form-group {
    margin-bottom: 1.25rem;
  }
}

/* Планшетный: 703px - 1037px */
@media (min-width: 703px) and (max-width: 1037px) {
  .point-form {
    padding: 1rem;
    min-height: 400px;
  }
  
  .form-group {
    margin-bottom: 1rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.65rem;
    font-size: 0.95rem;
  }
  
  .check-button {
    padding: 10px;
    font-size: 0.95rem;
  }
}

/* Мобильный: < 703px */
@media (max-width: 702px) {
  .point-form {
    padding: 1rem;
    min-height: auto;
  }
  
  .form-group {
    margin-bottom: 0.75rem;
  }
  
  .form-group label {
    font-size: 0.9rem;
    margin-bottom: 0.4rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.6rem;
    font-size: 0.9rem;
  }
  
  .check-button {
    padding: 10px;
    font-size: 0.95rem;
  }
}

/* Очень маленькие экраны */
@media (max-width: 480px) {
  .point-form {
    padding: 0.75rem;
  }
  
  .form-group {
    margin-bottom: 0.6rem;
  }
  
  .form-group label {
    font-size: 0.85rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.5rem;
    font-size: 0.85rem;
  }
  
  .check-button {
    padding: 8px;
    font-size: 0.9rem;
  }
}

/* Ландшафтная ориентация на мобильных */
@media (max-width: 702px) and (orientation: landscape) {
  .point-form {
    padding: 0.75rem;
  }
  
  .form-group {
    margin-bottom: 0.5rem;
  }
  
  .form-group label {
    margin-bottom: 0.3rem;
    font-size: 0.8rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.4rem;
    font-size: 0.8rem;
  }
  
  .check-button {
    padding: 6px;
    font-size: 0.8rem;
    margin-top: 0.25rem;
  }
}

/* Адаптивные стили по высоте */
@media (max-height: 700px) {
  .point-form {
    padding: 0.75rem;
  }
  
  .form-group {
    margin-bottom: 0.6rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.6rem;
  }
  
  .check-button {
    padding: 8px;
  }
}

@media (max-height: 600px) {
  .point-form {
    padding: 0.5rem;
  }
  
  .form-group {
    margin-bottom: 0.4rem;
  }
  
  .form-group label {
    margin-bottom: 0.2rem;
    font-size: 0.8rem;
  }
  
  .form-select,
  .form-input {
    padding: 0.4rem;
    font-size: 0.8rem;
  }
  
  .check-button {
    padding: 6px;
    font-size: 0.8rem;
  }
}
</style>