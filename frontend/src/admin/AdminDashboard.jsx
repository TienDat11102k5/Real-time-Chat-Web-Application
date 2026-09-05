import React, { useState, useEffect } from 'react'
import { getAdminStats } from './adminApi'
import { Users, Shield, MessageSquare, Clock, Database, UserCheck, Activity, RefreshCw } from 'lucide-react'

export default function AdminDashboard({ onNavigate }) {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchStats = async () => {
    try {
      const data = await getAdminStats()
      setStats(data)
      setError('')
    } catch (err) {
      setError(err.message || 'Không thể tải thống kê')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStats()
    const timer = setInterval(fetchStats, 5000)
    return () => clearInterval(timer)
  }, [])

  const formatUptime = (seconds) => {
    if (!seconds) return '0 giây'
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const s = seconds % 60
    let res = ''
    if (h > 0) res += `${h} giờ `
    if (m > 0 || h > 0) res += `${m} phút `
    res += `${s} giây`
    return res
  }

  if (loading && !stats) {
    return (
      <div className="admin-loading-state">
        <RefreshCw className="spin-icon" size={28} />
        <p>Đang tải dữ liệu Dashboard...</p>
      </div>
    )
  }

  return (
    <div className="admin-dashboard">
      <div className="dashboard-header-row">
        <div>
          <h2>Tổng Quan Hệ Thống</h2>
          <p className="subtitle">Giám sát trạng thái máy chủ chat và hoạt động người dùng theo thời gian thực</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={fetchStats} title="Làm mới ngay">
          <RefreshCw size={14} /> Làm mới
        </button>
      </div>

      {error && <div className="admin-alert-error">{error}</div>}

      {stats && (
        <>
          <div className="stats-grid">
            <div className="stat-card" onClick={() => onNavigate('users')}>
              <div className="stat-icon-wrapper bg-blue">
                <Users size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">
                  {stats.online_users} / {stats.max_clients}
                </div>
                <div className="stat-label">Clients Đang Online</div>
              </div>
            </div>

            <div className="stat-card" onClick={() => onNavigate('rooms')}>
              <div className="stat-icon-wrapper bg-green">
                <MessageSquare size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">{stats.public_room_users}</div>
                <div className="stat-label">User Trong Phòng Chung</div>
              </div>
            </div>

            <div className="stat-card" onClick={() => onNavigate('rooms')}>
              <div className="stat-icon-wrapper bg-purple">
                <Activity size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">{stats.private_pairs}</div>
                <div className="stat-label">Cặp Chat Riêng Tư</div>
              </div>
            </div>

            <div className="stat-card" onClick={() => onNavigate('requests')}>
              <div className="stat-icon-wrapper bg-amber">
                <Clock size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">{stats.pending_requests}</div>
                <div className="stat-label">Yêu Cầu Chat Đang Chờ</div>
              </div>
            </div>

            <div className="stat-card" onClick={() => onNavigate('accounts')}>
              <div className="stat-icon-wrapper bg-cyan">
                <UserCheck size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">{stats.total_accounts}</div>
                <div className="stat-label">Tổng Tài Khoản Đăng Ký</div>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper bg-indigo">
                <Database size={24} />
              </div>
              <div className="stat-info">
                <div className="stat-value">{stats.public_messages_count + stats.private_messages_count}</div>
                <div className="stat-label">Tổng Tin Nhắn Lưu Trữ</div>
              </div>
            </div>
          </div>

          <div className="server-status-panel">
            <h3>Thông Số Vận Hành Máy Chủ</h3>
            <div className="status-items-grid">
              <div className="status-item">
                <span className="status-item-label">Cơ sở dữ liệu (SQLite):</span>
                <span className="badge badge-success">WAL Mode • {stats.database}</span>
              </div>
              <div className="status-item">
                <span className="status-item-label">Thời gian hoạt động (Uptime):</span>
                <span className="status-item-value">{formatUptime(stats.uptime_seconds)}</span>
              </div>
              <div className="status-item">
                <span className="status-item-label">Giới hạn tải (Max Clients):</span>
                <span className="status-item-value">{stats.max_clients} kết nối đồng thời</span>
              </div>
              <div className="status-item">
                <span className="status-item-label">Phòng công cộng:</span>
                <span className="status-item-value">{stats.public_room_users} người đang kết nối</span>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
