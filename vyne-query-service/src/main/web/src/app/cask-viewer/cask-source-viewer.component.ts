import { Component, OnInit, Input } from '@angular/core';
import { VersionedSource } from '../services/schema';

@Component({
  selector: 'app-cask-source-viewer',
  templateUrl: './cask-source-viewer.component.html',
  styleUrls: ['./cask-source-viewer.component.scss']
})
export class CaskSourceViewerComponent implements OnInit {

  constructor() { }

  @Input()
  sources: VersionedSource[]

  ngOnInit() {
  }

}
