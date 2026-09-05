import React, { useState, useEffect } from 'react'
import { getAdminRooms } from './adminApi'
import { MessageSquare, Users, Lock, RefreshCw, ArrowRightLeft } from 'lucide-react'

export default function AdminRooms() {
  const [rooms, setRooms] = useState({ public: [], private_pairs: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchRooms = async () => {
    try {
      const data = await getAdminRooms()
      setRooms(data || { public: [], private_pairs: [] })
      setError('')
    } catch (err) {
      setError(err.message || 'Lỗi tải danh sách phòng')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRooms()
    const timer = setInterval(fetchRooms, 3000)
    return () => clearInterval(timer)
  }, [])

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Quản Lý Phòng Chat</h2>
          <p className="subtitle">Xem danh sách người dùng trong phòng chung và các cặp đang chat riêng tư</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={fetchRooms}>
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}

      <div className="rooms-grid">
        <div className="room-panel">
          <div className="room-panel-header">
            <div className="room-title">
              <Users size={20} color="#38bdf8" />
              <h3>Phòng Công Cộng (Public Room)</h3>
            </div>
            <span className="badge badge-public">{rooms.public?.length || 0} người</span>
          </div>

          <div className="room-users-list">
            {!rooms.public || rooms.public.length === 0 ? (
              <div className="admin-empty-state">Hiện không có ai trong phòng chung.</div>
            ) : (
              <div className="user-chips-container">
                {rooms.public.map((u) => (
                  <div key={u} className="user-chip">
                    <span className="status-dot online"></span>
                    <span>{u}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="room-panel">
          <div className="room-panel-header">
            <div className="room-title">
              <Lock size={20} color="#c084fc" />
              <h3>Các Cặp Đang Chat Riêng Tư (Private Pairs)</h3>
            </div>
            <span className="badge badge-private">{rooms.private_pairs?.length || 0} cặp</span>
          </div>

          <div className="room-pairs-list">
            {!rooms.private_pairs || rooms.private_pairs.length === 0 ? (
              <div className="admin-empty-state">Hiện không có cặp nào đang chat riêng.</div>
            ) : (
              <div className="pairs-container">
                {rooms.private_pairs.map((pair, idx) => (
                  <div key={idx} className="pair-card">
                    <span className="pair-user">
                      <span className="status-dot online"></span>
                      <strong>{pair[0]}</strong>
                    </span>
                    <ArrowRightLeft size={16} className="pair-arrow" />
                    <span className="pair-user">
                      <span className="status-dot online"></span>
                      <strong>{pair[1]}</strong>
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
