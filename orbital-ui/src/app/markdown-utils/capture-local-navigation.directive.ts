import {Directive, HostListener} from '@angular/core';
import {Router} from "@angular/router";

/**
 * Captures the user clicking on anchor tags
 * within markdown, and uses Angular's router to handle
 * relative navigation, preventing full-app reloads
 */
@Directive({
  selector: '[appCaptureLocalNavigation]',
  standalone: true
})
export class CaptureLocalNavigationDirective {

  constructor(private router: Router) { }

  @HostListener('click', ['$event'])
  onClick(event: Event) {
    const target = event.target as HTMLAnchorElement;
    if (target.tagName.toLowerCase() === 'a' && target.href) {
      const url = target.getAttribute('href') || '';
      if (this.isRelativeUrl(url)) {
        event.preventDefault();
        this.router.navigate([url]);
      }
    }
  }

  private isRelativeUrl(url: string): boolean {
    // Check if the URL is relative (does not start with http://, https://, or //)
    return !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("//")
  }


}
