import {Component, Inject, OnInit} from '@angular/core';
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";
import {TuiDialogContext} from "@taiga-ui/core";
import {UiCustomisations} from "../../environments/ui-customisations";

@Component({
  selector: 'app-add-token-panel',
  template: `
      <h2>Add a new authentication token</h2>
      <div>
          <p>Authentication tokens are defined in your Taxi projects.</p>
          <p>Click to learn more about how to <a
                  [href]="UiCustomisations.docsLinks.authenticationToServices" target="_blank">add
              authentication tokens</a>, or how <a [href]="UiCustomisations.docsLinks.managingSecrets"
                                                   target="_blank">secrets are managed</a> in our docs.</p>
      </div>
      <div class="form-buttons">
          <div class="spacer"></div>
          <button tuiButton type="button"
                  appearance="primary" size="m" (click)="cancel()">Close
          </button>
      </div>

  `,

  styleUrls: ['./add-token-panel.component.scss']
})
export class AddTokenPanelComponent {

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly dialogContext: TuiDialogContext<void>) {
  }

  cancel(): void {
    this.dialogContext.completeWith()
  }

  protected readonly UiCustomisations = UiCustomisations;
}
