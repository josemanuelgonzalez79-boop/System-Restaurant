export interface RestaurantSettings {
  id: number;
  businessName: string;
  displayName: string;
  currencyCode: string;
  timezone: string;
  primaryColor: string;
  secondaryColor: string;
  phone: string | null;
  address: string | null;
  version: number;
  updatedAt: string;
}

export interface RestaurantSettingsPayload {
  businessName: string;
  displayName: string;
  currencyCode: string;
  timezone: string;
  primaryColor: string;
  secondaryColor: string;
  phone: string;
  address: string;
}
