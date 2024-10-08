import {NgModule} from '@angular/core';
import {AuthModule, LogLevel} from 'angular-auth-oidc-client';

@NgModule({
  imports: [
    AuthModule.forRoot({
      config: {
        authority: (window as any).frontendProperties?.authenticationUrl,
        redirectUrl: window.location.origin + '/ars/',
        postLogoutRedirectUri: window.location.origin + '/ars/',
        clientId: (window as any).frontendProperties?.clientId,
        scope: 'openid profile offline_access',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        logLevel: LogLevel.Warn,
        ignoreNonceAfterRefresh: true
      }
    })],
  exports: [AuthModule]
})
export class AuthConfigModule {
}
