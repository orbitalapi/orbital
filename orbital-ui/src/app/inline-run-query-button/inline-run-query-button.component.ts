import { TuiButton } from "@taiga-ui/core";
import {
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  EventEmitter,
  NgZone, OnInit,
  Output,
  ViewContainerRef
} from '@angular/core';
import {SvgIconComponent} from "../svg-icon/svg-icon.component";
import Prism from 'prismjs'
import 'prismjs/plugins/toolbar/prism-toolbar';

let componentInstalled = false;

@Component({
  selector: 'app-inline-run-query-button',
  standalone: true,
  imports: [
    TuiButton,
    SvgIconComponent
  ],
  template: `
    <button class="inline-run-query-button" tuiButton size="s" appearance="outline-grayscale" type="button"
            (click)="clicked.emit()">
      <app-svg-icon tabler="player-play" strokeWidth="3"></app-svg-icon>
      <span class="label">{{ label }}</span>
    </button>
  `,
  styleUrl: './inline-run-query-button.component.scss'
})
export class InlineRunQueryButtonComponent {
  @Output() clicked = new EventEmitter<void>();

  constructor(private changeDetectorRef: ChangeDetectorRef) {
    setTimeout(() => {
      // For some reason, this is getting mounted outside of Angular lifecycle,
      // so change detection and lifecycle events are getting called.
      // Can't work out why.
      // However, it's a simple component, so just force change detection on startup.
      this.changeDetectorRef.detectChanges();
    })
  }

  label: string = 'Run query'

  static install(viewContainerRef: ViewContainerRef,
                 clickHandler: (code: string) => void,
                 ngZone: NgZone
  ) {
    if (componentInstalled) {
      return;
    }
    Prism.plugins.toolbar.registerButton('run-code', (env) => {
      if (env.language === 'taxiql') {
        const container = document.createElement('div');
        ngZone.run(() => {
          setTimeout(() => {
            const componentRef = viewContainerRef.createComponent(InlineRunQueryButtonComponent);

            // Subscribe to the @Output event
            componentRef.instance.clicked.subscribe(() => {
              clickHandler(env.code);
            });
            container.appendChild(componentRef.location.nativeElement);
          })
        })

        return container;
      } else {
        return null
      }
    });
    componentInstalled = true;
  }
}
