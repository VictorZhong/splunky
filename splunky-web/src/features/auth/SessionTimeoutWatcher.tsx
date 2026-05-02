import { Modal } from 'antd'
import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSessionStore } from './sessionStore'
import { logoutSession } from './sessionApi'

const inactivityLimitMs = 30 * 60 * 1000
const activityEvents = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart']

export function SessionTimeoutWatcher() {
  const navigate = useNavigate()
  const session = useSessionStore((state) => state.session)
  const logout = useSessionStore((state) => state.logout)
  const lastActivityAt = useRef(0)
  const modalOpen = useRef(false)

  useEffect(() => {
    if (!session) {
      return
    }

    lastActivityAt.current = Date.now()

    function markActivity() {
      lastActivityAt.current = Date.now()
    }

    function expireSession() {
      if (Date.now() - lastActivityAt.current < inactivityLimitMs) {
        return
      }

      if (modalOpen.current) {
        return
      }

      modalOpen.current = true
      if (session?.sessionId) {
        void logoutSession(session.sessionId).catch(() => undefined)
      }
      logout()
      Modal.info({
        title: 'Session expired',
        content:
          'Your Splunky session expired after 30 minutes of inactivity. Please log in again before querying Splunk.',
        okText: 'Log in again',
        onOk: () => {
          modalOpen.current = false
          navigate('/login', { replace: true })
        },
        afterClose: () => {
          modalOpen.current = false
        },
      })
    }

    activityEvents.forEach((eventName) =>
      window.addEventListener(eventName, markActivity, { passive: true }),
    )
    const timer = window.setInterval(expireSession, 30_000)

    return () => {
      activityEvents.forEach((eventName) =>
        window.removeEventListener(eventName, markActivity),
      )
      window.clearInterval(timer)
    }
  }, [logout, navigate, session])

  return null
}
