import {Component, EventEmitter, Inject, Injector, Input, Output} from '@angular/core';
import {AuthService} from '@auth0/auth0-angular';
import {TuiDialogService} from '@taiga-ui/core';
import {SubscribeDialogComponent} from '../subscribe-dialog/subscribe-dialog.component';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {CodeSample, CodeSamples} from 'src/voyager-app/code-examples';
import {TuiStringHandler} from '@taiga-ui/cdk';
import {tuiItemsHandlersProvider} from '@taiga-ui/kit';
import {PLAUSIBLE_ANALYTICS} from 'src/voyager-app/plausible';

const STRINGIFY_CODE_SAMPLE: TuiStringHandler<CodeSample> = (item: CodeSample) => item.title;

@Component({
  selector: 'playground-toolbar',
  templateUrl: './voyager-toolbar.component.html',
  styleUrls: ['./voyager-toolbar.component.scss'],
  providers: [tuiItemsHandlersProvider({stringify: STRINGIFY_CODE_SAMPLE})]
})
export class VoyagerToolbarComponent {

  readonly slackInviteLink = 'https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg';

  constructor(
    public authService: AuthService,
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(PLAUSIBLE_ANALYTICS) private plausible: any) {
  }

  examples = CodeSamples;

  @Input()
  selectedExample: CodeSample = this.examples[0];

  @Output()
  selectedExampleChange = new EventEmitter<CodeSample>();

  @Output()
  generateShareUrl = new EventEmitter();

  slackInvite() {
    this.plausible.trackEvent('visit slack invite');
    window.open(this.slackInviteLink);
  }

  showSubscribeDialog = false;

  subscribeForUpdates() {
    this.plausible.trackEvent('visit subscribe dialog');
    this.dialog.subscribe({
      next: data => {
        console.info(`Dialog emitted data = ${data}`);
      },
      complete: () => {
        console.info(`Dialog closed`);
      },
    });
  }

  private readonly dialog = this.dialogService.open(
    new PolymorpheusComponent(SubscribeDialogComponent, this.injector));

  setExample(value: CodeSample) {
    this.selectedExample = value;
    this.selectedExampleChange.next(value);
  }

  openNewSite(url: string) {
    let newWindow = window.open();
    newWindow.opener = null;
    // @ts-ignore says string can't be assigned here, it's wrong
    newWindow.location = url;
  }
}
