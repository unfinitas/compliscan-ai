"use server";

import { RequestEnum } from "./requestEnum";
import { sendRequest as fetchApi } from "./request";

export async function fetch(
  method: RequestEnum,
  url: string,
  headers?: object,
  data?: object
) {
  return fetchApi(
    method,
    url,
    { "Content-Type": "application/json", ...headers },
    data
  );
}

export async function sendRequest(
  moeId: string | null,
  method: RequestEnum,
  url: string,
  data?: object
) {
  if (!moeId) {
    return fetch(method, url, {}, data);
  }

  // POST: add moeId to body
  if (method === RequestEnum.POST) {
    return fetch(method, url, {}, { ...data, moeId });
  }

  // GET: add moeId as query param
  const separator = url.includes("?") ? "&" : "?";
  const urlWithMoeId = `${url}${separator}moeId=${moeId}`;
  return fetch(method, urlWithMoeId, {}, data);
}
