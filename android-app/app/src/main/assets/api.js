const API_BASE_URL = 'https://admin.dryfftjwieiwjw.online';
const DEFAULT_TIMEZONE = 'Europe/Amsterdam';

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
  DEFAULT_TIMEZONE: DEFAULT_TIMEZONE,

  /**
   * Log in user with credentials.
   * @param {string} email
   * @param {string} password
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The login response data
   */
  async login(email, password, params = {}) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'application/json'
        },
        body: new URLSearchParams({
          email,
          password,
          timezone: (params && params.timezone) || DEFAULT_TIMEZONE
        })
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
      body.append('timezone', params.timezone || DEFAULT_TIMEZONE);

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
   * Fetch new order details from the server.
   * Endpoint: /api/v1/getNewOrderDetails
   * @param {string|number} [orderId] - Optional order ID
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The new order details response data
   */
  async getNewOrderDetails(orderId, params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      if (orderId !== undefined && orderId !== null) {
        body.append('order_id', String(orderId));
      }
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

      const response = await fetch(`${API_BASE_URL}/api/v1/getNewOrderDetails`, {
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
        throw new Error(responseData.message || `Failed to fetch new order details (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch new order details');
      }

      return responseData;
    } catch (error) {
      console.error('API getNewOrderDetails error:', error);
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
      body.append('timezone', params.timezone || DEFAULT_TIMEZONE);

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
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The API response data
   */
  async changeOrderStatus(orderId, orderStatus, params = {}) {
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
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

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
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The API response data
   */
  async cancelOrder(orderId, status = '7', params = {}) {
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
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

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
      if (!token) {
        console.warn('API.getCategories: No auth token found, skipping category request before login.');
        return Promise.reject(new Error('Unauthenticated: No auth token found'));
      }
      const headers = {
        'Accept': 'application/json',
        'Authorization': `Bearer ${token}`
      };
      const body = new URLSearchParams();
      body.append('timezone', params.timezone || DEFAULT_TIMEZONE);

      let response = await fetch(`${API_BASE_URL}/api/v1/getCategories`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      if (response.status === 405 || response.status === 404) {
        response = await fetch(`${API_BASE_URL}/api/v1/getCategories?timezone=${encodeURIComponent(params.timezone || DEFAULT_TIMEZONE)}`, {
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
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The API response data
   */
  async changeDishStatus(dishId, status, params = {}) {
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
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

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
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The API response data
   */
  async updateRestaurantStatus(isOpen, params = {}) {
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
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

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
  },

  /**
   * Fetch restaurant details from server.
   * Endpoint: /api/v1/getRestaurentDetails
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The restaurant details data
   */
  async getRestaurentDetails(params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const body = new URLSearchParams();
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

      let response = await fetch(`${API_BASE_URL}/api/v1/getRestaurentDetails`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      if (response.status === 405 || response.status === 404) {
        response = await fetch(`${API_BASE_URL}/api/v1/getRestaurentDetails?timezone=${encodeURIComponent((params && params.timezone) || DEFAULT_TIMEZONE)}`, {
          method: 'GET',
          headers: headers
        });
      }

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to fetch restaurant details (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch restaurant details');
      }

      return responseData;
    } catch (error) {
      console.error('API getRestaurentDetails error:', error);
      throw error;
    }
  },

  /**
   * Update delivery and takeaway default times.
   * Endpoint: /api/v1/updateDeliveryTime
   * @param {string|number} deliveryTime - e.g. "45 Min" or 45
   * @param {string|number} takeawayTime - e.g. "15 Min" or 15
   * @param {Object} [params={}]
   * @returns {Promise<Object>} The API response data
   */
  async updateDeliveryTime(param1, param2, params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const body = new URLSearchParams();
      let isOrderUpdate = false;
      let orderIdVal = null;
      let expectedTimeVal = null;

      if (typeof param1 === 'object' && param1 !== null) {
        isOrderUpdate = true;
        orderIdVal = param1.order_id || param1.orderId || param1.id;
        expectedTimeVal = param1.expected_time || param1.expected_delivery_time || param1.time;
      } else if (param1 && param2 && (String(param2).indexOf(':') !== -1 || String(param2).indexOf('-') !== -1)) {
        isOrderUpdate = true;
        orderIdVal = param1;
        expectedTimeVal = param2;
      }

      if (isOrderUpdate) {
        body.append('order_id', String(orderIdVal));
        body.append('orderId', String(orderIdVal));
        body.append('expected_time', String(expectedTimeVal));
        body.append('expected_delivery_time', String(expectedTimeVal));
        body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);
      } else {
        var delStr = String(param1).indexOf('Min') !== -1 ? param1 : (parseInt(param1, 10) + ' Min');
        var pickStr = String(param2).indexOf('Min') !== -1 ? param2 : (parseInt(param2, 10) + ' Min');
        body.append('delivery_time', delStr);
        body.append('take_away_time', pickStr);
        body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);
      }

      let response = await fetch(`${API_BASE_URL}/api/v1/updateDeliveryTime`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      if (response.status === 405 || response.status === 404) {
        let queryStr = body.toString();
        response = await fetch(`${API_BASE_URL}/api/v1/updateDeliveryTime?${queryStr}`, {
          method: 'GET',
          headers: headers
        });
      }

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to update delivery time (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to update delivery time');
      }

      return responseData;
    } catch (error) {
      console.error('API updateDeliveryTime error:', error);
      throw error;
    }
  },

  async updateOrderDeliveryTime(orderId, expectedTime, params = {}) {
    return this.updateDeliveryTime({ order_id: orderId, expected_time: expectedTime }, null, params);
  },

  /**
   * Update order wished / expected delivery time.
   * Endpoint: /api/v1/updateWishedTime
   * @param {string|number|Object} param1 - order_id or payload object
   * @param {string} [param2] - expected_time
   * @param {Object} [params={}]
   * @returns {Promise<Object>}
   */
  async updateWishedTime(param1, param2, params = {}) {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = {
        'Accept': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      let orderIdVal = null;
      let expectedTimeVal = null;

      if (typeof param1 === 'object' && param1 !== null) {
        orderIdVal = param1.order_id || param1.orderId || param1.id;
        expectedTimeVal = param1.expected_time || param1.expected_delivery_time || param1.wished_time || param1.time;
      } else {
        orderIdVal = param1;
        expectedTimeVal = param2;
      }

      const body = new URLSearchParams();
      body.append('order_id', String(orderIdVal));
      body.append('orderId', String(orderIdVal));
      body.append('expected_time', String(expectedTimeVal));
      body.append('wished_time', String(expectedTimeVal));
      body.append('expected_delivery_time', String(expectedTimeVal));
      body.append('timezone', (params && params.timezone) || DEFAULT_TIMEZONE);

      let response = await fetch(`${API_BASE_URL}/api/v1/updateWishedTime`, {
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      });

      if (response.status === 405 || response.status === 404) {
        let queryStr = body.toString();
        response = await fetch(`${API_BASE_URL}/api/v1/updateWishedTime?${queryStr}`, {
          method: 'GET',
          headers: headers
        });
      }

      const responseData = await response.json().catch(() => ({}));

      if (handleTokenExpiration(responseData, response.status)) {
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (!response.ok) {
        throw new Error(responseData.message || `Failed to update wished time (Status ${response.status})`);
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to update wished time');
      }

      return responseData;
    } catch (error) {
      console.error('API updateWishedTime error:', error);
      throw error;
    }
  }
};
