import React, { useState, useEffect } from 'react'
import {
  getAdminAccounts,
  lockAccount,
  unlockAccount,
  resetPassword,
  changeRole,
  deleteAccount,
  getAdminUser
} from './adminApi'
import {
  Search,
  Lock,
  Unlock,
  Key,
  UserCheck,
  Trash2,
  Eye,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Shield,
  X
} from 'lucide-react'

export default function AdminAccounts({ onViewDetail }) {
  const [accounts, setAccounts] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Modals state
  const [resetModalUser, setResetModalUser] = useState(null)
  const [newPassword, setNewPassword] = useState('')
  const [roleModalUser, setRoleModalUser] = useState(null)
  const [selectedRole, setSelectedRole] = useState('user')
  const [deleteModalUser, setDeleteModalUser] = useState(null)

  const currentAdmin = getAdminUser()

  const fetchAccounts = async (targetPage = page, searchQuery = query) => {
    setLoading(true)
    try {
      const data = await getAdminAccounts(searchQuery, targetPage, 10)
      setAccounts(data.accounts || [])
      setTotal(data.total || 0)
      setPage(data.page || 1)
      setTotalPages(data.totalPages || 1)
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải danh sách tài khoản')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAccounts(1, query)
  }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(1)
    fetchAccounts(1, query)
  }

  const handleLockToggle = async (u) => {
    try {
      if (u.status === 'locked') {
        const res = await unlockAccount(u.username)
        setSuccess(res.message || `Đã mở khóa tài khoản ${u.username}`)
      } else {
        const res = await lockAccount(u.username)
        setSuccess(res.message || `Đã khóa tài khoản ${u.username}`)
      }
      fetchAccounts(page, query)
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi thay đổi trạng thái tài khoản')
    }
  }

  const handleResetPassword = async () => {
    if (!resetModalUser || !newPassword.trim()) return
    try {
      const res = await resetPassword(resetModalUser, newPassword.trim())
      setSuccess(res.message || `Đã đổi mật khẩu cho ${resetModalUser}`)
      setResetModalUser(null)
      setNewPassword('')
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi đặt lại mật khẩu')
    }
  }

  const handleChangeRole = async () => {
    if (!roleModalUser) return
    try {
      const res = await changeRole(roleModalUser, selectedRole)
      setSuccess(res.message || `Đã cập nhật role cho ${roleModalUser}`)
      setRoleModalUser(null)
      fetchAccounts(page, query)
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi thay đổi quyền')
    }
  }

  const handleDelete = async () => {
    if (!deleteModalUser) return
    try {
      const res = await deleteAccount(deleteModalUser)
      setSuccess(res.message || `Đã xóa tài khoản ${deleteModalUser}`)
      setDeleteModalUser(null)
      fetchAccounts(page, query)
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi xóa tài khoản')
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Quản Lý Tài Khoản ({total})</h2>
          <p className="subtitle">Tra cứu, phân quyền, khóa hoặc đặt lại mật khẩu người dùng</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={() => fetchAccounts(page, query)}>
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}
      {success && <div className="admin-alert-success">{success}</div>}

      <div className="admin-filters-bar">
        <form onSubmit={handleSearch} className="admin-search-form">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            placeholder="Tìm kiếm theo username..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button type="submit" className="btn-primary btn-sm">
            Tìm
          </button>
          {query && (
            <button
              type="button"
              className="btn-secondary btn-sm"
              onClick={() => {
                setQuery('')
                fetchAccounts(1, '')
              }}
            >
              Xóa lọc
            </button>
          )}
        </form>
      </div>

      <div className="admin-table-card">
        {loading && accounts.length === 0 ? (
          <div className="admin-empty-state">Đang tải tài khoản...</div>
        ) : accounts.length === 0 ? (
          <div className="admin-empty-state">Không tìm thấy tài khoản nào.</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Vai trò (Role)</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((u) => {
                const isDeleted = !!u.deletedAt
                const isLocked = u.status === 'locked'
                return (
                  <tr key={u.id} className={isDeleted ? 'row-deleted' : ''}>
                    <td>#{u.id}</td>
                    <td>
                      <div className="user-name-cell">
                        <span className={`status-dot ${u.isOnline ? 'online' : 'offline'}`}></span>
                        <strong>{u.username}</strong>
                        {u.username === currentAdmin && <span className="badge badge-you">Bạn</span>}
                      </div>
                    </td>
                    <td>
                      <span className={`badge badge-role-${u.role}`}>{u.role}</span>
                    </td>
                    <td>
                      {isDeleted ? (
                        <span className="badge badge-danger">Đã xóa</span>
                      ) : isLocked ? (
                        <span className="badge badge-warning">Đã khóa</span>
                      ) : (
                        <span className="badge badge-success">Hoạt động</span>
                      )}
                    </td>
                    <td>{u.createdAt || 'N/A'}</td>
                    <td>
                      <div className="table-actions">
                        <button
                          className="btn-secondary btn-xs"
                          onClick={() => onViewDetail(u.username)}
                          title="Xem chi tiết và lịch sử tin nhắn"
                        >
                          <Eye size={13} />
                        </button>

                        <button
                          className={`btn-xs ${isLocked ? 'btn-success' : 'btn-warning'}`}
                          onClick={() => handleLockToggle(u)}
                          disabled={u.username === currentAdmin || isDeleted}
                          title={isLocked ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
                        >
                          {isLocked ? <Unlock size={13} /> : <Lock size={13} />}
                        </button>

                        <button
                          className="btn-secondary btn-xs"
                          onClick={() => {
                            setResetModalUser(u.username)
                            setNewPassword('')
                          }}
                          disabled={isDeleted}
                          title="Đặt lại mật khẩu"
                        >
                          <Key size={13} />
                        </button>

                        <button
                          className="btn-secondary btn-xs"
                          onClick={() => {
                            setRoleModalUser(u.username)
                            setSelectedRole(u.role || 'user')
                          }}
                          disabled={u.username === currentAdmin || isDeleted}
                          title="Thay đổi vai trò (Role)"
                        >
                          <Shield size={13} />
                        </button>

                        <button
                          className="btn-danger btn-xs"
                          onClick={() => setDeleteModalUser(u.username)}
                          disabled={u.username === currentAdmin || isDeleted}
                          title="Xóa tài khoản (Soft delete)"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="admin-pagination">
            <button
              className="btn-secondary btn-sm"
              disabled={page <= 1}
              onClick={() => fetchAccounts(page - 1, query)}
            >
              <ChevronLeft size={14} /> Trước
            </button>
            <span className="pagination-info">
              Trang {page} / {totalPages}
            </span>
            <button
              className="btn-secondary btn-sm"
              disabled={page >= totalPages}
              onClick={() => fetchAccounts(page + 1, query)}
            >
              Sau <ChevronRight size={14} />
            </button>
          </div>
        )}
      </div>

      {/* Modal Reset Password */}
      {resetModalUser && (
        <div className="modal-backdrop" onClick={() => setResetModalUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Đặt lại mật khẩu</h3>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setResetModalUser(null)}
                title="Đóng"
              >
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <p>
                Đặt lại mật khẩu mới cho tài khoản <strong>{resetModalUser}</strong>:
              </p>
              <div className="form-group">
                <label>Mật khẩu mới (ít nhất 6 ký tự):</label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Nhập mật khẩu mới..."
                  autoFocus
                />
              </div>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setResetModalUser(null)}
              >
                Hủy
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleResetPassword}
                disabled={!newPassword || newPassword.length < 6}
              >
                Lưu mật khẩu mới
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Change Role */}
      {roleModalUser && (
        <div className="modal-backdrop" onClick={() => setRoleModalUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Thay đổi vai trò</h3>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setRoleModalUser(null)}
                title="Đóng"
              >
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <p>
                Cập nhật quyền hạn cho tài khoản <strong>{roleModalUser}</strong>:
              </p>
              <div className="form-group">
                <label>Chọn quyền hạn:</label>
                <select
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value)}
                  className="admin-select"
                  style={{ width: '100%', padding: '10px 14px' }}
                >
                  <option value="user">User (Người dùng chat bình thường)</option>
                  <option value="moderator">Moderator (Điều hành viên - Kick/Cancel request)</option>
                  <option value="admin">Admin (Toàn quyền quản trị hệ thống)</option>
                </select>
              </div>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setRoleModalUser(null)}
              >
                Hủy
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleChangeRole}
              >
                Cập nhật vai trò
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Delete Confirmation */}
      {deleteModalUser && (
        <div className="modal-backdrop" onClick={() => setDeleteModalUser(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Xác nhận xóa tài khoản</h3>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setDeleteModalUser(null)}
                title="Đóng"
              >
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <p>
                Bạn có chắc chắn muốn xóa tài khoản <strong>{deleteModalUser}</strong>?
              </p>
              <p style={{ marginTop: '10px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Hệ thống sẽ thực hiện Soft-delete: ngắt kết nối WebSocket ngay lập tức và chặn không cho đăng nhập lại.
              </p>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setDeleteModalUser(null)}
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
              >
                Xóa tài khoản
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
