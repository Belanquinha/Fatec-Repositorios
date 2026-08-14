import { MSAL_INSTANCE, MsalService } from '@azure/msal-angular';
import { IPublicClientApplication, PublicClientApplication } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';

  export function MSALInstanceFactory(): IPublicClientApplication {
    return new PublicClientApplication({
      auth: {
        clientId: environment.msalClientId,
        authority: environment.msalAuthority,
        redirectUri: environment.msalRedirectUri
      }
    });
  }

// Exportamos os providers limpos para o app.config.ts apenas ler
export const msalProviders = [
  { provide: MSAL_INSTANCE, useFactory: MSALInstanceFactory },
  MsalService
];
