// 权限管理工具
const permissions = {
    // 检查用户是否有某个角色
    hasRole(role) {
        const userRole = auth.getUserRole();
        return userRole === role;
    },

    // 检查用户是否有任一角色
    hasAnyRole(...roles) {
        const userRole = auth.getUserRole();
        return roles.includes(userRole);
    },

    // 检查用户是否是管理员
    isAdmin() {
        return this.hasRole('ADMIN');
    },

    // 检查用户是否是经理或管理员
    isManagerOrAdmin() {
        return this.hasAnyRole('MANAGER', 'ADMIN');
    },

    // 检查用户是否是前台、经理或管理员
    isReceptionistOrAbove() {
        return this.hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN');
    },

    // 根据角色显示/隐藏元素
    showForRoles(elementId, ...roles) {
        const element = document.getElementById(elementId);
        if (element && this.hasAnyRole(...roles)) {
            element.style.display = '';
        } else if (element) {
            element.style.display = 'none';
        }
    },

    // 根据角色隐藏元素
    hideForRoles(elementId, ...roles) {
        const element = document.getElementById(elementId);
        if (element && this.hasAnyRole(...roles)) {
            element.style.display = 'none';
        }
    },

    // 检查权限，如果没有权限则显示提示并返回false
    checkPermission(allowedRoles, actionName) {
        if (!this.hasAnyRole(...allowedRoles)) {
            const roleName = this.getRoleDisplayName();
            alert(`权限不足：${roleName}无法${actionName}。请联系管理员。`);
            return false;
        }
        return true;
    },

    // 获取角色显示名称
    getRoleDisplayName() {
        const role = auth.getUserRole();
        const roleMap = {
            'ADMIN': '管理员',
            'MANAGER': '经理',
            'RECEPTIONIST': '前台',
            'HOUSEKEEPING': '房务'
        };
        return roleMap[role] || role;
    },

    // 统一的菜单显示/隐藏函数 - 根据角色控制菜单可见性
    hideUnauthorizedMenus() {
        const userRole = auth.getUserRole();
        if (!userRole) {
            console.warn('未获取到用户角色，跳过菜单权限设置');
            return;
        }

        // 定义菜单权限规则
        const menuRules = {
            'reservationsMenuLink': ['ADMIN', 'MANAGER', 'RECEPTIONIST'], // 预订管理
            'roomStatusMenuLink': ['ADMIN', 'MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING'], // 房态管理
            'roomsMenuLink': ['ADMIN', 'MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING'], // 房间管理
            'guestsMenuLink': ['ADMIN', 'MANAGER', 'RECEPTIONIST'], // 宾客管理
            'statisticsMenuLink': ['ADMIN', 'MANAGER', 'RECEPTIONIST'], // 统计报表
            'logsMenuLink': ['ADMIN', 'MANAGER'], // 操作日志
            'settingsMenuLink': ['ADMIN', 'MANAGER'], // 规则设置
            'usersMenuLink': ['ADMIN'] // 员工管理
        };

        // 根据权限规则显示/隐藏菜单
        Object.keys(menuRules).forEach(menuId => {
            const menuElement = document.getElementById(menuId);
            if (menuElement) {
                const allowedRoles = menuRules[menuId];
                if (allowedRoles.includes(userRole)) {
                    menuElement.style.display = 'block';
                } else {
                    menuElement.style.display = 'none';
                }
            } else {
                // 如果菜单项不存在，记录警告但不影响其他菜单
                console.warn(`菜单项 ${menuId} 不存在`);
            }
        });
    },

    // 检查页面访问权限，无权限则跳转
    checkPageAccess(allowedRoles, redirectUrl = '/admin/dashboard.html') {
        const userRole = auth.getUserRole();
        if (!allowedRoles.includes(userRole)) {
            alert(`权限不足：您无权访问此页面。`);
            window.location.href = redirectUrl;
            return false;
        }
        return true;
    }
};



