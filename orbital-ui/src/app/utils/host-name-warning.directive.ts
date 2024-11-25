import {
  Directive,
  ElementRef,
  HostListener,
  Renderer2,
} from '@angular/core';

@Directive({
  selector: '[appHostNameWarning]',
  standalone: true,
})
export class HostNameWarningDirective {
  private warningBox: HTMLElement | null = null;

  constructor(private el: ElementRef, private renderer: Renderer2) {}

  @HostListener('input', ['$event.target.value'])
  onInputChange(value: string): void {
    if (value === 'localhost' || value === '127.0.0.1') {
      this.showWarning();
    } else {
      this.removeWarning();
    }
  }

  private showWarning(): void {
    if (!this.warningBox) {
      this.warningBox = this.renderer.createElement('div');
      this.warningBox.className = 'host-warning';
      this.warningBox.textContent =
        'Docker users: When connecting to services on your host machine (Mac/Windows), don\'t use localhost or 127.0.0.1 - these point to your container, not your computer. Instead, use host.docker.internal to reach your local machine.';
      this.renderer.setStyle(this.warningBox, 'backgroundColor', '#ffc7001f');
      this.renderer.setStyle(this.warningBox, 'borderRadius', '0.5rem');
      this.renderer.setStyle(this.warningBox, 'margin', '-0.25rem 0 1.25rem 0');
      this.renderer.setStyle(this.warningBox, 'padding', '0.5rem 1rem');
      this.renderer.setStyle(this.warningBox, 'fontSize', '1rem');
      this.renderer.setStyle(this.warningBox, 'lineHeight', '1.3rem');
      this.renderer.appendChild(this.el.nativeElement.parentNode, this.warningBox);
    }
  }

  private removeWarning(): void {
    if (this.warningBox) {
      this.renderer.removeChild(this.el.nativeElement.parentNode, this.warningBox);
      this.warningBox = null;
    }
  }
}
