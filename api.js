const API_BASE_URL = 'https://9569.dryfftjwieiwjw.online';

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

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('auth_token');
        }
        throw new Error(responseData.message || `Failed to fetch orders (Status ${response.status})`);
      }

      if (responseData.status === '2' || responseData.status === 2 || responseData.message === 'Token is Expired') {
        localStorage.removeItem('auth_token');
        throw new Error(responseData.message || 'Token is Expired');
      }

      if (responseData.status === '0' || responseData.status === 0 || responseData.status === false) {
        throw new Error(responseData.message || 'Failed to fetch orders');
      }

      return responseData;
    } catch (error) {
      console.error('API getOrders error:', error);
      throw error;
    }
  }
};
