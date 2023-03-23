import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-query-snippet-panel',
  template: `
    <tui-accordion>
      <tui-accordion-item *ngFor='let snippet of snippets' [open]='snippet.isDefault'>{{snippet.label}}
        <ng-template tuiAccordionItemContent>
          <td-highlight [codeLang]='snippet.language' [copyCodeToClipboard]='true'>{{snippet.snippet}}</td-highlight>
        </ng-template>
      </tui-accordion-item>
    </tui-accordion>
  `,
  styleUrls: ['./query-snippet-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuerySnippetPanelComponent {

  @Input()
  snippets: Snippet[];
}

export class Snippet {
  constructor(
    public readonly label: string,
    public readonly language: string,
    public readonly snippet: string,
    public readonly isDefault: boolean = false
  ) {
  }

}
