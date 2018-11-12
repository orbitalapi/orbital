import {Component, Input, OnInit} from '@angular/core';
import {Metadata, Operation, Parameter, SchemaMember, Type} from "../services/types.service";

@Component({
  selector: 'parameter-view',
  templateUrl: './parameter-view.component.html',
  styleUrls: ['./parameter-view.component.scss']
})
export class ParameterViewComponent implements OnInit {

  displayedColumns: string[] = ["inputOutput", "name1", "type"];

  @Input()
  schemaMember: SchemaMember;

  get operation(): Operation {
    if (!this.schemaMember) return null;
    return (<Operation>this.schemaMember.member)
  }

  get members(): SignatureMember[] {
    if (!this.schemaMember) return [];
    return [
      Members.forReturnType(this.operation),
    ].concat(this.operation.parameters.map(Members.forParam))
  }

  ngOnInit() {
    this.operation.parameters
  }


}

// either an input or the return type
export interface SignatureMember {
  type: Type
  name?: string
  metadata: Metadata[]
  kind: InputOutput
}

enum InputOutput {
  PARAM = "Input", RETURN = "Output"
}


class Members {
  static forReturnType(operation: Operation): SignatureMember {
    return {
      type: operation.returnType,
      kind: InputOutput.RETURN,
      metadata: [] //TODO
      // metadata: operation.contract.constraints
    }
  }

  static forParam(param: Parameter): SignatureMember {
    return {
      name: param.name,
      type: param.type,
      metadata: param.metadata,
      kind: InputOutput.PARAM
    }
  }
}
