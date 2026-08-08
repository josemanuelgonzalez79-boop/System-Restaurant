export type BusinessType = 'RESTAURANT' | 'RETAIL' | 'SERVICES' | 'OTHER';

export interface BusinessSettings {
  id: number;
  businessName: string;
  displayName: string;
  businessType: BusinessType;
  currencyCode: string;
  timezone: string;
  primaryColor: string;
  secondaryColor: string;
  phone: string | null;
  address: string | null;
  version: number;
  updatedAt: string;
}

export interface BusinessSettingsPayload {
  businessName: string;
  displayName: string;
  businessType: BusinessType;
  currencyCode: string;
  timezone: string;
  primaryColor: string;
  secondaryColor: string;
  phone: string;
  address: string;
}
