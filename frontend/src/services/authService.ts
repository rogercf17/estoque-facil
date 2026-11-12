import type { LoginRequest, LoginResponse } from '../models/auth'
import { api } from './api'

// Contrato: POST /api/v1/auth/login (implementado na Sprint 1).
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/auth/login', request)
  return data
}
