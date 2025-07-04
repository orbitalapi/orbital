import {MarkdownService, MarkedOptions, MarkedRenderer} from "ngx-markdown";
import {ApplicationRef, ComponentFactoryResolver, Injectable, Injector} from "@angular/core";
import {
  SchemaDiagramMarkdownWrapperComponent
} from "../schema-diagram-markdown-wrapper/schema-diagram-markdown-wrapper.component";
import {isNullOrUndefined} from "../utils/utils";

@Injectable({
  providedIn: 'root',
})
export class CustomMarkdownRenderer {
  constructor(
    private resolver: ComponentFactoryResolver,
    private injector: Injector,
    private appRef: ApplicationRef,
  ) {}

  code(code: string, language: string, escaped: boolean, delegateFunction):string {
    if (language === 'components' || language === 'schemaDiagram') {
      // Create a unique identifier for the placeholder
      const id = `orb-diagram-${Math.random().toString(36).substr(2, 9)}`;
      setTimeout(() => this.replacePlaceholderWithComponent(id, code), 0);
      return `<div id="${id}" style="
    padding: 1rem;
    border: 1px solid red;
    border-radius: 4px;
    margin-bottom: 1rem;
">An architecture diagram was supposed to appear here, but didn't. How bitterly disappointing.</div>`
    } else {
      return delegateFunction(code, language, escaped);
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

  installFor(markdownService: MarkdownService) {
    const delegateCodeFunction = markdownService.renderer.code;
    if (delegateCodeFunction === this.code) {
      return;
    }
    markdownService.renderer.code = (code: string, language: string, escaped: boolean): string => {
      return this.code(code, language, escaped, delegateCodeFunction)
    }
  }
}
