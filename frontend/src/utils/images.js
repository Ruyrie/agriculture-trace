export const parseImageUrls = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean).slice(0, 9)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter(Boolean).slice(0, 9) : []
  } catch {
    return String(value)
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
      .slice(0, 9)
  }
}

export const stringifyImageUrls = (value) => {
  const urls = parseImageUrls(value)
  return urls.length > 0 ? JSON.stringify(urls) : ''
}

export const resolveImageUrl = (url) => {
  if (!url || url.startsWith('http') || url.startsWith('data:')) return url
  return import.meta.env.DEV ? url : `${import.meta.env.VITE_API_BASE_URL}${url}`
}
