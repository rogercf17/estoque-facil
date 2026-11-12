export type Role = 'MANAGER' | 'OPERATOR' | 'VIEWER'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  role: Role
}
