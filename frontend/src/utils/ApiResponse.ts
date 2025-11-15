/**
 * Backend API response wrapper
 */
export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

/**
 * API result tuple type for server actions
 * [error, data, message]
 */
export type ApiResult<T> =
  | [null, T, string] // Success: [null, data, message]
  | [string, null, string]; // Error: [errorCode, null, message]

/**
 * Make all properties optional recursively
 */
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

/**
 * Make all properties required recursively
 */
export type DeepRequired<T> = {
  [P in keyof T]-?: T[P] extends object ? DeepRequired<T[P]> : T[P];
};
