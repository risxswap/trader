import { http } from './http';
export const listMsgPushLogs = async (query) => {
    const res = await http.get('/msg-push-log', { params: query });
    return res.data;
};
export const getMsgPushLogDetail = async (id) => {
    const res = await http.get(`/msg-push-log/${id}`);
    return res.data;
};
//# sourceMappingURL=log.js.map
