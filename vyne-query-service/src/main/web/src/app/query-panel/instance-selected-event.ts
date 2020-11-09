import {InstanceLike, Type, UntypedInstance} from '../services/schema';

export class InstanceSelectedEvent {
  constructor(public readonly selectedTypeInstance: InstanceLike | UntypedInstance,
              public readonly selectedTypeInstanceType: Type | null,
              public readonly nodeId: string | null
  ) {
  }
}

