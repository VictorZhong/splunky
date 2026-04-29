import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Spin } from 'antd'
import { lazy, Suspense } from 'react'

const InvestigationInputPage = lazy(() =>
  import('../pages/InvestigationPage/InvestigationInputPage').then((module) => ({
    default: module.InvestigationInputPage,
  })),
)
const InvestigationPage = lazy(() =>
  import('../pages/InvestigationPage/InvestigationPage').then((module) => ({
    default: module.InvestigationPage,
  })),
)

function RouteFallback() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <Spin size="large" />
    </div>
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/" element={<InvestigationInputPage />} />
          <Route
            path="/investigations/:investigationId"
            element={<InvestigationPage />}
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
