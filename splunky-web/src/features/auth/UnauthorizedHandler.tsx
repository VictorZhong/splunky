import { Modal } from 'antd'
import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSessionStore } from './sessionStore'
import { unauthorizedEventName } from './authEvents'
import { logoutSession } from './sessionApi'

export function UnauthorizedHandler() {
  const navigate = useNavigate()
  const session = useSessionStore((state) => state.session)
  const logout = useSessionStore((state) => state.logout)
  const modalOpen = useRef(false)

  useEffect(() => {
    function handleUnauthorized() {
      if (modalOpen.current) {
        return
      }

      modalOpen.current = true
      if (session?.sessionId) {
        void logoutSession(session.sessionId).catch(() => undefined)
      }
      logout()
      Modal.warning({
        title: 'Splunk credentials may be invalid',
        content:
          'The backend returned 401 for this investigation session. Please log in again with your Splunk username and password.',
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

    window.addEventListener(unauthorizedEventName, handleUnauthorized)
    return () =>
      window.removeEventListener(unauthorizedEventName, handleUnauthorized)
  }, [logout, navigate, session])

  return null
}
