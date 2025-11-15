import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface MoeState {
  moeId: string | null;
}

const initialState: MoeState = {
  moeId: null,
};

const moeSlice = createSlice({
  name: "moe",
  initialState,
  reducers: {
    setMoeId: (state, action: PayloadAction<string>) => {
      state.moeId = action.payload;
    },
    clearMoeId: (state) => {
      state.moeId = null;
    },
  },
});

export const { setMoeId, clearMoeId } = moeSlice.actions;
export default moeSlice.reducer;

