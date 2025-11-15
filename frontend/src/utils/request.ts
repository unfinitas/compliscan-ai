import { RequestEnum } from "./requestEnum";

export async function sendRequest(
  method: RequestEnum,
  url: string,
  headers?: Record<string, string>,
  data?: object
) {
  const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "";
  const fullUrl = url.startsWith("http")
    ? url
    : `${baseUrl.replace(/\/$/, "")}/${url.replace(/^\//, "")}`;

  return await fetch(fullUrl, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}
