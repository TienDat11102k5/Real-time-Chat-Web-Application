import React, { useState } from 'react'
import {
  LayoutDashboard,
  Users,
  UserCheck,
  MessageSquare,
  Clock,
  Sliders,
  FileText,
  LogOut,
  ArrowLeft,
  ShieldCheck,
  Menu,
  X
} from 'lucide-react'
import { getAdminUser, getAdminRole, clearAdminAuth } from './adminApi'
import AdminDashboard from './AdminDashboard'
import AdminUsers from './AdminUsers'
import AdminAccounts from './AdminAccounts'
import AdminUserDetail from './AdminUserDetail'
import AdminRooms from './AdminRooms'
import AdminRequests from './AdminRequests'
import AdminLimits from './AdminLimits'
import AdminLogs from './AdminLogs'

export default function AdminLayout({ onBackToChat, onLogout }) {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [selectedUserDetail, setSelectedUserDetail] = useState(null)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const adminName = getAdminUser() || 'Admin'
  const adminRole = getAdminRole() || 'admin'

  const handleLogout = () => {
    clearAdminAuth()
    if (onLogout) onLogout()
  }

  const navigateTo = (tab) => {
    setActiveTab(tab)
    setSelectedUserDetail(null)
    setMobileMenuOpen(false)
  }

  const handleViewDetail = (username) => {
    setSelectedUserDetail(username)
    setActiveTab('account-detail')
  }

  return (
    <div className="admin-layout">
      {/* Sidebar */}
      <aside className={`admin-sidebar ${mobileMenuOpen ? 'open' : ''}`}>
        <div className="admin-sidebar-header">
          <div className="admin-logo">
            <ShieldCheck size={26} color="#38bdf8" />
            <div className="admin-brand-text">
              <h3>Chat Admin</h3>
              <span>Control Panel</span>
            </div>
          </div>
          <button className="mobile-close-btn" onClick={() => setMobileMenuOpen(false)}>
            <X size={20} />
          </button>
        </div>

        <nav className="admin-nav">
          <button
            className={`admin-nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => navigateTo('dashboard')}
          >
            <LayoutDashboard size={18} />
            <span>Dashboard</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => navigateTo('users')}
          >
            <Users size={18} />
            <span>Người dùng Online</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'accounts' || activeTab === 'account-detail' ? 'active' : ''}`}
            onClick={() => navigateTo('accounts')}
          >
            <UserCheck size={18} />
            <span>Quản lý Tài khoản</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'rooms' ? 'active' : ''}`}
            onClick={() => navigateTo('rooms')}
          >
            <MessageSquare size={18} />
            <span>Phòng Chat</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'requests' ? 'active' : ''}`}
            onClick={() => navigateTo('requests')}
          >
            <Clock size={18} />
            <span>Yêu cầu Chat riêng</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'limits' ? 'active' : ''}`}
            onClick={() => navigateTo('limits')}
          >
            <Sliders size={18} />
            <span>Giới hạn Hệ thống</span>
          </button>

          <button
            className={`admin-nav-item ${activeTab === 'logs' ? 'active' : ''}`}
            onClick={() => navigateTo('logs')}
          >
            <FileText size={18} />
            <span>Nhật ký & Audit</span>
          </button>
        </nav>

        <div className="admin-sidebar-footer">
          <button className="admin-nav-item btn-back-chat" onClick={onBackToChat}>
            <ArrowLeft size={18} />
            <span>Về giao diện Chat</span>
          </button>

          <button className="admin-nav-item btn-admin-logout" onClick={handleLogout}>
            <LogOut size={18} />
            <span>Đăng xuất Admin</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="admin-main">
        <header className="admin-topbar">
          <button className="mobile-toggle-btn" onClick={() => setMobileMenuOpen(true)}>
            <Menu size={22} />
          </button>

          <div className="admin-topbar-breadcrumb">
            <span className="breadcrumb-root">Hệ thống</span> /{' '}
            <span className="breadcrumb-active">
              {activeTab === 'dashboard' && 'Tổng quan'}
              {activeTab === 'users' && 'Người dùng Online'}
              {activeTab === 'accounts' && 'Quản lý Tài khoản'}
              {activeTab === 'account-detail' && `Chi tiết: ${selectedUserDetail}`}
              {activeTab === 'rooms' && 'Phòng Chat'}
              {activeTab === 'requests' && 'Yêu cầu Chat riêng'}
              {activeTab === 'limits' && 'Giới hạn Hệ thống'}
              {activeTab === 'logs' && 'Nhật ký & Audit'}
            </span>
          </div>

          <div className="admin-profile-badge">
            <div className="admin-avatar">
              <ShieldCheck size={16} />
            </div>
            <div className="admin-name-info">
              <strong>{adminName}</strong>
              <span className={`badge badge-role-${adminRole}`}>{adminRole.toUpperCase()}</span>
            </div>
          </div>
        </header>

        <div className="admin-content-body">
          {activeTab === 'dashboard' && <AdminDashboard onNavigate={navigateTo} />}
          {activeTab === 'users' && <AdminUsers onViewDetail={handleViewDetail} />}
          {activeTab === 'accounts' && <AdminAccounts onViewDetail={handleViewDetail} />}
          {activeTab === 'account-detail' && (
            <AdminUserDetail
              username={selectedUserDetail}
              onBack={() => navigateTo('accounts')}
            />
          )}
          {activeTab === 'rooms' && <AdminRooms />}
          {activeTab === 'requests' && <AdminRequests />}
          {activeTab === 'limits' && <AdminLimits />}
          {activeTab === 'logs' && <AdminLogs />}
        </div>
      </main>
    </div>
  )
}
