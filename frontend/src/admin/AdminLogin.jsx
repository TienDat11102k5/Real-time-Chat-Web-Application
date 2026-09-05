import React, { useState } from 'react'
import { adminLogin } from './adminApi'
import { ShieldCheck, ArrowLeft } from 'lucide-react'

export default function AdminLogin({ onLoginSuccess, onBackToChat }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username.trim() || !password.trim()) {
      setError('Vui lòng nhập đầy đủ tên tài khoản và mật khẩu admin')
      return
    }

    setLoading(true)
    setError('')

    try {
      const res = await adminLogin(username.trim(), password.trim())
      if (res.ok) {
        onLoginSuccess(res)
      } else {
        setError(res.message || 'Đăng nhập thất bại')
      }
    } catch (err) {
      if (err.message && (err.message.includes('Not Found') || err.message.includes('404'))) {
        setError('Máy chủ backend chưa sẵn sàng hoặc đang khởi động. Vui lòng bấm thử lại!')
      } else if (err.message && err.message.includes('Failed to fetch')) {
        setError('Không thể kết nối đến máy chủ. Hãy đảm bảo Backend (port 8080) đang chạy.')
      } else {
        setError(err.message || 'Lỗi kết nối máy chủ')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card" style={{ maxWidth: '440px' }}>
        <div className="auth-header">
          <div
            style={{
              display: 'inline-flex',
              padding: '14px',
              borderRadius: '50%',
              background: 'rgba(56, 189, 248, 0.15)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              marginBottom: '16px',
              boxShadow: '0 0 24px rgba(56, 189, 248, 0.25)'
            }}
          >
            <ShieldCheck size={38} color="#38bdf8" />
          </div>
          <h2>Bảng Quản Trị Hệ Thống</h2>
          <p className="auth-subtitle">Đăng nhập tài khoản Quản trị viên (Admin Panel)</p>
        </div>

        {error && <div className="alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="admin-username">Tài khoản Quản trị</label>
            <input
              id="admin-username"
              type="text"
              placeholder="Nhập username admin..."
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="admin-password">Mật khẩu</label>
            <input
              id="admin-password"
              type="password"
              placeholder="Nhập mật khẩu admin..."
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{
              marginTop: '10px',
              height: '46px',
              fontSize: '0.95rem',
              fontWeight: '600',
              background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.35)',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            <ShieldCheck size={18} />
            {loading ? 'Đang xác thực...' : 'Truy Cập Admin Dashboard'}
          </button>
        </form>

        <div className="auth-footer" style={{ marginTop: '20px', paddingTop: '16px' }}>
          <button
            type="button"
            onClick={onBackToChat}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.85rem'
            }}
          >
            <ArrowLeft size={16} /> Quay lại màn hình Chat
          </button>
        </div>
      </div>
    </div>
  )
}
