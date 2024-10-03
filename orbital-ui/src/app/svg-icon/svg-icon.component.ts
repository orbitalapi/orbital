import {ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, input, Input, signal} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {SafePipe} from "../safe.pipe";
import {NgStyle} from "@angular/common";

/**
 * Displays configurable svg, loaded from a known url.
 * Unlike the svg-icon in taiga, doesn't require pre-registration of the svg.
 *
 * We expect this to be safe html - so don't load anything stupid here.
 *
 * Use for loading SVG from our assets folders.
 *
 * - Colors get inherited (set by using color: red in the css)
 * - Tweak width / height / stroke-width by setting properties
 */
@Component({
  selector: 'app-svg-icon',
  standalone: true,
  imports: [
    SafePipe,
    NgStyle
  ],
  template: `
    <div [style.width]="widthCss()" [style.height]="heightCss()" [innerHTML]="svgContent() | safe: 'html'"></div>
  `,
  styleUrl: './svg-icon.component.scss',
})
export class SvgIconComponent {

  constructor(private http: HttpClient, private changeDetectorRef: ChangeDetectorRef) {

  }


  width = input(16)
  height = input(16)

  widthCss = computed(() => `${this.width()}px`)
  heightCss = computed(() => `${this.height()}px`)

  strokeWidth = input(1.5)
  private rawSvg = signal<string>('');

  svgContent = computed(() => this.updateSvg(this.rawSvg(), this.width(), this.height(), this.strokeWidth()))

  private _src: string;
  @Input()
  get src(): string {
    return this._src
  }

  set src(value) {
    if (this._src !== value) {
      this._src = value;
      this.load(value)
    }
  }

  /**
   * Convenience helper.
   * Set the name of a tabler icon (without the .svg)
   * Will set the icon with the correct prefix
   * @param value
   */
  @Input()
  set tabler(value: string) {
    this.src = `/assets/img/tabler/${value}.svg`
  }

  private load(src: string) {
    this.http.get(src, {responseType: 'text'}).subscribe(svg => {
        this.rawSvg.set(svg);
        // Don't use markForCheck(), as this gets used in a component
        // (InlineRunQueryButtonComponent)
        // that is failing to registered with lifecycle hooks.
        // So, force change detection manually here.
        this.changeDetectorRef.detectChanges();
      },
      err => {
        console.log(`Error loading ${src}: ${err}`);
      });
  }

  private updateSvg(rawSvg: string, width: number, height: number, strokeWidth: number) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(rawSvg, 'image/svg+xml');
    const svgElement = doc.querySelector('svg');

    if (svgElement) {
      // Set or update the width and height attributes
      svgElement.setAttribute('width', width.toString());
      svgElement.setAttribute('height', height.toString());
      svgElement.setAttribute('stroke-width', strokeWidth.toString());

      // Serialize the DOM object back to an SVG string
      const serializer = new XMLSerializer();
      return serializer.serializeToString(svgElement);
    }
    // Return original string if unable to parse
    return rawSvg;
  }
}
