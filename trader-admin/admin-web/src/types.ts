export interface ResData<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface LoginDto {
  token: string
  username: string
  nickname: string
}

export interface LoginParam {
  username: string
  password: string
}

export interface PageRes<T> {
  items: T[]
  total: number
  pageNo: number
  pageSize: number
}
