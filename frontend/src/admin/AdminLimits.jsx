import React, { useState, useEffect } from 'react'
import { getAdminLimits } from './adminApi'
import { Sliders, RefreshCw, ShieldAlert } from 'lucide-react'

export default function AdminLimits() {
  const [limits, setLimits] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchLimits = async () => {
    try {
      const data = await getAdminLimits()
      setLimits(data)
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải giới hạn hệ thống')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLimits()
  }, [])

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Giới Hạn Hệ Thống (Server Limits)</h2>
          <p className="subtitle">Các hằng số cấu hình bảo vệ tài nguyên và vận hành máy chủ (Read-Only)</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={fetchLimits}>
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}

      {limits && (
        <div className="limits-grid">
          <div className="limit-card">
            <span className="limit-title">Kết nối đồng thời tối đa (MAX_CLIENTS)</span>
            <span className="limit-value text-blue">{limits.MAX_CLIENTS}</span>
            <span className="limit-desc">
              Hiện tại đang có <strong>{limits.CURRENT_CLIENTS}</strong> kết nối hoạt động
            </span>
          </div>

          <div className="limit-card">
            <span className="limit-title">Độ dài tin nhắn (MAX_MESSAGE_LENGTH)</span>
            <span className="limit-value text-green">{limits.MAX_MESSAGE_LENGTH}</span>
            <span className="limit-desc">Số ký tự tối đa cho mỗi tin nhắn gửi đi</span>
          </div>

          <div className="limit-card">
            <span className="limit-title">Độ dài Username cho phép</span>
            <span className="limit-value text-purple">
              {limits.MIN_USERNAME_LENGTH} – {limits.MAX_USERNAME_LENGTH}
            </span>
            <span className="limit-desc">Ký tự hợp lệ: chữ cái (a-z, A-Z), số (0-9) và gạch dưới (_)</span>
          </div>

          <div className="limit-card">
            <span className="limit-title">Độ dài Mật khẩu tối thiểu</span>
            <span className="limit-value text-amber">{limits.MIN_PASSWORD_LENGTH}</span>
            <span className="limit-desc">Mật khẩu được băm SHA-256 một chiều bảo mật</span>
          </div>

          <div className="limit-card">
            <span className="limit-title">Thời gian chờ phản hồi chat riêng (REQUEST_TIMEOUT)</span>
            <span className="limit-value text-red">{limits.REQUEST_TIMEOUT}s</span>
            <span className="limit-desc">Sau 60 giây yêu cầu không được phản hồi sẽ tự động hủy</span>
          </div>

          <div className="limit-card">
            <span className="limit-title">Hạn giờ xác thực (AUTH_TIMEOUT)</span>
            <span className="limit-value">{limits.AUTH_TIMEOUT}s</span>
            <span className="limit-desc">Thời gian cho phép kết nối handshake WebSocket</span>
          </div>
        </div>
      )}
    </div>
  )
}
