import { http } from './http';
export const getDashboardOverview = () => {
    return http.get('/dashboard/overview').then(res => res.data);
};
export const getSystemStatus = () => {
    return http.get('/dashboard/system-status').then(res => res.data);
};
//# sourceMappingURL=dashboard.js.map