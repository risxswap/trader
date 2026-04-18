import { http } from './http';
export const listCorrelations = async (params) => {
    const res = await http.get('/correlation', { params });
    return res.data;
};
export const getCorrelationDetail = async (id) => {
    const res = await http.get(`/correlation/${id}`);
    return res.data;
};
//# sourceMappingURL=correlation.js.map