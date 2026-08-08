import { UserRole } from './user.model';

export interface Branch {
  id: number;
  code: string;
  name: string;
  address: string | null;
  phone: string | null;
  timezone: string;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface BranchPayload {
  code: string;
  name: string;
  address: string;
  phone: string;
  timezone: string;
  sortOrder: number;
}

export type AreaType = 'SERVICE' | 'PRODUCTION' | 'STORAGE' | 'CHECKOUT' | 'OFFICE' | 'OTHER';

export interface OperationalArea {
  id: number;
  branchId: number;
  branchName: string;
  name: string;
  description: string | null;
  areaType: AreaType;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OperationalAreaPayload {
  branchId: number;
  name: string;
  description: string;
  areaType: AreaType;
  sortOrder: number;
}

export type ServicePointType =
  'TABLE' | 'COUNTER' | 'CHECKOUT' | 'STATION' | 'ROOM' | 'DESK' | 'WINDOW' | 'OTHER';

export interface ServicePoint {
  id: number;
  areaId: number;
  areaName: string;
  branchId: number;
  branchName: string;
  code: string;
  name: string;
  description: string | null;
  pointType: ServicePointType;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ServicePointPayload {
  areaId: number;
  code: string;
  name: string;
  description: string;
  pointType: ServicePointType;
  sortOrder: number;
}

export interface BranchAssignmentUser {
  userId: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  assigned: boolean;
}
