export interface HealthStatus {
  application: string;
  status: 'UP' | 'DEGRADED';
  database: 'UP' | 'DOWN';
  timestamp: string;
}
