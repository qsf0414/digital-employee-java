export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'digital_employee_token',
  REFRESH_TOKEN: 'digital_employee_refresh_token',
  USER_INFO: 'digital_employee_user',
} as const;

export const WHITE_LIST = ['/login', '/404'] as const;

export const HTTP_STATUS = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  TOO_MANY_REQUESTS: 429,
} as const;
