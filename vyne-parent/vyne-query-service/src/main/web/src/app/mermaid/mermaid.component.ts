import {AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';

import * as mermaid from 'mermaid';
import {log} from "util";

@Component({
  selector: 'mermaid',
  styleUrls: ['./mermaid.component.scss'],
  template: `
    <div #mermaidContainer></div>`
})
export class MermaidComponent implements OnInit, AfterViewInit {
  private _chartDef: string;
  get chartDef(): string {
    return this._chartDef;
  }

  private counter:number = 0;

  @Input()
  set chartDef(value: string) {
    if (this._chartDef == value) {
      return
    }
    this._chartDef = value;
    this.renderMermaid()
  }

  @ViewChild("mermaidContainer")
  mermaidContainer: ElementRef;

  ngOnInit() {
    mermaid.initialize({
      theme: "dark"
    });
    // mermaid.initialize({startOnLoad: false});
  }

  ngAfterViewInit(): void {
    if (this._chartDef) {
      this.renderMermaid()
    }
  }

  private renderMermaid() {
    mermaid.initialize({
      themeCSS: themeCSS
      // theme: "dark"
    });
    if (!this.mermaidContainer) {
      console.log("Not rendering mermaid - container not present yet");
      return;
    }

    if (!this.chartDef) {
      return;
    }

    let element = this.mermaidContainer.nativeElement;
    var insertSvg = function (svgCode, bindFunctions) {
      element.innerHTML = svgCode;
    };

    this.counter++;

    console.log("Generating mermaid for spec:");
    console.log(this.chartDef);
    var graph = mermaid.render('graphDiv' + this.counter, this._chartDef, insertSvg);
  }

}


const themeCSS = `
text.actor {
  font-size: 12px
}

.messageLine0 {
    stroke-width:1.5;
    stroke-dasharray: "2 2";
    marker-end:"url(#arrowhead)";
        stroke:black;
}

.messageLine1 {
    stroke-width:1.5;
    stroke-dasharray: "2 2";
    stroke:black;
}

#arrowhead {
        fill:black;
}

.messageText {
        fill:black;
    stroke:none;
    font-family: 'Nunito', verdana, arial;
    font-size:12px;
}

.labelText {
        fill:black;
    stroke:none;
    font-family: 'Work Sans', verdana, arial;
}

`
const input = `
sequenceDiagram
    participant John
    participant Alice
    Alice->>John: Hello John, how are you?
    John-->>Alice: Great!
`.trim();
