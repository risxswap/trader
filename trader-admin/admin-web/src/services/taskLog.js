import { http } from './http';
export const listTaskLogs = async (query) => {
    const res = await http.get('/logs/task', { params: query });
    return res.data;
};
//# sourceMappingURL=taskLog.js.map