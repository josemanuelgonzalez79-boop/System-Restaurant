export type UserRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'CASHIER' | 'OPERATOR';

export interface EssentialUser {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface InitialSetupPayload {
  username: string;
  fullName: string;
  password: string;
}

export interface UserCreatePayload {
  username: string;
  fullName: string;
  role: UserRole;
  password: string;
}

export interface UserUpdatePayload {
  fullName: string;
  role: UserRole;
}
