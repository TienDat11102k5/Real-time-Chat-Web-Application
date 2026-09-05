import React from 'react'
import { KeyRound, LogOut, Users, MessageSquareText } from 'lucide-react'
import ConversationList from './ConversationList'
import OnlineUsers from './OnlineUsers'

export default function Sidebar({
  currentUser,
  isConnected,
  onlineUsers = [],
  activeConversation = 'public',
  onSelectConversation,
  privateSessions = [],
  unreadByConversation = {},
  onEndPrivateChat,
  onOpenChangePassword,
  onLogout,
  onRequestPrivate
}) {
  return (
    <aside className="chat-sidebar">
      {/* Header thông tin cá nhân */}
      <div className="sidebar-header">
        <div className="user-profile">
          <div className="avatar-badge">
            {currentUser ? currentUser.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="user-info">
            <span className="user-name">{currentUser}</span>
            <span className="user-status">
              <span className={`status-dot ${isConnected ? '' : 'offline'}`} />
              {isConnected ? 'Đang trực tuyến' : 'Mất kết nối'}
            </span>
          </div>
        </div>

        <div className="sidebar-actions">
          <button
            type="button"
            className="icon-btn"
            onClick={onOpenChangePassword}
            title="Đổi mật khẩu"
          >
            <KeyRound size={16} />
          </button>
          <button
            type="button"
            className="icon-btn"
            onClick={onLogout}
            title="Đăng xuất"
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>

      {/* Danh sách các cuộc trò chuyện (Phòng chung + Các tab Chat riêng) */}
      <div className="sidebar-section conversations-section">
        <div className="section-title">
          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <MessageSquareText size={14} />
            <span>Cuộc trò chuyện</span>
          </span>
        </div>

        <ConversationList
          activeConversation={activeConversation}
          onSelectConversation={onSelectConversation}
          privateSessions={privateSessions}
          unreadByConversation={unreadByConversation}
          onlineUsers={onlineUsers}
          onEndPrivateChat={onEndPrivateChat}
        />
      </div>

      {/* Danh sách người dùng online */}
      <div className="sidebar-section users-section">
        <div className="section-title">
          <span>Người dùng online</span>
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <Users size={14} />
            {onlineUsers.length}
          </span>
        </div>

        <OnlineUsers
          onlineUsers={onlineUsers}
          currentUser={currentUser}
          currentPartner={activeConversation.startsWith('private:') ? activeConversation.replace('private:', '') : null}
          privateSessions={privateSessions}
          onSelectConversation={onSelectConversation}
          onRequestPrivate={onRequestPrivate}
        />
      </div>
    </aside>
  )
}
