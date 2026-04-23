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
