import { Injectable } from '@angular/core';

import { MSAL_INSTANCE, MsalService } from '@azure/msal-angular';
import { IPublicClientApplication, PublicClientApplication } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class MicrosoftLoginMsal { }

  export function MSALInstanceFactory(): IPublicClientApplication {
    return new PublicClientApplication({
      auth: {
        clientId: environment.msalClientId,
        authority: `https://login.microsoftonline.com/${environment.msalTenantId}`,
        redirectUri: environment.msalRedirectUri
      }
    });
  }

// Exportamos os providers limpos para o app.config.ts apenas ler
export const msalProviders = [
  { provide: MSAL_INSTANCE, useFactory: MSALInstanceFactory },
  MsalService
];
