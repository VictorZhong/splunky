import { create } from 'zustand'
import type { DrawerSelection } from '../types'

interface InvestigationUiState {
  drawerSelection: DrawerSelection | null
  openDrawer: (drawerSelection: DrawerSelection) => void
  closeDrawer: () => void
}

export const useInvestigationUiStore = create<InvestigationUiState>((set) => ({
  drawerSelection: null,
  openDrawer: (drawerSelection) => set({ drawerSelection }),
  closeDrawer: () => set({ drawerSelection: null }),
}))
