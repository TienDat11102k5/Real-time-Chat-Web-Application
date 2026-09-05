const ADMIN_TOKEN_KEY = 'chat_admin_token'
const ADMIN_USER_KEY = 'chat_admin_user'
const ADMIN_ROLE_KEY = 'chat_admin_role'

export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY)
}

export function getAdminUser() {
  return localStorage.getItem(ADMIN_USER_KEY)
}

export function getAdminRole() {
  return localStorage.getItem(ADMIN_ROLE_KEY)
}

export function setAdminAuth(token, username, role) {
  if (token) localStorage.setItem(ADMIN_TOKEN_KEY, token)
  if (username) localStorage.setItem(ADMIN_USER_KEY, username)
  if (role) localStorage.setItem(ADMIN_ROLE_KEY, role)
}

export function clearAdminAuth() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_USER_KEY)
  localStorage.removeItem(ADMIN_ROLE_KEY)
}

export function isAdminAuthenticated() {
  return !!getAdminToken()
}

async function request(endpoint, options = {}) {
  const token = getAdminToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(endpoint, {
    ...options,
    headers
  })

  // Nếu 401 Unauthorized -> xóa auth và chuyển về login
  if (response.status === 401 && !endpoint.includes('/api/admin/login')) {
    clearAdminAuth()
    window.location.hash = '#/admin/login'
    throw new Error('Phiên đăng nhập quản trị đã hết hạn')
  }

  const data = await response.json().catch(() => ({}))

  if (!response.ok) {
    throw new Error(data.message || data.error || `HTTP ${response.status}`)
  }

  return data
}

// B1. Đăng nhập
export async function adminLogin(username, password) {
  const data = await request('/api/admin/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
  if (data.ok && data.token) {
    setAdminAuth(data.token, data.username, 'admin')
  }
  return data
}

// B3. Dashboard stats
export async function getAdminStats() {
  return request('/api/admin/stats')
}

// B4. Users online
export async function getAdminUsers() {
  return request('/api/admin/users')
}

export async function disconnectUser(username, reason = '') {
  return request(`/api/admin/users/${encodeURIComponent(username)}/disconnect`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  })
}

// B5. Accounts
export async function getAdminAccounts(query = '', page = 1, limit = 10) {
  const params = new URLSearchParams()
  if (query) params.append('query', query)
  params.append('page', page)
  params.append('limit', limit)
  return request(`/api/admin/accounts?${params.toString()}`)
}

export async function getAdminAccountDetail(username) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}`)
}

export async function kickAccount(username, reason = '') {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/kick`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  })
}

export async function lockAccount(username) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/lock`, {
    method: 'POST'
  })
}

export async function unlockAccount(username) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/unlock`, {
    method: 'POST'
  })
}

export async function resetPassword(username, newPassword) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ new_password: newPassword })
  })
}

export async function changeRole(username, role) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role })
  })
}

export async function deleteAccount(username) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}`, {
    method: 'DELETE'
  })
}

export async function getAccountMessages(username, limit = 50) {
  return request(`/api/admin/accounts/${encodeURIComponent(username)}/messages?limit=${limit}`)
}

// B9. Rooms
export async function getAdminRooms() {
  return request('/api/admin/rooms')
}

// B10. Private Requests
export async function getAdminRequests() {
  return request('/api/admin/private-requests')
}

export async function cancelPrivateRequest(sender, receiver) {
  return request(`/api/admin/private-requests/${encodeURIComponent(sender)}/${encodeURIComponent(receiver)}`, {
    method: 'DELETE'
  })
}

// B11. Limits
export async function getAdminLimits() {
  return request('/api/admin/limits')
}

// B12 & B13. Logs
export async function getAdminLogs(limit = 100, type = '') {
  const params = new URLSearchParams()
  params.append('limit', limit)
  if (type) params.append('type', type)
  return request(`/api/admin/logs?${params.toString()}`)
}
