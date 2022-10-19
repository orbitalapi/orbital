import { Directive, ElementRef, HostListener, Input, OnDestroy } from '@angular/core';

@Directive({
  selector: '[appTooltip]'
})
export class TooltipDirective implements OnDestroy {

  @Input() appTooltip = ''; // The text for the tooltip to display
  @Input() delay? = 190; // Optional delay input, in ms

  private myPopup;
  private timer;

  constructor(private el: ElementRef) {
    console.log('To')
  }

  ngOnDestroy(): void {
    if (this.myPopup) {
      this.myPopup.remove()
    }
  }

  @HostListener('mouseenter') onMouseEnter() {
    this.timer = setTimeout(() => {
      let x = this.el.nativeElement.getBoundingClientRect().left + this.el.nativeElement.offsetWidth / 2; // Get the middle of the element
      let y = this.el.nativeElement.getBoundingClientRect().top + this.el.nativeElement.offsetHeight + 6; // Get the bottom of the element, plus a little extra
      this.createTooltipPopup(x, y);
    }, this.delay)
  }

  @HostListener('mouseleave') onMouseLeave() {
    if (this.timer) clearTimeout(this.timer);
    if (this.myPopup) {
      this.myPopup.remove()
    }
  }

  private createTooltipPopup(x: number, y: number) {
    console.log('create')
    let popup = document.createElement('div');
    popup.innerHTML = this.appTooltip;
    popup.setAttribute('class', 'tooltip-container');
    popup.style.top = y.toString() + 'px';
    popup.style.left = x.toString() + 'px';
    document.body.appendChild(popup);
    this.myPopup = popup;
    setTimeout(() => {
      if (this.myPopup) this.myPopup.remove();
    }, 5000); // Remove tooltip after 5 seconds
  }

}
