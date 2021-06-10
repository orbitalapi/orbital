import {InstanceLike, Type, UntypedInstance} from '../services/schema';

export class InstanceSelectedEvent implements QueryResultMemberCoordinates {
  constructor(public readonly selectedTypeInstance: InstanceLike | UntypedInstance,
              public readonly selectedTypeInstanceType: Type | null,
              public readonly rowValueId: number,
              public readonly attributeName: string,
              public readonly queryId: string
  ) {
  }
}

export interface QueryResultMemberCoordinates {
   rowValueId: number;
   attributeName: string;
   queryId: string;
}
