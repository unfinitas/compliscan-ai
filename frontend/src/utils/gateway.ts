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
 * @param moeId Optional moeId to include in the request (not used for document upload)
 * @param url API endpoint URL (should be full URL or relative path)
 * @param formData FormData containing the file
 * @returns ApiResponse with the parsed JSON response
 */
export async function uploadFile<T>(
  moeId: string | null,
  url: string,
  formData: FormData
): Promise<T> {
  // Construct full URL if needed
  let fullUrl: string;
  if (url.startsWith("http://") || url.startsWith("https://")) {
    fullUrl = url;
  } else {
    const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
    const cleanBase = baseUrl.replace(/\/$/, "");
    const cleanPath = url.replace(/^\//, "");
    fullUrl = `${cleanBase}/${cleanPath}`;
  }

  // Note: Backend only expects "file" parameter, not moeId
  // moeId parameter is kept for API consistency but not sent to backend

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
