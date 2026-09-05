import React, { useState, useEffect, useCallback, useRef } from 'react'
import { messageApi, userApi } from '../api/http'
import { socket } from '../api/socket'
import Sidebar from './Sidebar'
import MessageList from './MessageList'
import MessageInput from './MessageInput'
import ChangePasswordModal from './ChangePasswordModal'
import Toast from './Toast'
import { ArrowLeft, MessageSquare, Lock, Globe, WifiOff, RefreshCw, XCircle } from 'lucide-react'

export default function ChatLayout({ currentUser, onLogout }) {
  const [connectionState, setConnectionState] = useState('connecting') // 'connected' | 'connecting' | 'disconnected'
  const [activeConversation, setActiveConversation] = useState(() => {
    try {
      const saved = localStorage.getItem(`chat_active_conversation_${currentUser}`)
      return saved || 'public'
    } catch {
      return 'public'
    }
  })
  const [privateSessions, setPrivateSessions] = useState(() => {
    try {
      const saved = localStorage.getItem(`chat_private_sessions_${currentUser}`)
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })
  const [messagesByConversation, setMessagesByConversation] = useState({ public: [] })
  const [unreadByConversation, setUnreadByConversation] = useState({ public: 0 })
  const [onlineUsers, setOnlineUsers] = useState([])
  const [isChangePassOpen, setIsChangePassOpen] = useState(false)
  const [toast, setToast] = useState(null) // { message, type }
  const [isSidebarOpenMobile, setIsSidebarOpenMobile] = useState(false)

  // Đồng bộ activeConversation vào localStorage khi thay đổi
  useEffect(() => {
    if (currentUser) {
      try {
        localStorage.setItem(`chat_active_conversation_${currentUser}`, activeConversation)
      } catch (e) {
        console.error('Lỗi lưu activeConversation vào localStorage:', e)
      }
    }
  }, [currentUser, activeConversation])

  // Đồng bộ privateSessions vào localStorage khi thay đổi
  useEffect(() => {
    if (currentUser) {
      try {
        localStorage.setItem(`chat_private_sessions_${currentUser}`, JSON.stringify(privateSessions))
      } catch (e) {
        console.error('Lỗi lưu privateSessions vào localStorage:', e)
      }
    }
  }, [currentUser, privateSessions])

  // Ref để theo dõi activeConversation hiện tại trong WebSocket callback
  const activeConversationRef = useRef(activeConversation)
  useEffect(() => {
    activeConversationRef.current = activeConversation
  }, [activeConversation])

  // Tải danh sách online
  const fetchOnlineUsers = useCallback(async () => {
    try {
      const res = await userApi.getOnlineUsers()
      if (res.ok && Array.isArray(res.users)) {
        setOnlineUsers(res.users)
      }
    } catch (e) {
      console.error('Lỗi lấy danh sách online:', e)
    }
  }, [])

  // Tải lịch sử phòng chung
  const loadPublicHistory = useCallback(async () => {
    try {
      const history = await messageApi.getPublicHistory(50)
      setMessagesByConversation((prev) => ({
        ...prev,
        public: history || []
      }))
    } catch (e) {
      console.error('Lỗi tải lịch sử phòng chung:', e)
    }
  }, [])

  // Tải lịch sử chat riêng với partner
  const loadPrivateHistory = useCallback(async (partner) => {
    if (!partner) return
    const convKey = `private:${partner}`
    try {
      const history = await messageApi.getPrivateHistory(partner, 50)
      setMessagesByConversation((prev) => ({
        ...prev,
        [convKey]: history || []
      }))
    } catch (e) {
      console.error(`Lỗi tải lịch sử chat riêng với ${partner}:`, e)
    }
  }, [])

  // Chuyển cuộc trò chuyện (chỉ đổi UI view, không ngắt kết nối hay hủy session)
  const handleSelectConversation = useCallback((convKey) => {
    setActiveConversation(convKey)
    // Đọc conversation này -> reset unread về 0
    setUnreadByConversation((prev) => ({
      ...prev,
      [convKey]: 0
    }))

    if (convKey.startsWith('private:')) {
      const partner = convKey.replace('private:', '')
      loadPrivateHistory(partner)
    }
    setIsSidebarOpenMobile(false)
  }, [loadPrivateHistory])

  // Kết thúc phiên chat riêng
  const handleEndPrivateChat = useCallback((partner) => {
    socket.endPrivateChat(partner)
    setPrivateSessions((prev) => {
      const updated = prev.filter((p) => p.toLowerCase() !== partner.toLowerCase())
      try {
        localStorage.setItem(`chat_private_sessions_${currentUser}`, JSON.stringify(updated))
      } catch (e) {}
      return updated
    })
    if (activeConversationRef.current === `private:${partner}`) {
      setActiveConversation('public')
      try {
        localStorage.setItem(`chat_active_conversation_${currentUser}`, 'public')
      } catch (e) {}
      setUnreadByConversation((prev) => ({ ...prev, public: 0 }))
    }
  }, [currentUser])

  // Dọn dẹp lưu trữ và ngắt kết nối khi đăng xuất
  const handleLogoutWithCleanup = useCallback(() => {
    try {
      localStorage.removeItem(`chat_private_sessions_${currentUser}`)
      localStorage.removeItem(`chat_active_conversation_${currentUser}`)
    } catch (e) {}
    socket.disconnect()
    if (onLogout) onLogout()
  }, [currentUser, onLogout])

  // Kết nối và đăng ký socket events
  useEffect(() => {
    socket.connect()
    const offStatus = socket.onStatusChange((status) => {
      setConnectionState(status)
    })

    fetchOnlineUsers()
    loadPublicHistory()

    // Khôi phục lịch sử chat riêng cho các tab đã lưu
    privateSessions.forEach((p) => {
      loadPrivateHistory(p)
    })
    if (activeConversation.startsWith('private:')) {
      const partner = activeConversation.replace('private:', '')
      if (partner) {
        loadPrivateHistory(partner)
      }
    }

    // 1. Nhận tin nhắn (public hoặc private)
    const offMsg = socket.on('message', (msg) => {
      if (msg.room === 'public') {
        setMessagesByConversation((prev) => ({
          ...prev,
          public: [...(prev.public || []), msg]
        }))

        // Nếu user hiện không xem phòng chung thì tăng unread
        if (activeConversationRef.current !== 'public') {
          setUnreadByConversation((prev) => ({
            ...prev,
            public: (prev.public || 0) + 1
          }))
        }
      } else if (msg.room === 'private') {
        const partner = (msg.sender === currentUser) ? msg.receiver : msg.sender
        if (partner) {
          const convKey = `private:${partner}`

          // Tự động thêm vào privateSessions nếu chưa có
          setPrivateSessions((prev) => {
            const exists = prev.some((p) => p.toLowerCase() === partner.toLowerCase())
            return exists ? prev : [...prev, partner]
          })

          setMessagesByConversation((prev) => ({
            ...prev,
            [convKey]: [...(prev[convKey] || []), msg]
          }))

          // Nếu user hiện không xem cuộc trò chuyện này thì tăng unread
          if (activeConversationRef.current !== convKey) {
            setUnreadByConversation((prev) => ({
              ...prev,
              [convKey]: (prev[convKey] || 0) + 1
            }))
          }
        }
      }
    })

    // 2. Nhận tin nhắn hệ thống
    const offSys = socket.on('system', (sys) => {
      const text = sys.message || ''
      setToast({ message: text, type: 'info' })

      const lower = text.toLowerCase()
      const isPrivateNotice =
        lower.includes('chat riêng') ||
        lower.includes('yêu cầu tới') ||
        lower.includes('từ chối') ||
        lower.includes('kết thúc phiên')

      // TUYỆT ĐỐI KHÔNG thêm thông báo chat riêng vào lịch sử tin nhắn phòng chung
      if (!isPrivateNotice) {
        setMessagesByConversation((prev) => ({
          ...prev,
          public: [...(prev.public || []), { type: 'system', message: text }]
        }))
      }
    })

    // 3. Nhận thông báo lỗi
    const offErr = socket.on('error', (err) => {
      setToast({ message: err.message, type: 'error' })
    })

    // 4. Cập nhật danh sách online
    const offOnline = socket.on('online_users', (data) => {
      if (data && Array.isArray(data.users)) {
        setOnlineUsers(data.users)
      } else {
        fetchOnlineUsers()
      }
    })

    // 5. Phiên chat riêng bắt đầu
    const offPrivStarted = socket.on('private_session_started', (data) => {
      const partner = data.with
      if (partner) {
        setPrivateSessions((prev) => {
          const exists = prev.some((p) => p.toLowerCase() === partner.toLowerCase())
          return exists ? prev : [...prev, partner]
        })
        const convKey = `private:${partner}`
        loadPrivateHistory(partner)

        // Nếu là phiên vừa được chấp nhận (không phải restored sau F5) thì tự động chuyển tab
        if (!data.restored) {
          setActiveConversation(convKey)
          setUnreadByConversation((prev) => ({ ...prev, [convKey]: 0 }))
          setToast({ message: `Đã mở chat riêng với ${partner}`, type: 'success' })
        }
      }
    })

    // 6. Phiên chat riêng kết thúc
    const offPrivEnded = socket.on('private_session_ended', (data) => {
      const partner = data.with
      if (partner) {
        setPrivateSessions((prev) => {
          const updated = prev.filter((p) => p.toLowerCase() !== partner.toLowerCase())
          try {
            localStorage.setItem(`chat_private_sessions_${currentUser}`, JSON.stringify(updated))
          } catch (e) {}
          return updated
        })
        if (activeConversationRef.current === `private:${partner}`) {
          setActiveConversation('public')
          setUnreadByConversation((prev) => ({ ...prev, public: 0 }))
        }
        const reason = data.reason ? ` (${data.reason})` : ''
        setToast({ message: `Phiên chat riêng với ${partner} đã kết thúc${reason}`, type: 'info' })
      }
    })

    // 7. Hỗ trợ tương thích ngược cho room_state
    const offRoomState = socket.on('room_state', (data) => {
      if (data.room === 'private' && data.target) {
        const partner = data.target
        setPrivateSessions((prev) => {
          const exists = prev.some((p) => p.toLowerCase() === partner.toLowerCase())
          return exists ? prev : [...prev, partner]
        })
        const convKey = `private:${partner}`
        setActiveConversation(convKey)
        setUnreadByConversation((prev) => ({ ...prev, [convKey]: 0 }))
        loadPrivateHistory(partner)
      }
    })

    // 8. Tự động thêm vào tab chat riêng nếu nhận event
    const offPrivReq = socket.on('private_request', (req) => {
      const partner = req?.from || req?.sender
      if (partner) {
        setPrivateSessions((prev) => {
          const exists = prev.some((p) => p.toLowerCase() === partner.toLowerCase())
          return exists ? prev : [...prev, partner]
        })
        loadPrivateHistory(partner)
      }
    })

    // 9. Bị quản trị viên kick
    const offKicked = socket.on('kicked', (data) => {
      alert(data.reason || 'Bạn đã bị quản trị viên ngắt kết nối')
      onLogout()
    })

    return () => {
      offStatus()
      offMsg()
      offSys()
      offErr()
      offOnline()
      offPrivStarted()
      offPrivEnded()
      offRoomState()
      offPrivReq()
      offKicked()
      socket.disconnect()
    }
  }, [currentUser, fetchOnlineUsers, loadPublicHistory, loadPrivateHistory, onLogout])

  // Gửi tin nhắn
  const handleSendMessage = (text) => {
    if (activeConversation === 'public') {
      socket.sendPublicMessage(text)
    } else if (activeConversation.startsWith('private:')) {
      const partner = activeConversation.replace('private:', '')
      socket.sendPrivateMessage(partner, text)
    }
  }

  // Mở chat riêng trực tiếp với người dùng (không cần gửi yêu cầu hay chờ đồng ý)
  const handleStartDirectPrivateChat = useCallback((target) => {
    if (!target) return
    const convKey = `private:${target}`

    setPrivateSessions((prev) => {
      const exists = prev.some((p) => p.toLowerCase() === target.toLowerCase())
      return exists ? prev : [...prev, target]
    })

    setActiveConversation(convKey)
    setUnreadByConversation((prev) => ({ ...prev, [convKey]: 0 }))
    loadPrivateHistory(target)

    socket.startPrivateChat(target)
  }, [loadPrivateHistory])

  const isConnected = connectionState === 'connected'
  const isPublic = activeConversation === 'public'
  const activePartner = !isPublic ? activeConversation.replace('private:', '') : null
  const currentMessages = messagesByConversation[activeConversation] || []

  return (
    <div className={`chat-layout ${isSidebarOpenMobile ? 'sidebar-open' : ''}`}>
      {/* Toast thông báo */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* Sidebar bên trái với Danh sách cuộc trò chuyện và Danh sách Online */}
      <Sidebar
        currentUser={currentUser}
        isConnected={isConnected}
        onlineUsers={onlineUsers}
        activeConversation={activeConversation}
        onSelectConversation={handleSelectConversation}
        privateSessions={privateSessions}
        unreadByConversation={unreadByConversation}
        onEndPrivateChat={handleEndPrivateChat}
        onOpenChangePassword={() => setIsChangePassOpen(true)}
        onLogout={handleLogoutWithCleanup}
        onRequestPrivate={handleStartDirectPrivateChat}
      />

      {/* Khung chat chính */}
      <main className="chat-main">
        {/* Thanh tiêu đề Topbar */}
        <div className="chat-topbar">
          <div className="room-title-area">
            {isPublic ? (
              <>
                <Globe size={20} color="var(--primary)" />
                <span className="room-name">Phòng Chat Chung</span>
                <span className="room-badge badge-public">Công khai</span>
              </>
            ) : (
              <>
                <Lock size={20} color="var(--warning)" />
                <span className="room-name">Chat riêng với {activePartner}</span>
                <span className="room-badge badge-private">Bảo mật</span>
              </>
            )}
          </div>

          {/* Các nút hành động trên Topbar */}
          {!isPublic && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {/* Nút về phòng chung: chỉ chuyển view, KHÔNG hủy private session */}
              <button
                type="button"
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6 }}
                onClick={() => handleSelectConversation('public')}
                title="Quay lại xem phòng chung (vẫn giữ phiên chat riêng)"
              >
                <ArrowLeft size={15} />
                <span>Về phòng chung</span>
              </button>

              {/* Nút kết thúc chat riêng: gửi action end_private_chat và xóa session */}
              <button
                type="button"
                className="btn btn-danger"
                style={{ padding: '6px 12px', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6, backgroundColor: 'rgba(239, 68, 68, 0.2)', color: '#fca5a5', borderColor: 'rgba(239, 68, 68, 0.4)' }}
                onClick={() => handleEndPrivateChat(activePartner)}
                title="Đóng và kết thúc cuộc trò chuyện riêng này"
              >
                <XCircle size={15} />
                <span>Kết thúc chat riêng</span>
              </button>
            </div>
          )}
        </div>

        {/* Cảnh báo mất kết nối */}
        {!isConnected && (
          <div
            style={{
              padding: '8px 16px',
              backgroundColor: 'rgba(239, 68, 68, 0.2)',
              color: '#fca5a5',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.85rem'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <WifiOff size={16} />
              <span>
                {connectionState === 'connecting'
                  ? 'Đang kết nối lại tới máy chủ...'
                  : 'Mất kết nối tới máy chủ.'}
              </span>
            </div>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '4px 10px', fontSize: '0.75rem' }}
              onClick={() => socket.connect()}
            >
              <RefreshCw size={12} />
              Thử lại
            </button>
          </div>
        )}

        {/* Danh sách tin nhắn của conversation hiện tại */}
        <MessageList messages={currentMessages} currentUser={currentUser} />

        {/* Ô nhập tin nhắn */}
        <MessageInput
          onSendMessage={handleSendMessage}
          disabled={!isConnected}
          placeholder={
            isPublic
              ? 'Gửi tin nhắn tới phòng chung...'
              : `Gửi tin nhắn riêng tới ${activePartner}...`
          }
        />
      </main>

      {/* Modal đổi mật khẩu */}
      <ChangePasswordModal
        isOpen={isChangePassOpen}
        onClose={() => setIsChangePassOpen(false)}
      />
    </div>
  )
}
