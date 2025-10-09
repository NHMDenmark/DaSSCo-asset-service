import {Pipe, PipeTransform, OnDestroy} from '@angular/core';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';

@Pipe({
  name: 'safeUrl',
  pure: false // must be impure so transform runs on each new Blob
})
export class SafeUrlPipe implements PipeTransform, OnDestroy {
  private objectUrl: string | null = null;

  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | Blob | null): SafeUrl | null {
    // Clean up any previously created URL
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }

    if (!value) {
      return null;
    }

    let safe: SafeUrl;

    if (value instanceof Blob) {
      this.objectUrl = URL.createObjectURL(value);
      safe = this.sanitizer.bypassSecurityTrustUrl(this.objectUrl);
    } else {
      safe = this.sanitizer.bypassSecurityTrustUrl(value);
    }

    return safe;
  }

  ngOnDestroy(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
