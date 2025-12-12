// Утилита для определения базового URL API
export const getApiBaseUrl = () => {
  // В режиме разработки используем прокси
  if (import.meta.env.DEV) {
    return '/api';
  }
  
  // В production определяем относительно текущего пути
  const pathParts = window.location.pathname.split('/').filter(part => part.length > 0);
  if (pathParts.length > 0) {
    // Предполагаем, что первый сегмент пути - это контекст приложения
    return `/${pathParts[0]}/api`;
  }
  
  // По умолчанию
  return '/api';
};

export const API_BASE_URL = getApiBaseUrl();