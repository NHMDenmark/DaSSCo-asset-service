import {InjectionToken} from "@angular/core";
import {checkNotUndefined} from "@northtech/ginnungagap";

export const FileProxy = new InjectionToken<string>('File Proxy Url', {
  providedIn: "root",
  factory: () => checkNotUndefined((window as any).frontendProperties?.fileProxyRootUrl, 'missing fileproxy in frontendProperties'),
});

export const AssetService = new InjectionToken<string>('Asset Service Url', {
  providedIn: "root",
  factory: () => checkNotUndefined((window as any).frontendProperties?.rootUrl, 'missing assetservice in frontendProperties'),
});
