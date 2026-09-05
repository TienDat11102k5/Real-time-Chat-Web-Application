import React, { useState, useEffect, useRef } from 'react'
import { Zap, Edit3, X } from 'lucide-react'

export default function QuickIconPicker({ onSendIcon, onInsertIcon, onClose }) {
  const [mode, setMode] = useState('send') // 'send' hoặc 'insert'
  const pickerRef = useRef(null)

  const GROUPS = [
    {
      name: 'Phản ứng nhanh',
      icons: ['👍', '👎', '❤️', '😂']
    },
    {
      name: 'Cảm xúc',
      icons: ['😮', '😢', '🙏', '👏']
    },
    {
      name: 'Trạng thái',
      icons: ['✅', '❌', '⭐', '🔥']
    }
  ]

  // Đóng khi click ra ngoài
  useEffect(() => {
    function handleClickOutside(event) {
      if (pickerRef.current && !pickerRef.current.contains(event.target)) {
        if (onClose) onClose()
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [onClose])

  const handleIconClick = (icon) => {
    if (mode === 'send') {
      if (onSendIcon) onSendIcon(icon)
      if (onClose) onClose()
    } else {
      if (onInsertIcon) onInsertIcon(icon)
    }
  }

  return (
    <div className="quick-icon-picker" ref={pickerRef}>
      <div className="picker-header">
        <div className="picker-mode-switch">
          <button
            type="button"
            className={`mode-btn ${mode === 'send' ? 'active' : ''}`}
            onClick={() => setMode('send')}
            title="Click icon sẽ gửi ngay lập tức"
          >
            <Zap size={13} />
            <span>Gửi nhanh</span>
          </button>
          <button
            type="button"
            className={`mode-btn ${mode === 'insert' ? 'active' : ''}`}
            onClick={() => setMode('insert')}
            title="Click icon sẽ chèn vào vị trí con trỏ"
          >
            <Edit3 size={13} />
            <span>Chèn vào ô</span>
          </button>
        </div>
        {onClose && (
          <button type="button" className="btn-close-picker" onClick={onClose} title="Đóng">
            <X size={14} />
          </button>
        )}
      </div>

      <div className="picker-body">
        {GROUPS.map((group) => (
          <div key={group.name} className="icon-group">
            <div className="group-title">{group.name}</div>
            <div className="icon-grid">
              {group.icons.map((icon) => (
                <button
                  key={icon}
                  type="button"
                  className="icon-btn"
                  onClick={() => handleIconClick(icon)}
                  title={`${icon} (${mode === 'send' ? 'Gửi ngay' : 'Chèn'})`}
                >
                  {icon}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
