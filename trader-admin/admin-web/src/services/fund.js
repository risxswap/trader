import { http } from './http';
export const listFunds = async (payload) => {
    const res = await http.post('/fund/list', payload);
    return res.data;
};
export const getFundDetail = async (code) => {
    const res = await http.get(`/fund/detail/${code}`);
    return res.data;
};
export const updateFund = async (code, payload) => {
    const res = await http.put(`/fund/update/${code}`, payload);
    return res.data;
};
export const deleteFund = async (code) => {
    const res = await http.delete(`/fund/delete/${code}`);
    return res.data;
};
export const getFundMarket = async (payload) => {
    const res = await http.post('/fund/market', payload);
    return res.data;
};
export const getFundAdj = async (payload) => {
    const res = await http.post('/fund/adj', payload);
    return res.data;
};
export const getDefaultFundCode = async () => {
    const res = await http.get('/fund/default-code');
    return res.data;
};
export const listFundMarkets = async (payload) => {
    const res = await http.post('/fund/market/list', payload);
    return res.data;
};
export const listFundAdjs = async (payload) => {
    const res = await http.post('/fund/adj/list', payload);
    return res.data;
};
export const listFundNavs = async (payload) => {
    const res = await http.post('/fund-nav/list', payload);
    return res.data;
};
//# sourceMappingURL=fund.js.map