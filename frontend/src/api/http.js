const BASE_URL = '/api'

export const getToken = () => localStorage.getItem('chat_token')
export const setToken = (token) => localStorage.setItem('chat_token', token)
export const getStoredUsername = () => localStorage.getItem('chat_username')
export const setStoredUsername = (user) => localStorage.setItem('chat_username', user)
export const clearAuth = () => {
  localStorage.removeItem('chat_token')
  localStorage.removeItem('chat_username')
}

async function request(endpoint, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  try {
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers
    })

    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      throw new Error(data.message || `Yêu cầu thất bại (${res.status})`)
    }
    return data
  } catch (err) {
    throw err
  }
}

export const authApi = {
  login: (username, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),

  register: (username, password) =>
    request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),

  changePassword: (oldPassword, newPassword) =>
    request('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword })
    }),

  getMe: () => request('/auth/me')
}

export const messageApi = {
  getPublicHistory: (limit = 50) => request(`/messages/public/history?limit=${limit}`),
  getPrivateHistory: (target, limit = 50) => request(`/messages/private/${encodeURIComponent(target)}/history?limit=${limit}`)
}

export const userApi = {
  getOnlineUsers: () => request('/users/online')
}

export const serverApi = {
  getLimits: () => request('/server/limits')
}
