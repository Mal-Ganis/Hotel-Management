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
        try {
            const response = await api.get('/rooms');
            if (response && response.success && response.data) {
                return response.data;
            } else if (Array.isArray(response)) {
                return response;
            } else if (response && Array.isArray(response.data)) {
                return response.data;
            }
            return [];
        } catch (error) {
            console.error('获取房间列表失败:', error);
            throw error;
        }
    },

    async getById(id) {
        const response = await api.get(`/rooms/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取房间详情失败');
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
        try {
            const response = await api.get('/reservations');
            if (response && response.success && response.data) {
                return response.data;
            } else if (Array.isArray(response)) {
                return response;
            } else if (response && Array.isArray(response.data)) {
                return response.data;
            }
            return [];
        } catch (error) {
            console.error('获取预订列表失败:', error);
            throw error;
        }
    },

    async getById(id) {
        const response = await api.get(`/reservations/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取预订详情失败');
    },

    async getByStatus(status) {
        const response = await api.get(`/reservations/status/${status}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '按状态获取预订失败');
    },

    async getByCheckInRange(startDate, endDate) {
        const response = await api.get(`/reservations/checkin-range?start=${startDate}&end=${endDate}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '按日期范围获取预订失败');
    },

    async update(id, data) {
        const response = await api.put(`/reservations/${id}`, data);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '更新预订失败');
    },

    async cancel(id) {
        const response = await api.post(`/reservations/${id}/cancel`, {});
        return response;
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
        if (response && response.success) {
            return response.data || {};
        }
        throw new Error(response?.message || '获取今日统计失败');
    },

    async getDateRange(startDate, endDate) {
        const response = await api.get(`/api/statistics/date-range?startDate=${startDate}&endDate=${endDate}`);
        if (response && response.success) {
            return response.data || {};
        }
        throw new Error(response?.message || '获取日期范围统计失败');
    }
};

// 宾客相关
const guests = {
    async getAll() {
        try {
            const response = await api.get('/guests');
            if (response && response.success && response.data) {
                return response.data;
            } else if (Array.isArray(response)) {
                return response;
            } else if (response && Array.isArray(response.data)) {
                return response.data;
            }
            return [];
        } catch (error) {
            console.error('获取宾客列表失败:', error);
            throw error;
        }
    },

    async getById(id) {
        const response = await api.get(`/guests/${id}`);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取宾客详情失败');
    },

    async search(keyword) {
        const response = await api.get(`/guests/search?keyword=${encodeURIComponent(keyword || '')}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '搜索宾客失败');
    },

    async searchByName(name) {
        const response = await api.get(`/guests/search/name?name=${encodeURIComponent(name)}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '按姓名搜索失败');
    },

    async searchByPhone(phone) {
        const response = await api.get(`/guests/search/phone?phone=${encodeURIComponent(phone)}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '按手机号搜索失败');
    },

    async searchByIdCard(idCardNumber) {
        const response = await api.get(`/guests/search/id-card?idCardNumber=${encodeURIComponent(idCardNumber)}`);
        if (response && response.success) {
            return response.data ? [response.data] : [];
        }
        throw new Error(response?.message || '按身份证号搜索失败');
    }
};

// 用户相关
const users = {
    async getAll() {
        const response = await api.get('/users');
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '获取用户列表失败');
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

// 操作日志相关
const logs = {
    async getAll(page = 0, size = 20, username = null, action = null, startDate = null, endDate = null) {
        let url = `/api/logs?page=${page}&size=${size}`;
        if (username) url += `&username=${encodeURIComponent(username)}`;
        if (action) url += `&action=${encodeURIComponent(action)}`;
        if (startDate) url += `&startDate=${startDate}`;
        if (endDate) url += `&endDate=${endDate}`;
        const response = await api.get(url);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '获取操作日志失败');
    },

    async getRecent(limit = 10) {
        const response = await api.get(`/api/logs/recent?limit=${limit}`);
        if (response && response.success) {
            return response.data || [];
        }
        throw new Error(response?.message || '获取最近操作日志失败');
// 系统设置相关
const settings = {
    async getAll() {
        const response = await api.get('/settings');
        if (response && response.success && response.data) {
            return response.data;
        }
        return [];
    },

    async getByKey(key) {
        const response = await api.get(`/settings/${key}`);
        if (response && response.success && response.data) {
            return response.data;
        }
        return null;
    },

    async save(settingData) {
        const response = await api.post('/settings', settingData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '保存设置失败');
    },

    async update(key, settingData) {
        const response = await api.put(`/settings/${key}`, settingData);
        if (response && response.success) {
            return response.data;
        }
        throw new Error(response?.message || '更新设置失败');
    }
};

