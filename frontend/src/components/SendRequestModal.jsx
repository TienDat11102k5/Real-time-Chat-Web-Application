import React, { useState } from 'react'
import { MessageSquare, Send, X } from 'lucide-react'

export default function SendRequestModal({ target, isOpen, onClose, onSend }) {
  const [message, setMessage] = useState('Chat riêng nhé!')
  const [error, setError] = useState('')

  if (!isOpen || !target) return null

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!message.trim()) {
      setError('Vui lòng nhập lời nhắn')
      return
    }
    if (message.length > 500) {
      setError('Lời nhắn tối đa 500 ký tự')
      return
    }
    onSend(target, message.trim())
    onClose()
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <MessageSquare size={20} color="var(--primary)" />
            <h3>Gửi yêu cầu Chat riêng</h3>
          </div>
          <button type="button" className="icon-btn" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        {error && <div className="alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Người nhận</label>
            <input type="text" value={target} disabled style={{ opacity: 0.8 }} />
          </div>

          <div className="form-group">
            <label htmlFor="preview-msg">Lời nhắn đính kèm</label>
            <textarea
              id="preview-msg"
              className="chat-input"
              style={{
                width: '100%',
                padding: '10px 12px',
                backgroundColor: 'var(--bg-input)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-sm)',
                color: 'var(--text-main)',
                minHeight: 80
              }}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              maxLength={500}
              autoFocus
            />
            <div style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>
              {message.length}/500
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Hủy
            </button>
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
              <Send size={16} />
              Gửi yêu cầu (60s)
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
