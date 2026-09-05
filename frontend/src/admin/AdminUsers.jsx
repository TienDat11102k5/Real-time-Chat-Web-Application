import React, { useState, useEffect } from 'react'
import { getAdminUsers, disconnectUser } from './adminApi'
import { Users, UserX, RefreshCw, Radio, Eye, X } from 'lucide-react'

export default function AdminUsers({ onViewDetail }) {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [disconnectingUser, setDisconnectingUser] = useState(null)
  const [reason, setReason] = useState('Vi phạm quy chế phòng chat')

  const fetchUsers = async () => {
    try {
      const data = await getAdminUsers()
      setUsers(data || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải danh sách người dùng online')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
    const timer = setInterval(fetchUsers, 3000)
    return () => clearInterval(timer)
  }, [])

  const handleDisconnect = async () => {
    if (!disconnectingUser) return
    try {
      const res = await disconnectUser(disconnectingUser, reason)
      setSuccess(res.message || `Đã ngắt kết nối ${disconnectingUser}`)
      setDisconnectingUser(null)
      fetchUsers()
      setTimeout(() => setSuccess(''), 4000)
    } catch (err) {
      setError(err.message || 'Lỗi ngắt kết nối user')
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Người Dùng Đang Online ({users.length})</h2>
          <p className="subtitle">Giám sát các phiên kết nối WebSocket đang hoạt động</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={fetchUsers}>
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}
      {success && <div className="admin-alert-success">{success}</div>}

      <div className="admin-table-card">
        {loading && users.length === 0 ? (
          <div className="admin-empty-state">Đang tải danh sách kết nối...</div>
        ) : users.length === 0 ? (
          <div className="admin-empty-state">Hiện không có người dùng nào đang kết nối.</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Tài khoản</th>
                <th>Trạng thái phòng</th>
                <th>Địa chỉ IP / Port</th>
                <th>Thời điểm kết nối</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.username}>
                  <td>
                    <div className="user-name-cell">
                      <span className="status-dot online"></span>
                      <strong>{u.username}</strong>
                    </div>
                  </td>
                  <td>
                    {u.roomType === 'public' ? (
                      <span className="badge badge-public">Phòng chung</span>
                    ) : (
                      <span className="badge badge-private">Chat riêng với {u.roomTarget}</span>
                    )}
                  </td>
                  <td>
                    <code>{u.remoteAddress || 'unknown'}</code>
                  </td>
                  <td>{u.connectedAt}</td>
                  <td>
                    <div className="table-actions">
                      <button
                        className="btn-danger btn-xs"
                        onClick={() => setDisconnectingUser(u.username)}
                        title="Ngắt kết nối WebSocket của user này"
                      >
                        <UserX size={13} /> Kick
                      </button>
                      {onViewDetail && (
                        <button
                          className="btn-secondary btn-xs"
                          onClick={() => onViewDetail(u.username)}
                          title="Xem chi tiết tài khoản"
                        >
                          <Eye size={13} /> Xem
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal xác nhận ngắt kết nối */}
      {disconnectingUser && (
        <div className="modal-backdrop" onClick={() => setDisconnectingUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Ngắt kết nối người dùng</h3>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setDisconnectingUser(null)}
                title="Đóng"
              >
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <p>
                Bạn có chắc chắn muốn ngắt kết nối phiên làm việc của user <strong>{disconnectingUser}</strong>?
              </p>
              <div className="form-group" style={{ marginTop: '14px' }}>
                <label>Lý do ngắt kết nối gửi tới client:</label>
                <input
                  type="text"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Nhập lý do gửi tới client..."
                  autoFocus
                />
              </div>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setDisconnectingUser(null)}
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDisconnect}
              >
                Xác nhận Kick
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
