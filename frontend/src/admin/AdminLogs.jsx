import React, { useState, useEffect } from 'react'
import { getAdminLogs } from './adminApi'
import { ListFilter, RefreshCw, CheckCircle2, XCircle, FileText } from 'lucide-react'

export default function AdminLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filterType, setFilterType] = useState('')
  const [autoRefresh, setAutoRefresh] = useState(true)

  const fetchLogs = async () => {
    try {
      const data = await getAdminLogs(100, filterType)
      setLogs(data || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải nhật ký hệ thống')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLogs()
  }, [filterType])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = setInterval(fetchLogs, 5000)
    return () => clearInterval(timer)
  }, [autoRefresh, filterType])

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Nhật Ký Quản Trị & Audit Log ({logs.length})</h2>
          <p className="subtitle">Theo dõi lịch sử các thao tác bảo mật, quản trị viên và sự kiện hệ thống</p>
        </div>
        <div className="header-controls">
          <label className="auto-refresh-toggle">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            <span>Tự động làm mới (5s)</span>
          </label>
          <button className="btn-secondary btn-sm" onClick={fetchLogs}>
            <RefreshCw size={14} /> Làm mới
          </button>
        </div>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}

      <div className="admin-filters-bar">
        <div className="filter-group">
          <ListFilter size={16} />
          <label>Lọc thao tác:</label>
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="admin-select"
          >
            <option value="">Tất cả sự kiện</option>
            <option value="ADMIN_LOGIN">Đăng nhập Admin (ADMIN_LOGIN)</option>
            <option value="DISCONNECT_USER">Ngắt kết nối (DISCONNECT_USER)</option>
            <option value="LOCK_USER">Khóa tài khoản (LOCK_USER)</option>
            <option value="UNLOCK_USER">Mở khóa tài khoản (UNLOCK_USER)</option>
            <option value="RESET_PASSWORD">Đặt lại mật khẩu (RESET_PASSWORD)</option>
            <option value="CHANGE_ROLE">Đổi quyền hạn (CHANGE_ROLE)</option>
            <option value="DELETE_USER">Xóa người dùng (DELETE_USER)</option>
            <option value="CANCEL_REQUEST">Hủy yêu cầu chat (CANCEL_REQUEST)</option>
          </select>
        </div>
      </div>

      <div className="admin-table-card">
        {loading && logs.length === 0 ? (
          <div className="admin-empty-state">Đang tải nhật ký...</div>
        ) : logs.length === 0 ? (
          <div className="admin-empty-state">Không có bản ghi nhật ký nào.</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Người thực hiện</th>
                <th>Hành động</th>
                <th>Đối tượng</th>
                <th>Kết quả</th>
                <th>Chi tiết</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{log.timestamp}</td>
                  <td>
                    <strong>{log.adminUsername}</strong>
                  </td>
                  <td>
                    <span className="badge badge-action">{log.action}</span>
                  </td>
                  <td>
                    <code>{log.target}</code>
                  </td>
                  <td>
                    {log.result === 'SUCCESS' ? (
                      <span className="badge badge-success">
                        <CheckCircle2 size={12} style={{ marginRight: '4px' }} />
                        Thành công
                      </span>
                    ) : (
                      <span className="badge badge-danger">
                        <XCircle size={12} style={{ marginRight: '4px' }} />
                        Thất bại
                      </span>
                    )}
                  </td>
                  <td className="log-details-cell">{log.details || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
