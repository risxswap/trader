import { http } from './http';
export const listStrategies = async () => {
    const res = await http.get('/basic/strategies');
    return res.data;
};
export const listExchanges = async (payload) => {
    const res = await http.get('/exchange/list', { params: payload });
    return res.data;
};
export const listTradings = async (payload) => {
    const res = await http.get('/investment-trading/list', { params: payload });
    return res.data;
};
export const listPositions = async (payload) => {
    const res = await http.get('/investment-position/list', { params: payload });
    return res.data;
};
export const addPosition = async (payload) => {
    const res = await http.post('/investment-position/add', payload);
    return res.data;
};
export const updatePosition = async (payload) => {
    const res = await http.post('/investment-position/update', payload);
    return res.data;
};
export const deletePosition = async (id) => {
    const res = await http.post('/investment-position/delete', null, { params: { id } });
    return res.data;
};
export const listInvestments = async (payload) => {
    const res = await http.get('/investments', { params: payload });
    return res.data;
};
export const addInvestment = async (payload) => {
    const res = await http.post('/investments', payload);
    return res.data;
};
export const updateInvestment = async (payload) => {
    const res = await http.put('/investments', payload);
    return res.data;
};
export const deleteInvestment = async (id) => {
    const res = await http.delete(`/investments/${id}`);
    return res.data;
};
export const getInvestment = async (id) => {
    const res = await http.get(`/investments/${id}`);
    return res.data;
};
export const getSymbols = async (type, keyword) => {
    const res = await http.get('/basic/symbols', { params: { type, keyword } });
    return res.data;
};
export const listCalendars = async (payload) => {
    const res = await http.get('/calendar/list', { params: payload });
    return res.data;
};
export const listBrokers = async (payload) => {
    const res = await http.get('/broker', { params: payload });
    return res.data;
};
export const getBroker = async (id) => {
    const res = await http.get(`/broker/${id}`);
    return res.data;
};
export const addBroker = async (payload) => {
    const res = await http.post('/broker', payload);
    return res.data;
};
export const updateBroker = async (payload) => {
    const res = await http.put('/broker', payload);
    return res.data;
};
export const deleteBroker = async (id) => {
    const res = await http.delete(`/broker/${id}`);
    return res.data;
};
export const listInvestmentLogs = async (payload) => {
    const res = await http.get('/investment-logs', { params: payload });
    return res.data;
};
export const addInvestmentLog = async (payload) => {
    const res = await http.post('/investment-logs', payload);
    return res.data;
};
export const updateInvestmentLog = async (payload) => {
    const res = await http.put('/investment-logs', payload);
    return res.data;
};
export const deleteInvestmentLog = async (id) => {
    const res = await http.delete(`/investment-logs/${id}`);
    return res.data;
};
export const listNodeGroups = async () => {
    const res = await http.get('/node/group/list');
    return res.data;
};
export const addNodeGroup = async (payload) => {
    const res = await http.post('/node/group', payload);
    return res.data;
};
export const updateNodeGroup = async (payload) => {
    const res = await http.put('/node/group', payload);
    return res.data;
};
export const deleteNodeGroup = async (id) => {
    const res = await http.delete(`/node/group/${id}`);
    return res.data;
};
//# sourceMappingURL=basic.js.map