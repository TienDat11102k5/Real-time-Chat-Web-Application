import React, { useState, useEffect } from 'react'
import { getAdminRequests, cancelPrivateRequest } from './adminApi'
import { Clock, XCircle, RefreshCw, ArrowRight } from 'lucide-react'

export default function AdminRequests() {
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const fetchRequests = async () => {
    try {
      const data = await getAdminRequests()
      setRequests(data || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải danh sách yêu cầu')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRequests()
    const timer = setInterval(fetchRequests, 2000)
    return () => clearInterval(timer)
  }, [])

  const handleCancel = async (sender, receiver) => {
    try {
      const res = await cancelPrivateRequest(sender, receiver)
      setSuccess(res.message || `Đã hủy yêu cầu giữa ${sender} và ${receiver}`)
      fetchRequests()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      setError(err.message || 'Lỗi khi hủy yêu cầu')
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Yêu Cầu Chat Riêng Đang Chờ ({requests.length})</h2>
          <p className="subtitle">Giám sát các lời mời chat riêng tư kèm đếm ngược thời gian chờ (60s)</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={fetchRequests}>
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}
      {success && <div className="admin-alert-success">{success}</div>}

      <div className="admin-table-card">
        {loading && requests.length === 0 ? (
          <div className="admin-empty-state">Đang kiểm tra yêu cầu...</div>
        ) : requests.length === 0 ? (
          <div className="admin-empty-state">Hiện không có yêu cầu chat riêng nào đang chờ xử lý.</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Người gửi</th>
                <th>Người nhận</th>
                <th>Lời nhắn xem trước</th>
                <th>Thời gian còn lại</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((r) => (
                <tr key={`${r.sender}->${r.receiver}`}>
                  <td>
                    <strong>{r.sender}</strong>
                  </td>
                  <td>
                    <strong>{r.receiver}</strong>
                  </td>
                  <td className="message-content-cell">{r.preview || '(Không có lời nhắn)'}</td>
                  <td>
                    <span className="badge badge-warning">
                      <Clock size={12} style={{ marginRight: '4px' }} />
                      {r.remainingSeconds}s
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn-danger btn-xs"
                      onClick={() => handleCancel(r.sender, r.receiver)}
                      title="Hủy yêu cầu chat riêng này"
                    >
                      <XCircle size={13} /> Hủy yêu cầu
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
