import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Document } from '@/types'
import { documentsApi } from '@/api'

export const useDocumentsStore = defineStore('documents', () => {
  const documents = ref<Document[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAll(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await documentsApi.list()
      documents.value = res.data
    } catch (e: any) {
      error.value = e.response?.data?.message ?? 'Failed to load documents'
    } finally {
      loading.value = false
    }
  }

  async function upload(file: File, onProgress?: (pct: number) => void): Promise<Document> {
    const res = await documentsApi.upload(file, onProgress)
    documents.value.unshift(res.data)
    return res.data
  }

  async function remove(id: string): Promise<void> {
    await documentsApi.delete(id)
    documents.value = documents.value.filter((d) => d.id !== id)
  }

  return { documents, loading, error, fetchAll, upload, remove }
})
