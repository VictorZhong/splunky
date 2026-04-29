import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusTag } from './StatusTag'

describe('StatusTag', () => {
  it('renders readable status text', () => {
    render(<StatusTag status="NO_RESULT" />)
    expect(screen.getByText('NO RESULT')).toBeInTheDocument()
  })
})
