import React, { useEffect, useState } from 'react'
import { Check, X, Clock, MessageSquare } from 'lucide-react'

export default function PrivateRequestDialog({ request, onAccept, onDecline }) {
  const [timeLeft, setTimeLeft] = useState(request?.timeout || 60)

  useEffect(() => {
    setTimeLeft(request?.timeout || 60)
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer)
          onDecline(request.from)
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [request])

  if (!request) return null

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <MessageSquare size={20} color="var(--primary)" />
            <h3>Yêu cầu Chat riêng</h3>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--warning)', fontSize: '0.85rem' }}>
            <Clock size={16} />
            <span>{timeLeft}s</span>
          </div>
        </div>

        <div className="modal-body">
          <p style={{ marginBottom: 12, fontSize: '0.95rem' }}>
            <strong>{request.from}</strong> muốn mời bạn tham gia phòng chat riêng:
          </p>
          <div
            style={{
              padding: '12px 14px',
              backgroundColor: 'var(--bg-input)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.9rem',
              color: 'var(--text-main)',
              borderLeft: '4px solid var(--primary)',
              wordBreak: 'break-word'
            }}
          >
            "{request.preview || 'Không có tin nhắn kèm theo'}"
          </div>
        </div>

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => onDecline(request.from)}
          >
            <X size={16} />
            Từ chối
          </button>
          <button
            type="button"
            className="btn btn-primary"
            style={{ width: 'auto', backgroundColor: 'var(--success)' }}
            onClick={() => onAccept(request.from)}
          >
            <Check size={16} />
            Chấp nhận
          </button>
        </div>
      </div>
    </div>
  )
}
