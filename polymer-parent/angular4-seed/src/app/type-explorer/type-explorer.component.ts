import {
   Component,
   ViewEncapsulation,
   ViewChild,
   OnInit,
   HostListener,
   ElementRef,
   HostBinding
} from "@angular/core";
import { GlobalState } from "../app.state";
import { ConfigService } from "../shared/services/config/config.service";
import { MdSidenav } from "@angular/material";
import { DataService } from "../shared/services/data/data.service";
import { TypeExplorerService } from "app/type-explorer/type-explorer.service";
import * as _ from "lodash";

@Component({
   moduleId: module.id,
   selector: ".content_inner_wrapper",
   templateUrl: "./type-explorer.component.html",
   styleUrls: ["./type-explorer.component.scss"]
})
export class TypeExplorerComponent implements OnInit {
   navMode = "side";
   displayMode: string = "default";
   multi: boolean = false;
   hideToggle: boolean = false;
   isFocused: boolean = false;
   highlight: boolean = false;
   selectedAll: any;
   schema: any;
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
      private service: TypeExplorerService
   ) {

   }

   // Utility function for iterating keys in an object in an *ngFor
   objectKeys = Object.keys

   selectAll() {
      for (var i = 0; i < this.schema.length; i++) {
         this.schema[i].selected = this.selectedAll;
      }
      for (var i = 0; i < this.schema.length; i++) {
         if (this.schema[i].selected == true) {
            this.checked = true;
            return;
         } else {
            this.checked = false;
         }
      }
   }
   checkIfAllSelected() {
      for (var i = 0; i < this.schema.length; i++) {
         if (this.schema[i].selected == true) {
            this.checked = true;
            return;
         } else {
            this.checked = false;
         }
      }
      this.selectedAll = this.schema.every(function (item: any) {
         return item.selected == true;
      });
   }
   ngOnInit() {
      this.service.getTypes().subscribe(
         res => {
            let schema = res as any
            schema.types = _.sortBy(schema.types,  [( t ) => { return t.name.fullyQualifiedName}] )
            this.schema = schema
         },
         error => console.log("error : " + error)
      );
      if (window.innerWidth < 992) {
         this.navMode = "over";
      }
      if (window.innerWidth > 992) {
         this.navMode = "side";
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
      }
      if (event.target.innerWidth > 992) {
         this.navMode = "side";
      }
   }
}
