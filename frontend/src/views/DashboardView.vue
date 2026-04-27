<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useDocumentsStore } from '@/stores/documents'
import type { UploadState } from '@/types'

const store = useDocumentsStore()
const fileInput = ref<HTMLInputElement | null>(null)
const dragOver  = ref(false)
const uploadState = ref<UploadState>({ status: 'idle', progress: 0 })

onMounted(() => store.fetchAll())

function triggerPick() {
  fileInput.value?.click()
}

function onFilePick(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (files?.[0]) handleFile(files[0])
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) handleFile(file)
}

async function handleFile(file: File) {
  const allowed = ['application/pdf', 'text/plain']
  if (!allowed.includes(file.type)) {
    uploadState.value = { status: 'error', progress: 0, error: 'Only PDF and TXT files are supported.' }
    return
  }
  uploadState.value = { status: 'uploading', progress: 0 }
  try {
    await store.upload(file, (pct) => {
      uploadState.value.progress = pct
    })
    uploadState.value = { status: 'success', progress: 100 }
    setTimeout(() => { uploadState.value = { status: 'idle', progress: 0 } }, 3000)
  } catch {
    uploadState.value = { status: 'error', progress: 0, error: 'Upload failed. Please try again.' }
  }
}

async function remove(id: string) {
  if (confirm('Delete this document and all its indexed chunks?')) {
    await store.remove(id)
  }
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-CA', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">Document Library</h2>
      <p class="page-sub">{{ store.documents.length }} document{{ store.documents.length !== 1 ? 's' : '' }} indexed</p>
    </div>

    <!-- Upload zone -->
    <div
      class="upload-zone"
      :class="{ 'drag-over': dragOver, 'uploading': uploadState.status === 'uploading' }"
      @dragover.prevent="dragOver = true"
      @dragleave="dragOver = false"
      @drop.prevent="onDrop"
      @click="triggerPick"
    >
      <input ref="fileInput" type="file" accept=".pdf,.txt" hidden @change="onFilePick" />

      <template v-if="uploadState.status === 'idle'">
        <div class="upload-icon">↑</div>
        <p class="upload-label">Drop a PDF or TXT here, or <span class="link">browse</span></p>
        <p class="upload-hint">Max 20 MB</p>
      </template>

      <template v-else-if="uploadState.status === 'uploading'">
        <div class="progress-bar-wrap">
          <div class="progress-bar" :style="{ width: uploadState.progress + '%' }"></div>
        </div>
        <p class="upload-label">Uploading and indexing… {{ uploadState.progress }}%</p>
      </template>

      <template v-else-if="uploadState.status === 'success'">
        <div class="upload-icon success">✓</div>
        <p class="upload-label">Document indexed successfully</p>
      </template>

      <template v-else-if="uploadState.status === 'error'">
        <div class="upload-icon error">✕</div>
        <p class="upload-label error">{{ uploadState.error }}</p>
      </template>
    </div>

    <!-- Document list -->
    <div v-if="store.loading" class="state-message">Loading documents…</div>
    <div v-else-if="store.documents.length === 0" class="state-message muted">
      No documents yet. Upload your first PDF or text file above.
    </div>

    <div v-else class="doc-list">
      <div v-for="doc in store.documents" :key="doc.id" class="doc-row">
        <div class="doc-icon">{{ doc.contentType === 'application/pdf' ? '📄' : '📝' }}</div>
        <div class="doc-info">
          <span class="doc-name">{{ doc.filename }}</span>
          <span class="doc-meta">{{ formatBytes(doc.sizeBytes) }} · {{ formatDate(doc.createdAt) }}</span>
        </div>
        <button class="btn btn-danger btn-sm" @click="remove(doc.id)">Delete</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 1.5rem; }
.page-header { display: flex; align-items: baseline; gap: 1rem; }
.page-title { font-family: 'DM Serif Display', serif; font-size: 1.6rem; }
.page-sub { font-size: 0.85rem; color: var(--muted); }

.upload-zone {
  border: 2px dashed var(--border);
  border-radius: 12px;
  padding: 3rem 2rem;
  display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  user-select: none;
}
.upload-zone:hover, .upload-zone.drag-over {
  border-color: var(--accent);
  background: #5b8def0a;
}
.upload-icon { font-size: 2rem; line-height: 1; color: var(--muted); }
.upload-icon.success { color: var(--accent-2); }
.upload-icon.error   { color: var(--danger); }
.upload-label { font-size: 0.9rem; color: var(--muted); }
.upload-label.error  { color: var(--danger); }
.upload-hint  { font-size: 0.75rem; color: var(--muted); opacity: 0.6; }
.link { color: var(--accent); }

.progress-bar-wrap {
  width: 100%; max-width: 320px; height: 6px;
  background: var(--border); border-radius: 3px; overflow: hidden;
}
.progress-bar {
  height: 100%; background: var(--accent);
  border-radius: 3px; transition: width 0.2s;
}

.doc-list { display: flex; flex-direction: column; gap: 0.5rem; }
.doc-row {
  display: flex; align-items: center; gap: 1rem;
  padding: 0.875rem 1.25rem;
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: border-color 0.15s;
}
.doc-row:hover { border-color: var(--muted); }
.doc-icon { font-size: 1.25rem; flex-shrink: 0; }
.doc-info { flex: 1; display: flex; flex-direction: column; gap: 0.1rem; }
.doc-name { font-size: 0.9rem; font-weight: 500; }
.doc-meta { font-size: 0.75rem; color: var(--muted); font-family: 'DM Mono', monospace; }
.btn-sm { padding: 0.3rem 0.75rem; font-size: 0.75rem; }

.state-message { text-align: center; padding: 3rem; font-size: 0.875rem; color: var(--muted); }
</style>
