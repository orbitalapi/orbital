import {NgModule} from '@angular/core';
import {SequenceDiagramComponent} from './sequence-diagram.component';
import {MermaidComponent} from './mermaid.component';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [CommonModule],
  exports: [SequenceDiagramComponent, MermaidComponent],
  declarations: [SequenceDiagramComponent, MermaidComponent],
  providers: [],
})
export class SequenceDiagramModule {
}
