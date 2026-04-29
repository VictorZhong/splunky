import { create } from 'zustand'
import type { DrawerSelection } from '../types'

export type InvestigationTabKey =
  | 'summary'
  | 'timeline'
  | 'graph'
  | 'sequence'
  | 'calls'
  | 'logs'
  | 'spl'

interface InvestigationUiState {
  activeTab: InvestigationTabKey
  drawerSelection: DrawerSelection | null
  setActiveTab: (activeTab: InvestigationTabKey) => void
  openDrawer: (drawerSelection: DrawerSelection) => void
  closeDrawer: () => void
}

export const useInvestigationUiStore = create<InvestigationUiState>((set) => ({
  activeTab: 'summary',
  drawerSelection: null,
  setActiveTab: (activeTab) => set({ activeTab }),
  openDrawer: (drawerSelection) => set({ drawerSelection }),
  closeDrawer: () => set({ drawerSelection: null }),
}))
