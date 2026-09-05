import React, { useEffect } from 'react'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'

export default function Toast({ message, type = 'info', onClose }) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose()
    }, 4000)
    return () => clearTimeout(timer)
  }, [message, onClose])

  if (!message) return null

  const getIcon = () => {
    switch (type) {
      case 'error':
        return <AlertCircle size={18} color="#ef4444" />
      case 'success':
        return <CheckCircle2 size={18} color="#10b981" />
      default:
        return <Info size={18} color="#3b82f6" />
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 20,
        right: 20,
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        backgroundColor: 'var(--bg-card)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        padding: '12px 16px',
        boxShadow: 'var(--shadow)',
        color: 'var(--text-main)',
        fontSize: '0.9rem',
        maxWidth: 360,
        animation: 'modalScale 0.2s ease-out'
      }}
    >
      {getIcon()}
      <div style={{ flex: 1 }}>{message}</div>
      <button
        type="button"
        onClick={onClose}
        style={{
          background: 'none',
          border: 'none',
          color: 'var(--text-muted)',
          cursor: 'pointer',
          display: 'flex'
        }}
      >
        <X size={16} />
      </button>
    </div>
  )
}
