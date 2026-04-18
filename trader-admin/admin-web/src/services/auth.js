import { http } from './http';
export const login = async (payload) => {
    const res = await http.post('/auth/login', payload);
    return res.data;
};
export const changePassword = async (payload) => {
    const res = await http.post('/user/change-password', payload);
    return res.data;
};
//# sourceMappingURL=auth.js.map