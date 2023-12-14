import {Component, ContentChild, Input, OnInit, TemplateRef, ViewChild, ViewContainerRef} from '@angular/core';
import {TemplatePortal} from "@angular/cdk/portal";

@Component({
  selector: 'app-panel',
  template: `
    <ng-template><ng-content></ng-content></ng-template>
  `,
  styleUrls: ['./panel.component.scss']
})
export class PanelComponent implements OnInit {

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  @Input()
  icon: string;

  @Input()
  title: string;

  @ViewChild(TemplateRef, {static: true})
  implicitContent: TemplateRef<any>;


  contentPortal: TemplatePortal;

  ngOnInit(): void {
    this.contentPortal = new TemplatePortal<any>(this.implicitContent, this.viewContainerRef)
  }
}
