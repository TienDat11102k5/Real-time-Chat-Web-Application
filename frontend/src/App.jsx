import React, { useState, useEffect } from 'react'
import { getToken, getStoredUsername, clearAuth, authApi } from './api/http'
import { isAdminAuthenticated, clearAdminAuth, getAdminToken } from './admin/adminApi'
import LoginForm from './components/LoginForm'
import RegisterForm from './components/RegisterForm'
import ChatLayout from './components/ChatLayout'
import AdminLayout from './admin/AdminLayout'
import AdminLogin from './admin/AdminLogin'

export default function App() {
  const [token, setTokenState] = useState(getToken())
  const [currentUser, setCurrentUser] = useState(getStoredUsername())
  const [adminToken, setAdminToken] = useState(getAdminToken())
  const [isRegistering, setIsRegistering] = useState(false)
  const [isCheckingAuth, setIsCheckingAuth] = useState(true)
  const [hash, setHash] = useState(window.location.hash)

  // Lắng nghe thay đổi hash URL (hỗ trợ điều hướng #/admin, #/admin/login)
  useEffect(() => {
    const handleHashChange = () => {
      setHash(window.location.hash)
      setAdminToken(getAdminToken())
    }
    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  // Kiểm tra tính hợp lệ của token khi mở trang
  useEffect(() => {
    const verifyAuth = async () => {
      const savedToken = getToken()
      if (savedToken) {
        try {
          const res = await authApi.getMe()
          if (res.ok && res.username) {
            setCurrentUser(res.username)
            setTokenState(savedToken)
          } else {
            handleLogout()
          }
        } catch {
          handleLogout()
        }
      }
      setIsCheckingAuth(false)
    }

    verifyAuth()
  }, [])

  const handleLoginSuccess = (newToken, username) => {
    setTokenState(newToken)
    setCurrentUser(username)
  }

  const handleLogout = () => {
    clearAuth()
    setTokenState(null)
    setCurrentUser(null)
  }

  if (isCheckingAuth) {
    return (
      <div className="auth-container">
        <div style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
          Đang kết nối tới hệ thống...
        </div>
      </div>
    )
  }

  // ==========================================
  // ROUTING CHO ADMIN PANEL (#/admin hoặc #admin)
  // ==========================================
  const isAdminRoute = hash.startsWith('#/admin') || hash.startsWith('#admin')
  if (isAdminRoute) {
    const adminLoggedIn = !!adminToken
    if (adminLoggedIn && !hash.includes('/login')) {
      return (
        <AdminLayout
          onBackToChat={() => {
            window.location.hash = ''
            setHash('')
          }}
          onLogout={() => {
            clearAdminAuth()
            setAdminToken(null)
            window.location.hash = '#/admin/login'
            setHash('#/admin/login')
          }}
        />
      )
    } else {
      return (
        <AdminLogin
          onLoginSuccess={() => {
            setAdminToken(getAdminToken())
            window.location.hash = '#/admin'
            setHash('#/admin')
          }}
          onBackToChat={() => {
            window.location.hash = ''
            setHash('')
          }}
        />
      )
    }
  }

  // ==========================================
  // ROUTING CHO USER CHAT
  // ==========================================
  if (!token || !currentUser) {
    if (isRegistering) {
      return <RegisterForm onSwitchToLogin={() => setIsRegistering(false)} />
    }
    return (
      <LoginForm
        onLoginSuccess={handleLoginSuccess}
        onSwitchToRegister={() => setIsRegistering(true)}
      />
    )
  }

  return <ChatLayout currentUser={currentUser} onLogout={handleLogout} />
}
