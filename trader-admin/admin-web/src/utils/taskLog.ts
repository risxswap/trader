export const formatExecutionDuration = (executionMs?: number | null) => {
  if (executionMs === undefined || executionMs === null || executionMs < 0) return '-'
  if (executionMs < 1000) return `${executionMs} ms`

  const totalSeconds = Math.floor(executionMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0) {
    return `${hours}时${minutes}分${seconds}秒`
  }

  return `${minutes}分${seconds}秒`
}

export type ParsedTaskLogContent = {
  syncedCount?: number
  failedCount?: number
  message?: string
  errorDetail?: any
}

export const parseTaskLogContent = (content?: string | null): ParsedTaskLogContent => {
  if (!content) return {}
  try {
    const obj = JSON.parse(content)
    if (!obj || typeof obj !== 'object') return {}
    return {
      syncedCount: typeof obj.syncedCount === 'number' ? obj.syncedCount : undefined,
      failedCount: typeof obj.failedCount === 'number' ? obj.failedCount : undefined,
      message: typeof obj.message === 'string' ? obj.message : undefined,
      errorDetail: obj.errorDetail
    }
  } catch {
    return {}
  }
}
