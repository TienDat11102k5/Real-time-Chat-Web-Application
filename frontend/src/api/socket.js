import { getToken } from './http'

class ChatSocket {
  constructor() {
    this.ws = null
    this.listeners = new Map()
    this.statusListeners = new Set()
    this.isConnected = false
    this.reconnectTimer = null
    this.wasKicked = false
  }

  connect() {
    const token = getToken()
    if (!token) {
      console.warn('[WS] Chưa có token, không thể kết nối')
      return
    }

    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.wasKicked = false
    this.notifyStatus('connecting')

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws?token=${encodeURIComponent(token)}`

    try {
      this.ws = new WebSocket(wsUrl)
    } catch (err) {
      this.notifyStatus('disconnected')
      this.scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      this.isConnected = true
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
      this.notifyStatus('connected')
      console.log('[WS] Kết nối WebSocket thành công')
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        const type = data.type

        if (type === 'kicked') {
          this.wasKicked = true
          if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
            this.reconnectTimer = null
          }
        }

        if (type && this.listeners.has(type)) {
          this.listeners.get(type).forEach((fn) => fn(data))
        }
      } catch (e) {
        console.error('[WS] Lỗi parse message:', e, event.data)
      }
    }

    this.ws.onclose = (event) => {
      this.isConnected = false
      this.notifyStatus('disconnected')
      console.log('[WS] Ngắt kết nối WebSocket', event.code, event.reason)
      
      // Nếu không phải do bị kicked và không phải do chủ động đóng thì thử kết nối lại
      if (!this.wasKicked && event.code !== 1000 && event.code !== 1008) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (err) => {
      console.error('[WS ERROR]', err)
    }
  }

  scheduleReconnect() {
    if (this.wasKicked) return
    if (this.reconnectTimer) return
    console.log('[WS] Lên lịch kết nối lại sau 3 giây...')
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      const token = getToken()
      if (token && !this.wasKicked) {
        this.connect()
      }
    }, 3000)
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
    this.notifyStatus('disconnected')
  }

  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type).add(callback)
    return () => this.off(type, callback)
  }

  off(type, callback) {
    if (this.listeners.has(type)) {
      this.listeners.get(type).delete(callback)
    }
  }

  onStatusChange(callback) {
    this.statusListeners.add(callback)
    return () => this.statusListeners.delete(callback)
  }

  notifyStatus(status) {
    this.statusListeners.forEach((fn) => fn(status))
  }

  send(type, payload = {}) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WS] Không thể gửi tin: WebSocket chưa kết nối', type, payload)
      return false
    }

    try {
      const msg = { type, ...payload }
      this.ws.send(JSON.stringify(msg))
      return true
    } catch (e) {
      console.error('[WS] Lỗi gửi tin:', e)
      return false
    }
  }

  sendPublicMessage(message) {
    return this.send('public_message', { message })
  }

  startPrivateChat(target) {
    return this.send('start_private_chat', { target })
  }

  sendPrivateRequest(target, preview) {
    return this.send('start_private_chat', { target, preview, message: preview })
  }

  acceptPrivateRequest(sender) {
    return this.send('accept_request', { sender, from: sender })
  }

  acceptRequest(sender) {
    return this.acceptPrivateRequest(sender)
  }

  declinePrivateRequest(sender) {
    return this.send('decline_request', { sender, from: sender })
  }

  declineRequest(sender) {
    return this.declinePrivateRequest(sender)
  }

  sendPrivateMessage(targetOrMsg, message) {
    if (message === undefined) {
      return this.send('private_message', { message: targetOrMsg })
    }
    return this.send('private_message', { target: targetOrMsg, message })
  }

  endPrivateChat(target) {
    return this.send('end_private_chat', { target })
  }

  backToPublic() {
    return this.send('back_to_public')
  }
}

export const socket = new ChatSocket()

