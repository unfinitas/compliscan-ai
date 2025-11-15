import { configureStore } from "@reduxjs/toolkit";
import moeReducer from "./moeSlice";

export const makeStore = () => {
  return configureStore({
    reducer: {
      moe: moeReducer,
    },
  });
};

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];

