import {Component, EventEmitter, Inject, Input, Output} from '@angular/core';
import {TuiDialogService} from '@taiga-ui/core';
import {CodeSample, CodeSamples, ExampleGroups} from 'src/voyager-app/code-examples';
import {TuiStringHandler} from '@taiga-ui/cdk';
import {TUI_ARROW, TUI_PROMPT, tuiItemsHandlersProvider, TuiPromptData} from '@taiga-ui/kit';
import {StubQueryMessageWithSlug, StubQueryMessage} from "../../services/query.service";

const STRINGIFY_CODE_SAMPLE: TuiStringHandler<CodeSample> = (item: CodeSample) => item.title;

@Component({
  selector: 'playground-toolbar',
  templateUrl: './playground-toolbar.component.html',
  styleUrls: ['./playground-toolbar.component.scss'],
  providers: [tuiItemsHandlersProvider({stringify: STRINGIFY_CODE_SAMPLE})]
})
export class PlaygroundToolbarComponent {

  constructor(
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    ) {
  }

  readonly arrow = TUI_ARROW;

  examples = ExampleGroups;
  examplesOpen = false;

  @Output()
  selectedExampleChange = new EventEmitter<StubQueryMessageWithSlug>();

  @Output()
  generateShareUrl = new EventEmitter();

  @Output()
  clear = new EventEmitter();

  openNewSite(event: Event, url: string) {
    openNewSiteAndCancel(event, url)
  }

  showClearConfirmation() {
    const data: TuiPromptData = {
      content: 'This will clear your schema, query and any stubs you\'ve configured.',
      yes: 'OK',
      no: 'Cancel'
    }
    this.dialogService.open<boolean>(TUI_PROMPT, {
      label: 'Trash it all?',
      size: 's',
      data: data,
      dismissible: false,
      closeable: true,
    })
      .subscribe({
        next: value => {
          if (value) {
            this.clear.emit()
          }
        }
      })
  }
}


export function openNewSiteAndCancel(event: Event, url: string) {
  event.preventDefault()
  event.stopImmediatePropagation();
  event.stopPropagation();
  window.open(url, '_blank');
}
