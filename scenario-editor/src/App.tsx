import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { ScenarioListPage } from './components/ScenarioListPage'
import { EditorPage } from './components/EditorPage'

export default function App() {
  return (
    <>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#1e293b',
            color: '#e2e8f0',
            border: '1px solid #334155',
            borderRadius: '10px',
            fontSize: '13px',
          },
        }}
      />
      <Routes>
        <Route path="/"            element={<ScenarioListPage />} />
        <Route path="/editor/:id"  element={<EditorPage />} />
        <Route path="*"            element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}
