import {Directive, ElementRef, EventEmitter, Input, Output} from '@angular/core';

@Directive({
  selector: '[appTocHost]'
})
export class TocHostDirective {
  private _tocTag: string;

  @Input()
  get tocTag(): string {
    return this._tocTag;
  }

  @Output()
  contentsChanged: EventEmitter<Contents> = new EventEmitter();


  set tocTag(value: string) {
    this._tocTag = value;
    this.rebuildToc();
  }

  private mutationObserver: MutationObserver;

  constructor(private el: ElementRef) {
    const element = this.el.nativeElement;

    this.mutationObserver = new MutationObserver((mutations: MutationRecord[]) => {
        mutations.forEach((mutation: MutationRecord) => {
          this.rebuildToc();
        });
      }
    );

    this.mutationObserver.observe(element, {
      attributes: true,
      childList: true,
      characterData: true
    });

    this.rebuildToc();
  }

  private rebuildToc() {
    const items: ContentsItem[] = [];
    this.el.nativeElement.querySelectorAll(this._tocTag).forEach(node => {
      const title: string = node.textContent;
      const slug = title.toLowerCase().replace(' ', '-');
      node.id = slug;
      items.push({
        name: title,
        slug
      });
    });
    this.contentsChanged.emit({
      items
    });
  }
}

export interface Contents {
  items: ContentsItem[];
}

export interface ContentsItem {
  name: string;
  slug: string;
}
