export const environment = {
  production: false,

  // Azure AD/Entra ID - sobrescritos via .env (NG_APP_*)
  msalClientId: import.meta.env['NG_APP_MSAL_CLIENT_ID'] as string | undefined ?? '',
  msalAuthority: import.meta.env['NG_APP_MSAL_AUTHORITY'] as string | undefined
    ?? 'https://login.microsoftonline.com/common',
  msalRedirectUri: import.meta.env['NG_APP_MSAL_REDIRECT_URI'] as string | undefined
    ?? 'http://localhost:4200',

  // URL base da API Spring Boot
  apiUrl: import.meta.env['NG_APP_API_URL'] as string | undefined ?? 'http://localhost:4040'
};