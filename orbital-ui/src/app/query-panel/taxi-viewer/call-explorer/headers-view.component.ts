import { TuiAccordion } from "@taiga-ui/kit";
import {Component, Input} from '@angular/core';
import { CommonModule } from '@angular/common';
import {HttpExchangeHeaders} from "../../../services/query.service";

@Component({
  selector: 'app-headers-view',
  standalone: true,
  imports: [CommonModule, TuiAccordion],
  template: `
    <tui-accordion [closeOthers]="false" [rounded]="false">
      <tui-accordion-item size="s" [open]="true">
        Request headers
        <ng-template tuiAccordionItemContent>
          <table>
            <tr *ngFor="let header of headers.requestHeaders | keyvalue">
              <td class="header-key">{{header.key}}</td>
              <td><div *ngFor="let headerValue of header.value">{{headerValue}}</div></td>
            </tr>
          </table>
        </ng-template>
      </tui-accordion-item>
      <tui-accordion-item size="s" [open]="true">
        Response headers
        <ng-template tuiAccordionItemContent>
          <table>
            <tr *ngFor="let header of headers.responseHeaders | keyvalue">
              <td class="header-key">{{header.key}}</td>
              <td><div *ngFor="let headerValue of header.value">{{headerValue}}</div></td>
            </tr>
          </table>
        </ng-template>
      </tui-accordion-item>

    </tui-accordion>
  `,
  styleUrls: ['./headers-view.component.scss']
})
export class HeadersViewComponent {

  @Input()
  headers: HttpExchangeHeaders


}
