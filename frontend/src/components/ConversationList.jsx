import React from 'react'
import { Globe, MessageSquare, X, Lock } from 'lucide-react'

export default function ConversationList({
  activeConversation,
  onSelectConversation,
  privateSessions = [],
  unreadByConversation = {},
  onlineUsers = [],
  onEndPrivateChat
}) {
  const publicUnread = unreadByConversation['public'] || 0

  // Helper kiểm tra đối phương có đang online không
  const isPartnerOnline = (partnerName) => {
    return onlineUsers.some((u) => {
      const uName = typeof u === 'string' ? u : u?.username
      return uName && uName.toLowerCase() === partnerName.toLowerCase()
    })
  }

  return (
    <div className="conversation-list-container">
      {/* 1. Phòng chat chung */}
      <div
        className={`conversation-item ${activeConversation === 'public' ? 'active' : ''}`}
        onClick={() => onSelectConversation('public')}
      >
        <div className="conversation-item-main">
          <div className="conv-icon-avatar public">
            <Globe size={18} />
          </div>
          <div className="conv-item-info">
            <span className="conv-item-name">Phòng chung</span>
            <span className="conv-item-sub">Tất cả mọi người</span>
          </div>
        </div>

        {publicUnread > 0 && (
          <span className="conv-unread-badge" title={`${publicUnread} tin nhắn chưa đọc`}>
            {publicUnread > 99 ? '99+' : publicUnread}
          </span>
        )}
      </div>

      {/* 2. Danh sách chat riêng */}
      <div className="section-title" style={{ marginTop: 16, marginBottom: 8 }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Lock size={13} />
          <span>Chat riêng ({privateSessions.length})</span>
        </span>
      </div>

      {privateSessions.length === 0 ? (
        <div className="empty-conv-hint">
          Chưa có phiên chat riêng nào. Chọn user bên dưới để bắt đầu.
        </div>
      ) : (
        <div className="private-conv-list">
          {privateSessions.map((partner) => {
            const convKey = `private:${partner}`
            const isActive = activeConversation === convKey
            const unread = unreadByConversation[convKey] || 0
            const online = isPartnerOnline(partner)

            return (
              <div
                key={convKey}
                className={`conversation-item ${isActive ? 'active' : ''}`}
                onClick={() => onSelectConversation(convKey)}
              >
                <div className="conversation-item-main">
                  <div className="conv-avatar-wrapper">
                    <div className="conv-icon-avatar private">
                      {partner.charAt(0).toUpperCase()}
                    </div>
                    <span className={`conv-status-dot ${online ? 'online' : 'offline'}`} />
                  </div>
                  <div className="conv-item-info">
                    <span className="conv-item-name">{partner}</span>
                    <span className="conv-item-sub">
                      {online ? 'Đang trực tuyến' : 'Ngoại tuyến'}
                    </span>
                  </div>
                </div>

                <div className="conv-item-actions">
                  {unread > 0 && (
                    <span className="conv-unread-badge" title={`${unread} tin nhắn chưa đọc`}>
                      {unread > 99 ? '99+' : unread}
                    </span>
                  )}
                  <button
                    type="button"
                    className="conv-close-btn"
                    title={`Kết thúc chat riêng với ${partner}`}
                    onClick={(e) => {
                      e.stopPropagation()
                      onEndPrivateChat(partner)
                    }}
                  >
                    <X size={14} />
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
