import { Component, ViewEncapsulation, ViewChild, OnInit, HostListener, ElementRef} from '@angular/core';
import {GlobalState} from '../../../app.state';
import { ConfigService } from '../../../shared/services/config/config.service';
import { MdSidenav } from "@angular/material";

@Component({
  selector: '.content_inner_wrapper',
  templateUrl: './detached-toolbar-left.component.html',
  styleUrls: ['./detached-toolbar-left.component.scss'],
  encapsulation: ViewEncapsulation.Emulated,
})
export class DetachedToolbarLeftComponent implements OnInit {

  constructor(public config: ConfigService, private _elementRef: ElementRef, private _state: GlobalState) {

  }

	ngOnInit() {

		
    }


}
