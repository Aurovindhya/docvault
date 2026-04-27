// All shared TypeScript interfaces for DocVault

export interface User {
  email: string
}

export interface AuthResponse {
  token: string
}

export interface Document {
  id: string
  filename: string
  contentType: string
  sizeBytes: number
  createdAt: string
}

export interface SearchResult {
  documentId: string
  filename: string
  excerpt: string
  score: number
  matchType: 'semantic' | 'fulltext'
}

export interface UploadState {
  progress: number
  status: 'idle' | 'uploading' | 'success' | 'error'
  error?: string
}

export type SearchMode = 'hybrid' | 'semantic'
