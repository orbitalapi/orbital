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
import { MatSidenav } from "@angular/material";
import { DataService } from "../shared/services/data/data.service";
import * as _ from "lodash";
import { TypesService, QualifiedName, Service, Type, SourceCode, Operation } from "app/common-api/types.service";
import {colorSets} from '@swimlane/ngx-charts-dag/src/utils'

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

   members:SchemaMember[] = [];

   // Can't seem to use enums in an ng-if.  Grrr.
   serviceType = SchemaMemberType.SERVICE
   typeType = SchemaMemberType.TYPE
   operationType = SchemaMemberType.OPERATION

   constructor(
      public config: ConfigService,
      private _elementRef: ElementRef,
      private _state: GlobalState,
      private service: TypesService
   ) {


   }

   // Utility function for iterating keys in an object in an *ngFor
   objectKeys = Object.keys

   attributes(member:SchemaMember):string[] {
      if (member.kind == SchemaMemberType.TYPE) {
         return Object.keys((member.member as Type).attributes)
      } else {
         return []
      }
   }

   ngOnInit() {
      this.service.getTypes().subscribe(
         res => {
            let schema = res as any
            let typeMembers:SchemaMember[] = schema.types.map((t) => SchemaMember.fromType(t as Type))
            let operationMembers:SchemaMember[] = [];
            schema.services.forEach((service) => operationMembers = operationMembers.concat(SchemaMember.fromService(service as Service)))
            let members:SchemaMember[] = typeMembers.concat(operationMembers)
            members = _.sortBy(members, [(m:SchemaMember) => { return m.name.fullyQualifiedName}])
            this.schema = schema
            this.members = members
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

   loadTypeLinks(member:SchemaMember) {
      console.log("Load type links for `${member}`")
   }
}

export class SchemaMember {
   constructor(
      public readonly name: QualifiedName,
      public readonly kind: SchemaMemberType,
      public readonly aliasForType: string,
      public readonly member:Type | Service | Operation,
      public readonly sources:SourceCode[]
   ) {
      this.attributeNames = kind == SchemaMemberType.TYPE
         ? Object.keys((member as Type).attributes)
         : []
   }

   attributeNames:string[]
   static fromService(service: Service): SchemaMember[] {
      return service.operations.map( operation => {
         return new SchemaMember(
            {name: operation.name, fullyQualifiedName: service.name.fullyQualifiedName + " #" + operation.name},
            SchemaMemberType.OPERATION,
            null,
            operation,
            [] // TODO
         )
      })

   }

   static fromType(type:Type):SchemaMember {
      return new SchemaMember(
         type.name,
         SchemaMemberType.TYPE,
         (type.aliasForType) ? type.aliasForType.fullyQualifiedName : null,
         type,
         type.sources
      )
   }
}

export enum SchemaMemberType {
   SERVICE,
   TYPE,
   OPERATION
}
