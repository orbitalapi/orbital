import { Directive, ElementRef, Renderer2, AfterViewInit, HostListener, AfterViewChecked } from '@angular/core';

@Directive({
  standalone: true,
  selector: "[app-ellipsis]"
})
export class EllipsisDirective implements AfterViewInit, AfterViewChecked {
  private isTruncated: boolean = false;

  constructor(private el: ElementRef, private renderer: Renderer2) {}

  ngAfterViewInit() {
    // Apply truncation styles after the view initializes
    this.applyEllipsis();
  }

  ngAfterViewChecked() {
    // Ensure the element is correctly measured and truncated after each view check
    this.checkTruncation();
  }

  @HostListener('window:resize')
  onResize() {
    this.checkTruncation();
  }

  private applyEllipsis() {
    const nativeElement = this.el.nativeElement;

    // Apply CSS styles to enable truncation
    this.renderer.setStyle(nativeElement, 'overflow', 'hidden');
    this.renderer.setStyle(nativeElement, 'white-space', 'nowrap');
    this.renderer.setStyle(nativeElement, 'text-overflow', 'ellipsis');
    this.renderer.setStyle(nativeElement, 'display', 'block');
  }

  private checkTruncation() {
    const nativeElement = this.el.nativeElement;

    // Use requestAnimationFrame to ensure the DOM is fully rendered
    requestAnimationFrame(() => {
      const isCurrentlyTruncated = nativeElement.scrollWidth > nativeElement.offsetWidth;

      if (isCurrentlyTruncated && !this.isTruncated) {
        // Text is truncated, set the full text as the tooltip
        this.renderer.setAttribute(nativeElement, 'title', nativeElement.textContent.trim());
        this.isTruncated = true;
      } else if (!isCurrentlyTruncated && this.isTruncated) {
        // Text is no longer truncated, remove the tooltip
        this.renderer.removeAttribute(nativeElement, 'title');
        this.isTruncated = false;
      }
    });
  }
}
