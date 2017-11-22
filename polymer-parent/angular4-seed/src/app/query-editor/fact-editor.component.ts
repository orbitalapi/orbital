import { OnInit, Component, Input } from "@angular/core";
import { FormControl } from "@angular/forms";
import { Observable } from "rxjs/Observable";
import { Type, Schema, TypeReference } from "app/common-api/types.service";
import { Fact } from "app/query-editor/query-editor.component";
import { MatAutocompleteSelectedEvent } from "@angular/material";
import { ITdDynamicElementConfig, TdDynamicType } from "@covalent/dynamic-forms";
import { MaterialLabDbService } from "app/shared/data/MaterialLabDb.service";



@Component({
   selector: 'fact-editor',
   templateUrl: './fact-editor.component.html',
   styleUrls: ['./fact-editor.component.scss']
})
export class FactEditorComponent implements OnInit {
   // Utility function for iterating keys in an object in an *ngFor
   objectKeys = Object.keys

   private typeByName:Map<String,Type>
   @Input() types: Array<Type>
   @Input() fact: Fact
   filteredTypes: Observable<Type[]>
   selectedType:Type

   typeSelectorCtrl: FormControl
   // inputElements: ITdDynamicElementConfig[] = []
   nodes:TreeNodeData[] = [];

   factAttributes() : Array<string> {
      return (this.fact && this.fact.value) ? Object.keys(this.fact.value) : [];
   }
   ngOnInit(): void {
      // let schemas = new MaterialLabDbService().createDb().types.types as Type[]
      this.typeSelectorCtrl = new FormControl()
      this.filteredTypes = this.typeSelectorCtrl.valueChanges
         .startWith(null)
         .map(input => input ? this.filterTypes(input) : this.types.slice())

      this.typeByName = new Map();
      this.types.forEach( value => this.typeByName[value.name.fullyQualifiedName] = value)

   }

   onOptionSelected(event:MatAutocompleteSelectedEvent):void {
      let selectedTypename = event.option.value

      this.updateFact(selectedTypename)
      this.fact.typeName = selectedTypename
   }


   filterTypes(name: string) {
      return this.types.filter(type =>
         type.name.fullyQualifiedName.toLowerCase().indexOf(name.toLowerCase()) !== -1);
   }

   updateFact(selectedTypename: string): any {
      this.selectedType = this.typeByName[selectedTypename]
      this.fact.typeName = selectedTypename
      let factValue:any = {}
      Object.keys(this.selectedType.attributes).forEach( attributeName => {
         factValue[attributeName] = null
      })
      this.fact.value = factValue

      this.nodes = this.buildDynamicFormElements(this.selectedType, "$", "Value")
      // this.inputElements = this.buildDynamicFormElements(this.selectedType)
   }

   scalarTypeToTreeNode(path:string, attributeName:string, scalarType:Type):TreeNodeData {
         let rootType = this.getRootType(scalarType)
      return new TreeNodeData(
         path + "." + attributeName,
         attributeName,
         [],
         this.buildFormControlForType(attributeName,scalarType)
      )
         }

   fooFun(node) {
      console.log("foo fun")
   }


   buildDynamicFormElements(type:Type, path:string, typeAttributeName:string):Array<TreeNodeData> {


      if (type.scalar) {
         return [ this.scalarTypeToTreeNode(path, typeAttributeName, type) ]
      }


      let nodes = Object.keys(type.attributes).map( attributeName => {
         let attribute = type.attributes[attributeName] as TypeReference
         let attributeType = this.typeByName[attribute.fullyQualifiedName] as Type
         if (attributeType == null) throw new Error("Type `${attribute.fullyQualifiedName}` - as defined by param `${attributeName}` on type `${type.name.fullyQualifiedName}` is not found")

         if (attributeType.scalar) {
            return this.scalarTypeToTreeNode(path, attributeName, attributeType)
         } else {
            // We need a nested component editor
            return new TreeNodeData(
               path + "." + attribute,
               attributeName,
               this.buildDynamicFormElements(attributeType, path + "." + attributeName, attributeName),
               null
            )
         }
      })

      return nodes;
   }

   private buildFormControlForType(attributeName:string,type:Type):ITdDynamicElementConfig {
      return {
         label: attributeName,
         name: attributeName,
         type: TdDynamicType.Text, // TODO: use the type
         required: false, // TODO : This is knowable from the type system
         // min?: any;
         // max?: any;
         // minLength?: string;
         // maxLength?: string;
         // selections?: any[];
         // default?: any;
         // validators?: ITdDynamicElementValidator[]
      }
   }

   private getRootType(type:Type):Type {
      if (type.aliasForType != null) {
         let aliasedType = this.typeByName[type.aliasForType.fullyQualifiedName]
         if (aliasedType == null) {
            throw Error("Type " + type.name + " is an alias for type " + type.aliasForType + " which is unknown")
         }
         return this.getRootType(this.typeByName[type.aliasForType.fullyQualifiedName])
      } else {
         return type
      }
   }
}


class TreeNodeData {

   constructor(
      public id:any,
      public name:string,
      public children:Array<TreeNodeData>,
      public dataControl:ITdDynamicElementConfig) {}

   dataControlArray():ITdDynamicElementConfig[] {
      return this.dataControl ? [this.dataControl] : [];
   }
}
