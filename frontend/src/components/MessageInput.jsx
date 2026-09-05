import React, { useState, useRef } from 'react'
import { Send, Smile } from 'lucide-react'
import QuickIconPicker from './QuickIconPicker'

export default function MessageInput({ onSendMessage, disabled, placeholder }) {
  const [text, setText] = useState('')
  const [showPicker, setShowPicker] = useState(false)
  const inputRef = useRef(null)
  const MAX_LENGTH = 500

  const handleSend = () => {
    const trimmed = text.trim()
    if (!trimmed || disabled) return
    onSendMessage(trimmed)
    setText('')
    setShowPicker(false)
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleSendIcon = (icon) => {
    if (disabled) return
    onSendMessage(icon)
    setShowPicker(false)
  }

  const handleInsertIcon = (icon) => {
    if (disabled) return
    const input = inputRef.current
    if (!input) {
      if (text.length + icon.length <= MAX_LENGTH) {
        setText((prev) => prev + icon)
      }
      return
    }

    const start = input.selectionStart || text.length
    const end = input.selectionEnd || text.length
    const before = text.substring(0, start)
    const after = text.substring(end)

    if (before.length + icon.length + after.length <= MAX_LENGTH) {
      const newText = before + icon + after
      setText(newText)
      setTimeout(() => {
        input.focus()
        const newPos = start + icon.length
        input.setSelectionRange(newPos, newPos)
      }, 0)
    }
  }

  const charCount = text.length
  let counterClass = 'char-counter'
  if (charCount >= 500) counterClass += ' at-limit'
  else if (charCount >= 450) counterClass += ' near-limit'

  return (
    <div className="chat-input-area">
      {showPicker && (
        <QuickIconPicker
          onSendIcon={handleSendIcon}
          onInsertIcon={handleInsertIcon}
          onClose={() => setShowPicker(false)}
        />
      )}

      <div className="input-wrapper">
        <button
          type="button"
          className={`btn-emoji-toggle ${showPicker ? 'active' : ''}`}
          onClick={() => setShowPicker((prev) => !prev)}
          disabled={disabled}
          title="Biểu tượng cảm xúc (Quick Icon)"
        >
          <Smile size={20} />
        </button>

        <input
          ref={inputRef}
          type="text"
          className="chat-input"
          placeholder={placeholder || 'Nhập tin nhắn (tối đa 500 ký tự)...'}
          value={text}
          onChange={(e) => {
            if (e.target.value.length <= MAX_LENGTH) {
              setText(e.target.value)
            }
          }}
          onKeyDown={handleKeyDown}
          maxLength={MAX_LENGTH}
          disabled={disabled}
        />

        <div className="input-meta">
          <span className={counterClass}>
            {charCount}/{MAX_LENGTH}
          </span>
          <button
            type="button"
            className="btn-send"
            onClick={handleSend}
            disabled={!text.trim() || disabled}
            title="Gửi (Enter)"
          >
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  )
}
