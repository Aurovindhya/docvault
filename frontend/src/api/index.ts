import axios from 'axios'
import type { AuthResponse, Document, SearchResult } from '@/types'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT on every request
api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// Redirect to login on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// --- Auth ---
export const authApi = {
  register: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/register', { email, password }),

  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }),
}

// --- Documents ---
export const documentsApi = {
  list: () => api.get<Document[]>('/documents'),

  upload: (file: File, onProgress?: (pct: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<Document>('/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      },
    })
  },

  delete: (id: string) => api.delete(`/documents/${id}`),
}

// --- Search ---
export const searchApi = {
  search: (q: string, limit = 10) =>
    api.get<SearchResult[]>('/search', { params: { q, limit } }),

  semanticSearch: (q: string, limit = 10) =>
    api.get<SearchResult[]>('/search/semantic', { params: { q, limit } }),
}
