import {AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';

import * as mermaid from 'mermaid';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'mermaid',
  styleUrls: ['./mermaid.component.scss'],
  template: `
    <div class="mermaid-diagram" #mermaidContainer></div>`
})
export class MermaidComponent implements OnInit, AfterViewInit {
  private _chartDef: string;
  get chartDef(): string {
    return this._chartDef;
  }

  private counter = 0;

  @Input()
  set chartDef(value: string) {
    if (this._chartDef === value) {
      return;
    }
    this._chartDef = value;
    this.renderMermaid();
  }

  @ViewChild('mermaidContainer', {static: true})
  mermaidContainer: ElementRef;

  ngOnInit() {
    mermaid.initialize({
      themeCSS: themeCSS
    });
    // mermaid.initialize({startOnLoad: false});
  }

  ngAfterViewInit(): void {
    if (this._chartDef) {
      this.renderMermaid();
    }
  }

  private renderMermaid() {
    // mermaid.sequenceConfig = {
    //   actorFontFamily: '"DM Sans", "nunito-sans", "sans-serif',
    //   messageFontFamily: '"nunito-sans", "sans-serif"'
    // }
    mermaid.initialize({
      themeCSS: themeCSS,
      // Docs suggest this should be mermaid.sequenceConfig -- but that doesn't work
      sequence: {
        useMaxWidth: false,
        actorFontFamily: '"DM Sans", "nunito-sans", "sans-serif',
        messageFontFamily: '"nunito-sans", "sans-serif"'
      }

      // theme: "dark"
    });
    if (!this.mermaidContainer) {
      console.log('Not rendering mermaid - container not present yet');
      return;
    }

    if (!this.chartDef) {
      return;
    }

    const element = this.mermaidContainer.nativeElement;
    const insertSvg = function (svgCode, bindFunctions) {
      element.innerHTML = svgCode;
    };

    this.counter++;
    const graph = mermaid.render('graphDiv' + Date.now(), this._chartDef, insertSvg);
  }

}

const color2 = '#274060';
const themeCSS = `
text.actor {
  font-size: 13px
  font-family: 'nunito-sans', verdana, arial;
}

.actor {
  fill: #ffffff;
  stroke: #4bb04f;
  stroke-width:1.5;
}

.messageLine0 {
    stroke-width:1.5;
    stroke-dasharray: "2 2";
    marker-end:"url(#arrowhead)";
        stroke:${color2};
}

.messageLine1 {
    stroke-width:1.5;
    stroke-dasharray: "2 2";
    stroke:#d5d9ea;
}

#arrowhead {
        fill:${color2};
}

.messageText {
        fill:${color2};
    stroke:none;
    font-family: 'nunito-sans', verdana, arial;
    font-size:14px;
}

`;
