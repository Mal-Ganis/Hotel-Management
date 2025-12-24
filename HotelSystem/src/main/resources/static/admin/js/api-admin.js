// 管理端API工具类 - 使用8081端口
const API_BASE_URL = 'http://localhost:8081';

class ApiClient {
    constructor() {
        this.token = localStorage.getItem('adminToken');
    }

    setToken(token) {
        this.token = token;
        if (token) {
            localStorage.setItem('adminToken', token);
        } else {
            localStorage.removeItem('adminToken');
        }
    }

    async request(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(`${API_BASE_URL}${url}`, config);
            
            const text = await response.text();
            if (!text) {
                throw new Error('响应为空');
            }
            
            let data;
            try {
                data = JSON.parse(text);
            } catch (e) {
                console.error('JSON解析失败:', text);
                throw new Error('响应格式错误: ' + text.substring(0, 100));
            }
            
            if (!response.ok) {
                // 处理权限错误
                if (response.status === 401 || response.status === 403) {
                    const errorMsg = data?.message || '权限不足或登录已过期';
                    // 如果是权限问题，清除token并跳转登录
                    if (response.status === 401) {
                        this.setToken(null);
                        localStorage.removeItem('adminRole');
                        localStorage.removeItem('adminUsername');
                        if (window.location.pathname !== '/admin/login.html') {
                            window.location.href = '/admin/login.html';
                        }
                    }
                    throw new Error(errorMsg);
                }
                throw new Error(data?.message || `请求失败 (${response.status})`);
            }
            
            return data;
        } catch (error) {
            console.error('API请求错误:', error);
            throw error;
        }
    }

    async get(url) {
        return this.request(url, { method: 'GET' });
    }

    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async delete(url) {
        return this.request(url, { method: 'DELETE' });
    }
}

const api = new ApiClient();

// 认证相关
const auth = {
    async login(username, password, role) {
        // 传递角色信息到后端进行验证
        const response = await api.post('/auth/login', { username, password, role: role });
        if (response.success && response.data && response.data.token) {
            api.setToken(response.data.token);
            localStorage.setItem('adminRole', role);
            localStorage.setItem('adminUsername', username);
        }
        return response;
    },

    logout() {
        api.setToken(null);
        localStorage.removeItem('adminRole');
        localStorage.removeItem('adminUsername');
        window.location.href = '/admin/login.html';
    },

    isAuthenticated() {
        return !!api.token;
    },

    getUserRole() {
        return localStorage.getItem('adminRole');
    }
};

// 房间相关
const rooms = {
    async getAll() {
        const response = await api.get('/rooms');
        return response.data || [];
    },

    async getById(id) {
        const response = await api.get(`/rooms/${id}`);
        return response.data;
    },

    async create(roomData) {
        const response = await api.post('/rooms', roomData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '创建房间失败');
    },

    async update(id, roomData) {
        const response = await api.put(`/rooms/${id}`, roomData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '更新房间失败');
    },

    async delete(id) {
        const response = await api.delete(`/rooms/${id}`);
        if (response && response.success) {
            return response;
        }
        throw new Error(response?.message || '删除房间失败');
    }
};

// 预订相关
const reservations = {
    async getAll() {
        const response = await api.get('/reservations');
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '获取预订列表失败');
    },

    async getById(id) {
        const response = await api.get(`/reservations/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取预订详情失败');
    },

    async checkIn(reservationId, data) {
        const response = await api.post(`/frontdesk/checkin/${reservationId}`, data || {});
        return response;
    },

    async checkOut(reservationId, data) {
        const response = await api.post(`/frontdesk/checkout/${reservationId}`, data || {});
        return response;
    }
};

// 统计相关
const statistics = {
    async getToday() {
        const response = await api.get('/api/statistics/today');
        return response.data || {};
    },

    async getDateRange(startDate, endDate) {
        const response = await api.get(`/api/statistics/date-range?startDate=${startDate}&endDate=${endDate}`);
        return response.data || {};
    }
};

// 宾客相关
const guests = {
    async getAll() {
        const response = await api.get('/guests');
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '获取宾客列表失败');
    },

    async getById(id) {
        const response = await api.get(`/guests/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取宾客详情失败');
    }
};

// 用户相关
const users = {
    async getAll() {
        const response = await api.get('/users');
        return response.data || [];
    },

    async getById(id) {
        const response = await api.get(`/users/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取用户失败');
    },

    async create(userData) {
        const response = await api.post('/users', userData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '创建用户失败');
    },

    async update(id, userData) {
        const response = await api.put(`/users/${id}`, userData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '更新用户失败');
    },

    async delete(id) {
        const response = await api.delete(`/users/${id}`);
        return response;
    }
};

