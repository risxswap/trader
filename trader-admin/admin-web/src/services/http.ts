import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const http = axios.create({
  baseURL,
  timeout: 15000
})

http.interceptors.response.use(
  (res) => res,
  (err) => Promise.reject(err)
)
