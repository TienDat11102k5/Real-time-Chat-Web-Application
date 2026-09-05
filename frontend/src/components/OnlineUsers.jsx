import React from 'react'
import { MessageCircle, MessageSquare } from 'lucide-react'

export default function OnlineUsers({
  onlineUsers = [],
  currentUser,
  currentPartner,
  privateSessions = [],
  onSelectConversation,
  onRequestPrivate
}) {
  return (
    <ul className="online-list">
      {onlineUsers.length === 0 ? (
        <li style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
          Không có người dùng nào khác online
        </li>
      ) : (
        onlineUsers.map((user, idx) => {
          // Chuẩn hóa user whether it's a string ("dat") or an object ({ username: "dat", roomType: ... })
          const username = typeof user === 'string' ? user : (user?.username || '')
          const roomType = typeof user === 'object' ? (user?.roomType || 'public') : 'public'
          const isSelf = username.toLowerCase() === (currentUser || '').toLowerCase()
          const isCurrentViewingPartner = currentPartner && username.toLowerCase() === currentPartner.toLowerCase()
          const hasActiveSession = privateSessions.some((p) => p.toLowerCase() === username.toLowerCase())
          const isInPrivate = roomType === 'private' || hasActiveSession

          let itemClass = 'online-item'
          if (isCurrentViewingPartner) itemClass += ' active-partner'

          return (
            <li key={username || idx} className={itemClass}>
              <div className="online-user-meta">
                <div className="status-dot" />
                <div>
                  <div style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-main)' }}>
                    {username} {isSelf && <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(Bạn)</span>}
                  </div>
                  <span
                    className={`online-status-badge ${
                      isInPrivate ? 'badge-private' : 'badge-public'
                    }`}
                  >
                    {isInPrivate ? 'Đang chat riêng' : 'Phòng chung'}
                  </span>
                </div>
              </div>

              {!isSelf && (
                <button
                  type="button"
                  className="chat-req-btn"
                  style={{
                    backgroundColor: hasActiveSession ? 'rgba(59, 130, 246, 0.25)' : 'var(--primary)',
                    color: hasActiveSession ? '#93c5fd' : '#ffffff',
                    border: hasActiveSession ? '1px solid rgba(59, 130, 246, 0.4)' : 'none'
                  }}
                  onClick={() => onRequestPrivate ? onRequestPrivate(username) : (onSelectConversation && onSelectConversation(`private:${username}`))}
                  title={`Nhắn tin riêng với ${username}`}
                >
                  <MessageCircle size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} />
                  {hasActiveSession ? 'Mở chat' : 'Nhắn tin'}
                </button>
              )}
            </li>
          )
        })
      )}
    </ul>
  )
}
