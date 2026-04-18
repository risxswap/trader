import request from '../utils/request'

export function getNodeList() {
  return request({
    url: '/api/node/list',
    method: 'get'
  })
}

export function getNodeHistory(nodeId, startTime, endTime) {
  return request({
    url: `/api/node/${nodeId}/history`,
    method: 'get',
    params: {
      startTime,
      endTime
    }
  })
}
