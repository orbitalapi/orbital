import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-push-schema-config-panel',
  templateUrl: './push-panel.component.html',
  styleUrls: ['./push-panel.component.scss']
})
export class PushSchemaConfigPanelComponent {
  pushMethod: 'cd-pipeline' | 'application-push' = 'cd-pipeline'

  pushMethods: string[] = [
    'In a CI / CD pipeline',
    'From '
  ]

}
