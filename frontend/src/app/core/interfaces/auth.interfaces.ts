export interface LoginCredentials {
  email: string;
  password: string;
}

export interface SignupPayload extends LoginCredentials {
  name: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: AuthUser;
}

export interface AuthApiResponse {
  id: string;
  name: string;
  email: string;
  role: string;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}
