import {Metadata} from '../services/schema';
import {isNullOrUndefined} from 'util';

export const DATA_OWNER_FQN = 'io.vyne.catalog.DataOwner';
export const DATA_OWNER_TAG_OWNER_NAME = 'name';
export const DATA_OWNER_TAG_OWNER_USER_ID = 'id';

export function findDataOwner(metadata: Metadata[]): Metadata {
  if (isNullOrUndefined(metadata)) {
    return null;
  }
  return metadata.find(m => m.name.fullyQualifiedName === DATA_OWNER_FQN);
}
