import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('dv_token'))
  const email = ref<string | null>(localStorage.getItem('dv_email'))

  const isAuthenticated = computed(() => !!token.value)

  async function register(emailVal: string, password: string): Promise<void> {
    const res = await authApi.register(emailVal, password)
    _setSession(res.data.token, emailVal)
  }

  async function login(emailVal: string, password: string): Promise<void> {
    const res = await authApi.login(emailVal, password)
    _setSession(res.data.token, emailVal)
  }

  function logout(): void {
    token.value = null
    email.value = null
    localStorage.removeItem('dv_token')
    localStorage.removeItem('dv_email')
  }

  function _setSession(tok: string, em: string): void {
    token.value = tok
    email.value = em
    localStorage.setItem('dv_token', tok)
    localStorage.setItem('dv_email', em)
  }

  return { token, email, isAuthenticated, register, login, logout }
})
