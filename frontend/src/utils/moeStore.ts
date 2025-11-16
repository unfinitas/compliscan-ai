/**
 * Helper functions to work with moeId in Redux and localStorage
 */

import { useAppDispatch, useAppSelector } from "@/lib/redux/hooks";
import {
  setMoeId as setMoeIdAction,
  clearMoeId as clearMoeIdAction,
} from "@/lib/redux/moeSlice";

const MOE_ID_KEY = "compliscan_moeId";
const ANALYSIS_ID_KEY = "compliscan_analysisId";

/**
 * Get moeId from sessionStorage
 */
export function getMoeIdFromStorage(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(MOE_ID_KEY);
}

/**
 * Set moeId in sessionStorage
 */
export function setMoeIdInStorage(id: string | null): void {
  if (typeof window === "undefined") return;
  if (id) {
    sessionStorage.setItem(MOE_ID_KEY, id);
  } else {
    sessionStorage.removeItem(MOE_ID_KEY);
  }
}

/**
 * Get analysisId from sessionStorage
 */
export function getAnalysisIdFromStorage(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(ANALYSIS_ID_KEY);
}

/**
 * Set analysisId in sessionStorage
 */
export function setAnalysisIdInStorage(id: string | null): void {
  if (typeof window === "undefined") return;
  if (id) {
    sessionStorage.setItem(ANALYSIS_ID_KEY, id);
  } else {
    sessionStorage.removeItem(ANALYSIS_ID_KEY);
  }
}

/**
 * Hook to access and manage moeId
 */
export function useMoeId() {
  const dispatch = useAppDispatch();
  const moeId = useAppSelector((state) => state.moe.moeId);

  return {
    moeId,
    setMoeId: (id: string) => {
      dispatch(setMoeIdAction(id));
      setMoeIdInStorage(id);
    },
    clearMoeId: () => {
      dispatch(clearMoeIdAction());
      setMoeIdInStorage(null);
    },
    hasMoeId: () => !!moeId,
  };
}
