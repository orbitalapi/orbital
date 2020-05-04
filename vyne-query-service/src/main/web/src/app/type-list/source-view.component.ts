import {Component, Input, OnInit} from '@angular/core';
import {SchemaMember} from "../services/schema";

@Component({
  selector: 'type-source-view',
  templateUrl: './source-view.component.html',
  styleUrls: ['./source-view.component.scss']
})
export class SourceViewComponent implements OnInit {

  ngOnInit() {
  }

  @Input()
  schemaMember:SchemaMember;


}
