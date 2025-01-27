import { TuiSegmented } from "@taiga-ui/kit";
import { TuiButton } from "@taiga-ui/core";
import {
  Component,
  ComponentFactoryResolver,
  EventEmitter,
  Input,
  NgZone,
  Output,
  ViewContainerRef
} from '@angular/core';
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {MarkdownComponent, MarkdownService, MARKED_OPTIONS, provideMarkdown} from "ngx-markdown";
import {CustomMarkdownRenderer} from "../../markdown-utils/markdown-custom-renderer";
import markedAlert from "marked-alert";
import {NgIf} from "@angular/common";
import {SimpleCodeEditorComponent} from "../../simple-code-editor/simple-code-editor.component";
import {taxi} from "../../utils/prism.languages";
import {InlineRunQueryButtonComponent} from "../../inline-run-query-button/inline-run-query-button.component";
import Prism from 'prismjs'
@Component({
  selector: 'app-readme-panel',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    TuiSegmented,
    TuiButton,
    MarkdownComponent,
    NgIf,
    SimpleCodeEditorComponent,
  ],
  providers: [
    CustomMarkdownRenderer,
    provideMarkdown(
      {
        markedExtensions: [markedAlert()],
      },
    )
  ],
  template: `
    <app-panel-header tablerIcon="book" title="Readme">
      <div class="spacer"></div>
      <tui-segmented size="s" [(activeItemIndex)]="viewModeActiveIndex" class="dark">
        <button [class.active]="viewModeActiveIndex === 0">Preview</button>
        <button [class.active]="viewModeActiveIndex === 1">Code</button>
      </tui-segmented>
    </app-panel-header>

    <div class="markdown-container" *ngIf="viewModeActiveIndex === 0">
      <div *ngIf="!markdown" class="empty-state">
        <p>There's no README here. Try adding one in by switching to the code view.</p>
        <p>Markdown is supported (including Github flavoured markdown goodies).</p>
      </div>
      <markdown *ngIf="markdown" [data]="markdown" [disableSanitizer]="true" class="markdown-body"/>
    </div>
    <app-simple-code-editor *ngIf="viewModeActiveIndex === 1" [(content)]="markdown"></app-simple-code-editor>


  `,
  styleUrl: './readme-panel.component.scss'
})
export class ReadmePanelComponent {

  constructor(
    markdownService: MarkdownService,
    customMarkdownRenderer: CustomMarkdownRenderer,
    viewContainerRef: ViewContainerRef,
    zone: NgZone
  ) {
    Prism.languages.taxi = taxi
    Prism.languages.taxiql = taxi

    InlineRunQueryButtonComponent.install(viewContainerRef, (code) => this.onRunQueryClicked(code), zone)
    customMarkdownRenderer.installFor(markdownService)
  }

  private onRunQueryClicked(query:string) {
    console.log(query);
    this.onRunQuery.emit(query)
  }

  @Input()
  get markdown(): string {
    return this._markdown;
  }

  set markdown(value: string) {
    if (value !== this._markdown) {
      this._markdown = value;
      this.markdownChange.emit(this.markdown);
    }
  }

  viewModeActiveIndex = 0

  @Output()
  markdownChange = new EventEmitter<string>();

  @Output()
  onRunQuery = new EventEmitter<string>()

  private _markdown: string

  resetViewMode() {
    this.viewModeActiveIndex = 0;
  }
}
