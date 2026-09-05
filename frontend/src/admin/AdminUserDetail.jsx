import React, { useState, useEffect } from 'react'
import { getAdminAccountDetail, getAccountMessages, kickAccount, lockAccount, unlockAccount } from './adminApi'
import { ArrowLeft, User, Shield, MessageSquare, Clock, Lock, Unlock, UserX, RefreshCw } from 'lucide-react'

export default function AdminUserDetail({ username, onBack }) {
  const [account, setAccount] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const fetchDetail = async () => {
    setLoading(true)
    try {
      const [accData, msgData] = await Promise.all([
        getAdminAccountDetail(username),
        getAccountMessages(username, 50)
      ])
      setAccount(accData)
      setMessages(msgData || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải thông tin chi tiết')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDetail()
  }, [username])

  const handleKick = async () => {
    try {
      const res = await kickAccount(username, 'Bị quản trị viên ngắt kết nối từ trang chi tiết')
      setSuccess(res.message || 'Đã kick user')
      fetchDetail()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi kick user')
    }
  }

  const handleLockToggle = async () => {
    try {
      if (account.status === 'locked') {
        const res = await unlockAccount(username)
        setSuccess(res.message || 'Đã mở khóa')
      } else {
        const res = await lockAccount(username)
        setSuccess(res.message || 'Đã khóa')
      }
      fetchDetail()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi thay đổi trạng thái')
    }
  }

  if (loading && !account) {
    return (
      <div className="admin-loading-state">
        <RefreshCw className="spin-icon" size={24} />
        <p>Đang tải chi tiết người dùng...</p>
      </div>
    )
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <button className="btn-secondary btn-sm" onClick={onBack}>
          <ArrowLeft size={14} /> Quay lại danh sách
        </button>
        <h2>Chi Tiết Tài Khoản: {username}</h2>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}
      {success && <div className="admin-alert-success">{success}</div>}

      {account && (
        <>
          <div className="detail-cards-grid">
            <div className="detail-card">
              <h3>Thông Tin Cơ Bản</h3>
              <div className="detail-row">
                <span className="label">User ID:</span>
                <span className="val">#{account.id}</span>
              </div>
              <div className="detail-row">
                <span className="label">Tên tài khoản:</span>
                <span className="val">
                  <strong>{account.username}</strong>
                </span>
              </div>
              <div className="detail-row">
                <span className="label">Vai trò:</span>
                <span className={`badge badge-role-${account.role}`}>{account.role}</span>
              </div>
              <div className="detail-row">
                <span className="label">Trạng thái:</span>
                <span className="val">
                  {account.deletedAt ? (
                    <span className="badge badge-danger">Đã xóa</span>
                  ) : account.status === 'locked' ? (
                    <span className="badge badge-warning">Đã khóa ({account.lockedAt})</span>
                  ) : (
                    <span className="badge badge-success">Hoạt động</span>
                  )}
                </span>
              </div>
              <div className="detail-row">
                <span className="label">Đang online:</span>
                <span className="val">
                  <span className={`status-dot ${account.isOnline ? 'online' : 'offline'}`}></span>
                  {account.isOnline ? 'Đang online' : 'Ngoại tuyến'}
                </span>
              </div>
              <div className="detail-row">
                <span className="label">Ngày tạo tài khoản:</span>
                <span className="val">{account.createdAt}</span>
              </div>

              <div className="detail-actions" style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
                {account.isOnline && (
                  <button className="btn-danger btn-sm" onClick={handleKick}>
                    <UserX size={14} /> Kick ngay
                  </button>
                )}
                <button
                  className={`btn-sm ${account.status === 'locked' ? 'btn-success' : 'btn-warning'}`}
                  onClick={handleLockToggle}
                >
                  {account.status === 'locked' ? <Unlock size={14} /> : <Lock size={14} />}
                  {account.status === 'locked' ? ' Mở khóa' : ' Khóa tài khoản'}
                </button>
              </div>
            </div>

            <div className="detail-card">
              <h3>Thống Kê Tin Nhắn</h3>
              <div className="detail-stat-row">
                <div className="detail-stat-item">
                  <span className="stat-num">{account.publicMessagesCount || 0}</span>
                  <span className="stat-desc">Tin phòng chung đã gửi</span>
                </div>
                <div className="detail-stat-item">
                  <span className="stat-num">{account.privateMessagesCount || 0}</span>
                  <span className="stat-desc">Tin riêng tư (Gửi & Nhận)</span>
                </div>
              </div>
            </div>
          </div>

          <div className="admin-table-card" style={{ marginTop: '24px' }}>
            <h3 style={{ padding: '16px 20px 0' }}>Lịch Sử Tin Nhắn Gần Nhất ({messages.length})</h3>
            {messages.length === 0 ? (
              <div className="admin-empty-state">Chưa có tin nhắn nào được ghi nhận.</div>
            ) : (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Loại</th>
                    <th>Người gửi</th>
                    <th>Người nhận</th>
                    <th>Nội dung</th>
                    <th>Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {messages.map((m) => (
                    <tr key={m.id || Math.random()}>
                      <td>
                        <span className={`badge ${m.room === 'public' ? 'badge-public' : 'badge-private'}`}>
                          {m.room === 'public' ? 'Phòng chung' : 'Tin riêng'}
                        </span>
                      </td>
                      <td>
                        <strong>{m.sender}</strong>
                      </td>
                      <td>{m.receiver || '-'}</td>
                      <td className="message-content-cell">{m.message}</td>
                      <td>{m.timestamp}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  )
}
