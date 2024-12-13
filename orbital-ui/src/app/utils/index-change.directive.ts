import {VIRTUAL_SCROLL_STRATEGY} from '@angular/cdk/scrolling';
import {Directive, inject, Output} from '@angular/core';

// Taken from https://taiga-ui.dev/components/combo-box
@Directive({
  standalone: true,
  selector: '[indexChange]',
})
export class IndexChange {
  @Output()
  public readonly indexChange = inject(VIRTUAL_SCROLL_STRATEGY).scrolledIndexChange;
}
