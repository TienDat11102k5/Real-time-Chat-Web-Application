import React, { useState } from 'react'
import { authApi } from '../api/http'
import { UserPlus, MessageSquare } from 'lucide-react'

export default function RegisterForm({ onSwitchToLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    const cleanUser = username.trim()
    if (!cleanUser) {
      setError('Tên tài khoản không được để trống')
      return
    }

    if (cleanUser.length < 3 || cleanUser.length > 20) {
      setError('Tên tài khoản phải từ 3 đến 20 ký tự')
      return
    }

    if (!/^[a-zA-Z0-9_]+$/.test(cleanUser)) {
      setError('Tên tài khoản chỉ được chứa chữ, số và dấu gạch dưới')
      return
    }

    if (['ADMIN', 'SERVER', 'SYSTEM', 'ROOT'].includes(cleanUser.toUpperCase())) {
      setError('Tên tài khoản không được sử dụng từ khóa hệ thống')
      return
    }

    if (password.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự')
      return
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu nhập lại không khớp')
      return
    }

    try {
      setLoading(true)
      const res = await authApi.register(cleanUser, password)
      if (res.ok) {
        setSuccess('Đăng ký thành công! Đang chuyển về trang đăng nhập...')
        setTimeout(() => {
          onSwitchToLogin()
        }, 1500)
      } else {
        setError(res.message || 'Đăng ký không thành công')
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
          <h1>Tạo Tài Khoản</h1>
          <p>Đăng ký để tham gia phòng chat</p>
        </div>

        {error && <div className="alert-error">{error}</div>}
        {success && <div className="alert-success">{success}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="reg-username">Tên tài khoản</label>
            <input
              id="reg-username"
              type="text"
              placeholder="Chữ, số, gạch dưới (3-20 ký tự)"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-password">Mật khẩu</label>
            <input
              id="reg-password"
              type="password"
              placeholder="Tối thiểu 6 ký tự"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-confirm">Xác nhận mật khẩu</label>
            <input
              id="reg-confirm"
              type="password"
              placeholder="Nhập lại mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            <UserPlus size={18} />
            {loading ? 'Đang tạo...' : 'Đăng ký'}
          </button>
        </form>

        <div className="auth-footer">
          Đã có tài khoản?
          <button type="button" onClick={onSwitchToLogin}>
            Đăng nhập
          </button>
        </div>
      </div>
    </div>
  )
}
