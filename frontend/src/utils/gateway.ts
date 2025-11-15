"use client";

import { RequestEnum } from "./requestEnum";
import { sendRequest as fetchApi } from "./request";
import { ApiResponse } from "./ApiResponse";

export async function fetchRequest(
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
    return fetchRequest(method, url, {}, data);
  }

  // POST: add moeId to body
  if (method === RequestEnum.POST) {
    return fetchRequest(method, url, {}, { ...data, moeId });
  }

  // GET: add moeId as query param
  const separator = url.includes("?") ? "&" : "?";
  const urlWithMoeId = `${url}${separator}moeId=${moeId}`;
  return fetchRequest(method, urlWithMoeId, {}, data);
}

/**
 * Upload a file with FormData
 * @param moeId Optional moeId to include in the request
 * @param url API endpoint URL
 * @param formData FormData containing the file
 * @returns ApiResponse with the parsed JSON response
 */
export async function uploadFile<T>(
  moeId: string | null,
  url: string,
  formData: FormData
): Promise<T> {
  const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "";
  const fullUrl = url.startsWith("http")
    ? url
    : `${baseUrl.replace(/\/$/, "")}/${url.replace(/^\//, "")}`;

  // Add moeId to FormData if provided
  if (moeId) {
    formData.append("moeId", moeId);
  }

  const response = await fetch(fullUrl, {
    method: "POST",
    body: formData,
    // Don't set Content-Type - browser will set it with boundary for multipart/form-data
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Upload failed: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
}

/**
 * Send a request and return ApiResponse
 * @param moeId Optional moeId to include in the request
 * @param method HTTP method
 * @param url API endpoint URL
 * @param data Optional request body data
 * @returns ApiResponse with the parsed JSON response
 */
export async function sendRequestWithResponse<T>(
  moeId: string | null,
  method: RequestEnum,
  url: string,
  data?: object
): Promise<T> {
  const response = await sendRequest(moeId, method, url, data);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
}
