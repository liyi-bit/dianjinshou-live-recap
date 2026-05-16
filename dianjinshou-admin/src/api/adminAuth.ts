import http from './http';

export interface AdminLoginResult {
  accessToken: string;
  expiresIn: number;
  user: AdminUserInfo;
}

export interface AdminUserInfo {
  id: number;
  username: string;
  displayName: string | null;
  email: string | null;
  role: string;
  status: number;
  lastLoginAt: string | null;
}

export function adminLogin(username: string, password: string) {
  return http.post<any, AdminLoginResult>('/api/v1/admin-auth/login', { username, password });
}

export function adminMe() {
  return http.get<any, AdminUserInfo>('/api/v1/admin-auth/me');
}

export function adminLogout() {
  return http.post<any, void>('/api/v1/admin-auth/logout', {});
}
