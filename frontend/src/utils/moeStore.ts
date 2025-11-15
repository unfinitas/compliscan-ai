/**
 * Helper functions to work with moeId in Redux
 */

import { useAppDispatch, useAppSelector } from "@/lib/redux/hooks";
import {
  setMoeId as setMoeIdAction,
  clearMoeId as clearMoeIdAction,
} from "@/lib/redux/moeSlice";

/**
 * Hook to access and manage moeId
 */
export function useMoeId() {
  const dispatch = useAppDispatch();
  const moeId = useAppSelector((state) => state.moe.moeId);

  return {
    moeId,
    setMoeId: (id: string) => dispatch(setMoeIdAction(id)),
    clearMoeId: () => dispatch(clearMoeIdAction()),
    hasMoeId: () => !!moeId,
  };
}
