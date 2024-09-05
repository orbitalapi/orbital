import {MarkedOptions, MarkedRenderer} from "ngx-markdown";
import {ApplicationRef, ComponentFactoryResolver, Injectable, Injector} from "@angular/core";
import {
  SchemaDiagramMarkdownWrapperComponent
} from "../schema-diagram-markdown-wrapper/schema-diagram-markdown-wrapper.component";

@Injectable({
  providedIn: 'root',
})
export class CustomMarkdownRenderer extends MarkedRenderer {
  constructor(
    private resolver: ComponentFactoryResolver,
    private injector: Injector,
    private appRef: ApplicationRef
  ) {
    super();
  }

  code(code: string, language: string, escaped: boolean):string {
    if (language === 'components') {
      // Create a unique identifier for the placeholder
      const id = `orb-diagram-${Math.random().toString(36).substr(2, 9)}`;
      setTimeout(() => this.replacePlaceholderWithComponent(id, code), 0);
      return `<div id="${id}">Diagram goes here</div>`
    } else {
      return super.code(code, language, escaped);
    }
  }

  replacePlaceholderWithComponent(id: string, code: string) {
    const factory = this.resolver.resolveComponentFactory(SchemaDiagramMarkdownWrapperComponent);
    const componentRef = factory.create(this.injector);
    componentRef.instance.code = code;

    this.appRef.attachView(componentRef.hostView);

    const domElem = (componentRef.hostView as any).rootNodes[0] as HTMLElement;
    const placeholder = document.getElementById(id);

    if (placeholder) {
      placeholder.replaceWith(domElem);
    }
  }
}
