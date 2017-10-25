import {
  Component,
  ViewEncapsulation,
  ViewChild,
  OnInit,
  HostListener,
  ElementRef,
  HostBinding
} from "@angular/core";
import { GlobalState } from "../../app.state";
import { ConfigService } from "../../shared/services/config/config.service";
import { MdSidenav } from "@angular/material";
import { DataService } from "../../shared/services/data/data.service";

@Component({
  moduleId: module.id,
  selector: ".content_inner_wrapper",
  templateUrl: "./mail.component.html",
  styleUrls: ["./mail.component.scss"]
})
export class MailComponent implements OnInit {
  @ViewChild("leftSidenav2") leftSidenav2: MdSidenav;
  navMode = "side";
  displayMode: string = "default";
  multi: boolean = false;
  hideToggle: boolean = false;
  isFocused: boolean = false;
  highlight: boolean = false;
  selectedAll: any;
  mail: any;
  term: any;
  selected: boolean = false;
  checked: boolean = false;
  isComposeActive: boolean = false;
  open: boolean = false;
  spin: boolean = false;
  fixed: boolean = false;
  direction: string = "up";
  animationMode: string = "fling";
  constructor(
    public config: ConfigService,
    private _elementRef: ElementRef,
    private _state: GlobalState,
    private _DataService: DataService
  ) {

  }
  selectAll() {
    for (var i = 0; i < this.mail.length; i++) {
      this.mail[i].selected = this.selectedAll;
    }
    for (var i = 0; i < this.mail.length; i++) {
      if (this.mail[i].selected == true) {
        this.checked = true;
        return;
      } else {
        this.checked = false;
      }
    }
  }
  checkIfAllSelected() {
    for (var i = 0; i < this.mail.length; i++) {
      if (this.mail[i].selected == true) {
        this.checked = true;
        return;
      } else {
        this.checked = false;
      }
    }
    this.selectedAll = this.mail.every(function(item: any) {
      return item.selected == true;
    });
  }
  ngOnInit() {
    this._DataService.getMailDemo().subscribe(
      res => {
				(this.mail = res),
				(this.mail = this.mail.data)
      },
      error => console.log("error : " + error)
    );
    if (window.innerWidth < 992) {
      this.navMode = "over";
      this.leftSidenav2.opened = false;
    }
    if (window.innerWidth > 992) {
      this.navMode = "side";
      this.leftSidenav2.open();
    }
  }
  onMailChecked(event) {
    event.stopPropagation();

  }
  _click(event: any) {
    //console.log(event);
  }
  @HostListener("window:resize", ["$event"])
  onResize(event) {
    if (event.target.innerWidth < 992) {
      this.navMode = "over";
      this.leftSidenav2.close();
    }
    if (event.target.innerWidth > 992) {
      this.navMode = "side";
      this.leftSidenav2.open();
    }
  }
}
