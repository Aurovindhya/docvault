<script setup lang="ts">
import { ref } from 'vue'
import { searchApi } from '@/api'
import type { SearchResult, SearchMode } from '@/types'

const query   = ref('')
const mode    = ref<SearchMode>('hybrid')
const results = ref<SearchResult[]>([])
const loading = ref(false)
const error   = ref('')
const searched = ref(false)

async function search() {
  if (!query.value.trim()) return
  loading.value = true
  error.value   = ''
  searched.value = false

  try {
    const fn = mode.value === 'semantic' ? searchApi.semanticSearch : searchApi.search
    const res = await fn(query.value.trim())
    results.value  = res.data
    searched.value = true
  } catch (e: any) {
    error.value = 'Search failed. Please try again.'
  } finally {
    loading.value = false
  }
}

function scoreColor(score: number): string {
  if (score >= 0.8) return 'var(--accent-2)'
  if (score >= 0.5) return 'var(--accent)'
  return 'var(--muted)'
}
</script>

<template>
  <div class="search-page">
    <div class="search-header">
      <h2 class="page-title">Semantic Search</h2>
      <p class="page-sub">Ask a question or describe a topic — find relevant content across all your documents.</p>
    </div>

    <!-- Search bar -->
    <form class="search-bar" @submit.prevent="search">
      <input
        v-model="query"
        type="text"
        class="search-input"
        placeholder="e.g. What are the key financial risks mentioned?"
        autocomplete="off"
      />
      <div class="mode-toggle">
        <button
          type="button"
          class="mode-btn"
          :class="{ active: mode === 'hybrid' }"
          @click="mode = 'hybrid'"
        >Hybrid</button>
        <button
          type="button"
          class="mode-btn"
          :class="{ active: mode === 'semantic' }"
          @click="mode = 'semantic'"
        >Semantic only</button>
      </div>
      <button class="btn btn-primary" type="submit" :disabled="loading || !query.trim()">
        {{ loading ? 'Searching…' : 'Search' }}
      </button>
    </form>

    <p v-if="error" class="error-msg">{{ error }}</p>

    <!-- No results -->
    <div v-if="searched && results.length === 0" class="state-message">
      No results found for "{{ query }}". Try different keywords.
    </div>

    <!-- Results -->
    <div v-if="results.length" class="results">
      <p class="results-count">{{ results.length }} result{{ results.length !== 1 ? 's' : '' }}</p>

      <div v-for="r in results" :key="r.documentId" class="result-card">
        <div class="result-top">
          <span class="result-filename">{{ r.filename }}</span>
          <span class="result-badge" :style="{ color: scoreColor(r.score) }">
            {{ (r.score * 100).toFixed(0) }}% · {{ r.matchType }}
          </span>
        </div>
        <p class="result-excerpt">{{ r.excerpt }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-page { display: flex; flex-direction: column; gap: 1.5rem; }
.search-header { margin-bottom: 0.5rem; }
.page-title { font-family: 'DM Serif Display', serif; font-size: 1.6rem; }
.page-sub { font-size: 0.875rem; color: var(--muted); margin-top: 0.25rem; }

.search-bar {
  display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;
}
.search-input {
  flex: 1; min-width: 260px;
  background: var(--surface); border: 1px solid var(--border);
  color: var(--text); border-radius: var(--radius);
  padding: 0.65rem 1rem; font-size: 0.9rem;
  font-family: 'DM Sans', sans-serif;
  outline: none; transition: border-color 0.15s;
}
.search-input:focus { border-color: var(--accent); }

.mode-toggle { display: flex; border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
.mode-btn {
  background: transparent; color: var(--muted); border: none;
  padding: 0.5rem 0.875rem; font-size: 0.8rem; font-family: 'DM Sans', sans-serif;
  cursor: pointer; transition: background 0.15s, color 0.15s;
}
.mode-btn.active { background: var(--accent); color: #fff; }

.results { display: flex; flex-direction: column; gap: 0.75rem; }
.results-count { font-size: 0.8rem; color: var(--muted); }

.result-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 1.25rem 1.5rem;
  display: flex; flex-direction: column; gap: 0.75rem;
  transition: border-color 0.15s;
}
.result-card:hover { border-color: var(--muted); }
.result-top { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
.result-filename { font-weight: 500; font-size: 0.9rem; }
.result-badge { font-size: 0.75rem; font-family: 'DM Mono', monospace; white-space: nowrap; }
.result-excerpt { font-size: 0.875rem; color: var(--muted); line-height: 1.65; }

.state-message { text-align: center; padding: 3rem; font-size: 0.875rem; color: var(--muted); }
</style>
