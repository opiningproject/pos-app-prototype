const API_BASE_URL = 'https://admin.dryfftjwieiwjw.online';

const OrderStatus = Object.freeze({
  Accepted: '1',
  InKitchen: '2',
  Ready: '3',
  ReadyForPickup: '4',
  OutForDelivery: '5',
  Delivered: '6',
  Cancelled: '7'
});

function handleTokenExpiration(responseData, httpStatus) {
  const isExpired = (
    httpStatus === 401 ||
    responseData.status === '2' ||
    responseData.status === 2 ||
    responseData.message === 'Token is Expired' ||
    responseData.message === 'Authorization Token not found' ||
    responseData.message === 'Unauthenticated.'
  );

  if (isExpired) {
    localStorage.removeItem('auth_token');
    if (typeof doLogout === 'function') {
      doLogout();
    } else if (typeof window.doLogout === 'function') {
      window.doLogout();
    }
  }
  return isExpired;
}

const API = {
  /**
   * Log in user with credentials.
   * @param {string} email
   * @param {string} password
   * @returns {Promise<Object>} The login response data
   */
  async login(email, password) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'application/json'
        },
        body: new URLSearchParams({ email, password })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(data.message || `Login failed (Status ${response.status})`);
      }

      if (data.status === '0' || data.status === 0 || data.status === false) {
        throw new Error(data.message || 'Login failed');
      }

      return data;
    } catch (error) {
      console.error('API login error:', error);
      throw error;
    }
  },

  /**
   * Fetch all orders from the server.
   * @returns {Promise<Object>} The orders data
   */
  async getOrders(params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      if (params.status) {
        body.append('status', params.status);
      }
      if (params.order_id) {
        body.append('order_id', params.order_id);
      }
      const response = await fetch(`${API_BASE_URL}/api/v1/getOrders`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to fetch orders (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch orders');
      }

      return responseData;
    } catch (error) {
      console.error('API getOrders error:', error);
      throw error;
    }
  },

  /**
   * Fetch today's sales summary from the server.
   * @param {Object} params - parameters such as { type: 'summary'|'details'|'cancelled' }
   * @returns {Promise<Object>} The summary data
   */
  async getSummary(params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      if (params.type) {
        body.append('type', params.type);
      }
      const response = await fetch(`${API_BASE_URL}/api/v1/getSummary`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to fetch summary (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch summary');
      }

      return responseData;
    } catch (error) {
      console.error('API getSummary error:', error);
      throw error;
    }
  },

  /**
   * OrderStatus constants.
   */
  OrderStatus: OrderStatus,

  /**
   * Change order status.
   * @param {string|number|Object} orderId - Order ID or object containing order_id and order_status
   * @param {string|number} [orderStatus] - Order status value (e.g. '2' for InKitchen, '6' for Delivered)
   * @returns {Promise<Object>} The API response data
   */
  async changeOrderStatus(orderId, orderStatus) {
    try {
      let id = orderId;
      let status = orderStatus;
      if (typeof orderId === 'object' && orderId !== null) {
        id = orderId.order_id;
        status = orderId.order_status !== undefined ? orderId.order_status : orderId.status;
      }
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      if (id !== undefined && id !== null) {
        body.append('id', id);
        body.append('order_id', id);
      }
      if (status !== undefined && status !== null) {
        body.append('order_status', status);
      }
      const response = await fetch(`${API_BASE_URL}/api/v1/changeOrderStatus`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to change order status (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to change order status');
      }

      return responseData;
    } catch (error) {
      console.error('API changeOrderStatus error:', error);
      throw error;
    }
  },

  /**
   * Cancel an order.
   * @param {string|number|Object} orderId - Order ID or object with parameters
   * @param {string|number} [status='7'] - Cancelled status (default '7')
   * @returns {Promise<Object>} The API response data
   */
  async cancelOrder(orderId, status = '7') {
    try {
      let id = orderId;
      let orderStatus = status;
      if (typeof orderId === 'object' && orderId !== null) {
        id = orderId.order_id !== undefined ? orderId.order_id : orderId.id;
        orderStatus = orderId.status !== undefined ? orderId.status : (orderId.order_status !== undefined ? orderId.order_status : '7');
      }
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      if (id !== undefined && id !== null) {
        body.append('id', id);
        body.append('order_id', id);
      }
      const finalStatus = String(orderStatus !== undefined && orderStatus !== null ? orderStatus : '7');
      body.append('status', finalStatus);
      body.append('order_status', finalStatus);

      const response = await fetch(`${API_BASE_URL}/api/v1/cancelOrder`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to cancel order (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to cancel order');
      }

      return responseData;
    } catch (error) {
      console.error('API cancelOrder error:', error);
      throw error;
    }
  },

  /**
   * Fetch categories from the server.
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The categories response data
   */
  async getCategories(params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      let response = await fetch(`${API_BASE_URL}/api/v1/getCategories`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams()
      });

      if (response.status === 405 || response.status === 404) {
        response = await fetch(`${API_BASE_URL}/api/v1/getCategories`, {
          method: 'GET',
          headers: headers
        });
      }

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to fetch categories (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch categories');
      }

      return responseData;
    } catch (error) {
      console.error('API getCategories error:', error);
      throw error;
    }
  },

  /**
   * Change status of a dish (active/inactive).
   * Endpoint: /api/v1/change-dish-status/{id}
   * @param {string|number} dishId - The ID of the dish
   * @param {number|string} status - 1 for active, 0 for inactive
   * @returns {Promise<Object>} The API response data
   */
  async changeDishStatus(dishId, status) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const finalStatus = (status === 1 || status === '1' || status === true) ? '1' : '0';
      const body = new URLSearchParams();
      body.append('status', finalStatus);
      body.append('dish_id', String(dishId));
      body.append('id', String(dishId));

      const response = await fetch(`${API_BASE_URL}/api/v1/change-dish-status/${dishId}`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to change dish status (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to change dish status');
      }

      return responseData;
    } catch (error) {
      console.error('API changeDishStatus error:', error);
      throw error;
    }
  },

  /**
   * Update restaurant open/closed status.
   * Endpoint: /api/v1/updateRestaurantStatus
   * @param {number|string} isOpen - 1 for Open, 0 for Closed
   * @returns {Promise<Object>} The API response data
   */
  async updateRestaurantStatus(isOpen) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const finalVal = (isOpen === 1 || isOpen === '1' || isOpen === true) ? '1' : '0';
      const body = new URLSearchParams();
      body.append('is_open', finalVal);

      const response = await fetch(`${API_BASE_URL}/api/v1/updateRestaurantStatus`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to update restaurant status (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to update restaurant status');
      }

      return responseData;
    } catch (error) {
      console.error('API updateRestaurantStatus error:', error);
      throw error;
    }
  }
};
