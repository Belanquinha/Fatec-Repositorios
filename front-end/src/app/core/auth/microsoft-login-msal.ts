import { MSAL_INSTANCE, MsalService } from '@azure/msal-angular';
import {
  BrowserCacheLocation,
  IPublicClientApplication,
  PublicClientApplication,
} from '@azure/msal-browser';
import { environment } from '../../../environments/environment';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msalClientId,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    },
    cache: {
      /**
       * `localStorage`, e não o `sessionStorage` padrão: o cache padrão morre com a aba, e sem ele
       * cada nova aba vira um `loginRedirect` completo — ou seja, MFA a cada troca de aba.
       *
       * O custo é conhecido: token em `localStorage` é legível por qualquer script que rode na
       * página. Para um SPA acadêmico sem dado sensível fora do domínio do tenant, o silêncio do
       * login vale mais que a proteção contra XSS. Se um dia inverter, reverta para
       * `BrowserCacheLocation.SessionStorage` e o problema do MFA volta.
       */
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
  });
}

// Exportamos os providers limpos para o app.config.ts apenas ler
export const msalProviders = [
  { provide: MSAL_INSTANCE, useFactory: MSALInstanceFactory },
  MsalService,
];