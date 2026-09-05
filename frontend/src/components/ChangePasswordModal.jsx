import React, { useState } from 'react'
import { authApi } from '../api/http'
import { KeyRound, X, Check } from 'lucide-react'

export default function ChangePasswordModal({ isOpen, onClose }) {
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  if (!isOpen) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (!oldPassword || !newPassword) {
      setError('Vui lòng nhập đầy đủ thông tin')
      return
    }

    if (newPassword.length < 6) {
      setError('Mật khẩu mới phải có ít nhất 6 ký tự')
      return
    }

    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp')
      return
    }

    try {
      setLoading(true)
      const res = await authApi.changePassword(oldPassword, newPassword)
      if (res.ok) {
        setSuccess('Đổi mật khẩu thành công!')
        setTimeout(() => {
          onClose()
          setOldPassword('')
          setNewPassword('')
          setConfirmPassword('')
          setSuccess('')
        }, 1200)
      } else {
        setError(res.message || 'Đổi mật khẩu thất bại')
      }
    } catch (err) {
      setError(err.message || 'Lỗi hệ thống')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <KeyRound size={20} color="var(--primary)" />
            <h3>Đổi Mật Khẩu</h3>
          </div>
          <button type="button" className="icon-btn" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        {error && <div className="alert-error">{error}</div>}
        {success && <div className="alert-success">{success}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="old-pass">Mật khẩu hiện tại</label>
            <input
              id="old-pass"
              type="password"
              placeholder="Nhập mật khẩu cũ"
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="new-pass">Mật khẩu mới</label>
            <input
              id="new-pass"
              type="password"
              placeholder="Tối thiểu 6 ký tự"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirm-pass">Xác nhận mật khẩu mới</label>
            <input
              id="confirm-pass"
              type="password"
              placeholder="Nhập lại mật khẩu mới"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="modal-actions" style={{ marginTop: 24 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={loading}>
              Hủy
            </button>
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={loading}>
              <Check size={16} />
              {loading ? 'Đang lưu...' : 'Cập nhật'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
