// API工具类
const API_BASE_URL = 'http://localhost:8080';

class ApiClient {
    constructor() {
        this.token = localStorage.getItem('token');
    }

    setToken(token) {
        this.token = token;
        if (token) {
            localStorage.setItem('token', token);
        } else {
            localStorage.removeItem('token');
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
            
            // 检查响应是否为空
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
                throw new Error(data.message || '请求失败');
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
    async login(email, password, role = 'GUEST') {
        const response = await api.post('/auth/login', { username: email, password: password });
        if (response.success && response.data && response.data.token) {
            api.setToken(response.data.token);
            localStorage.setItem('userRole', role);
            localStorage.setItem('userEmail', email);
        }
        return response;
    },

    async register(guestData) {
        return api.post('/guests', guestData);
    },

    logout() {
        api.setToken(null);
        localStorage.removeItem('userRole');
        localStorage.removeItem('userEmail');
        window.location.href = '/index.html';
    },

    isAuthenticated() {
        return !!api.token;
    },

    getUserRole() {
        return localStorage.getItem('userRole');
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

    async getAvailable(checkInDate, checkOutDate) {
        // 简化处理：获取所有房间，前端过滤
        return this.getAll();
    }
};

// 预订相关
const reservations = {
    async create(reservationData) {
        const response = await api.post('/reservations', reservationData);
        return response.data;
    },

    async getMyReservations() {
        const response = await api.get('/reservations/me');
        return response.data || [];
    },

    async cancel(id) {
        const response = await api.post(`/reservations/${id}/cancel`);
        return response.data;
    }
};

// 支付相关
const payments = {
    async createPayment(transactionId, amount, description) {
        const response = await api.post('/payments/create', {
            reservationId: transactionId,
            amount: amount,
            note: description
        });
        return response.data;
    },

    async simulatePayment(transactionId) {
        // 模拟支付成功
        const response = await api.post('/payments/callback', {
            transactionId: transactionId,
            status: 'SUCCESS',
            providerTransactionId: 'SIM_' + Date.now()
        });
        return response;
    }
};

