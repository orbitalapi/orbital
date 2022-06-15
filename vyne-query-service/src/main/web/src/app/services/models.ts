import {Type, TypedObjectAttributes} from './schema';
import {RemoteCall, ResponseStatus} from './query.service';
import {isNullOrUndefined} from 'util';


/**
 * During a streaming query, we can receive any of these message types
 */
export type StreamingQueryMessage = ValueWithTypeName | FailedSearchResponse;

export function isFailedSearchResponse(message: StreamingQueryMessage): message is FailedSearchResponse {
  return !isNullOrUndefined(message['responseStatus']) && message['responseStatus'] === ResponseStatus.ERROR;
}

export function isValueWithTypeName(message: any): message is ValueWithTypeName {
  return !isNullOrUndefined(message) && !isNullOrUndefined(message['value']) &&
    !isNullOrUndefined(message['anonymousTypes']) && // always present, often [],
    !isNullOrUndefined(message['queryId']); // always present.
}

export interface ValueWithTypeName {
  typeName: string | null;
  anonymousTypes: Type[];
  /**
   * This is the serialized instance, as converted by a RawObjectMapper.
   * It's a raw json object.
   * Use TypedObjectAttributes here, rather than any, as it's compatible with InstanceLike interface
   */
  value: TypedObjectAttributes;
  valueId: number;
  /**
   * Only populated when this value is returned from an active query
   */
  queryId: string | null;
}

export interface FailedSearchResponse {
  message: string;
  responseStatus: ResponseStatus;
  queryResponseId: string | null;
  clientQueryId: string | null;
  remoteCalls: RemoteCall[];
}
