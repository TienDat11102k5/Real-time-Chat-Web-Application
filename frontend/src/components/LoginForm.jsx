import React, { useState } from 'react'
import { authApi, setToken, setStoredUsername } from '../api/http'
import { LogIn, MessageSquare } from 'lucide-react'

export default function LoginForm({ onLoginSuccess, onSwitchToRegister }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!username.trim() || !password.trim()) {
      setError('Vui lòng nhập tên tài khoản và mật khẩu')
      return
    }

    try {
      setLoading(true)
      const res = await authApi.login(username.trim(), password.trim())
      if (res.ok && res.token) {
        setToken(res.token)
        setStoredUsername(res.username)
        onLoginSuccess(res.token, res.username)
      } else {
        setError(res.message || 'Đăng nhập không thành công')
      }
    } catch (err) {
      setError(err.message || 'Lỗi kết nối tới máy chủ')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 12 }}>
            <div className="avatar-badge" style={{ width: 50, height: 50 }}>
              <MessageSquare size={26} />
            </div>
          </div>
          <h1>Đăng Nhập</h1>
          <p>Hệ thống Chat Realtime React + Spring Boot</p>
        </div>

        {error && <div className="alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Tên tài khoản</label>
            <input
              id="username"
              type="text"
              placeholder="Nhập username (3-20 ký tự)"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mật khẩu</label>
            <input
              id="password"
              type="password"
              placeholder="Nhập mật khẩu (từ 6 ký tự)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            <LogIn size={18} />
            {loading ? 'Đang xử lý...' : 'Đăng nhập'}
          </button>
        </form>

        <div className="auth-footer">
          Chưa có tài khoản?
          <button type="button" onClick={onSwitchToRegister}>
            Đăng ký ngay
          </button>
        </div>
      </div>
    </div>
  )
}
