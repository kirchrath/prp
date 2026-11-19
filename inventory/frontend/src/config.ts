export interface AppConfig {
  /** Without OIDC configuration the app runs without login (local development only). */
  oidc?: {
    authority: string;
    clientId: string;
  };
}

declare global {
  interface Window {
    __APP_CONFIG__?: AppConfig;
  }
}

export const config: AppConfig = window.__APP_CONFIG__ ?? {};
