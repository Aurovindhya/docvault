<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header v-if="auth.isAuthenticated" class="topbar">
      <div class="topbar-left">
        <span class="logo">DocVault</span>
        <nav>
          <RouterLink to="/dashboard">Library</RouterLink>
          <RouterLink to="/search">Search</RouterLink>
        </nav>
      </div>
      <div class="topbar-right">
        <span class="user-email">{{ auth.email }}</span>
        <button class="btn-ghost" @click="logout">Sign out</button>
      </div>
    </header>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>
