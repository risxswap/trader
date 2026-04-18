import { http } from './http'
import type { ResData, LoginDto, LoginParam } from '../types'

export const login = async (payload: LoginParam): Promise<ResData<LoginDto>> => {
  const res = await http.post('/auth/login', payload)
  return res.data as ResData<LoginDto>
}

export const changePassword = async (payload: { username: string; oldPassword: string; newPassword: string }): Promise<ResData<void>> => {
  const res = await http.post('/user/change-password', payload)
  return res.data as ResData<void>
}
