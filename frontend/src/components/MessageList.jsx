import React, { useEffect, useRef } from 'react'

export default function MessageList({ messages, currentUser }) {
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="message-list-container">
      {messages.length === 0 ? (
        <div className="system-msg">Chưa có tin nhắn nào trong phòng này. Hãy bắt đầu cuộc trò chuyện!</div>
      ) : (
        messages.map((msg, idx) => {
          if (msg.type === 'system') {
            return (
              <div key={idx} className="system-msg">
                {msg.message}
              </div>
            )
          }

          const isSelf = msg.sender === currentUser
          return (
            <div
              key={idx}
              className={`message-bubble-wrapper ${isSelf ? 'self' : 'other'}`}
            >
              {!isSelf && <div className="msg-sender">{msg.sender}</div>}
              <div className="message-bubble">
                <div>{msg.message}</div>
                {msg.timestamp && <div className="msg-time">{msg.timestamp}</div>}
              </div>
            </div>
          )
        })
      )}
      <div ref={bottomRef} />
    </div>
  )
}
