import { Type } from 'src/app/services/schema';

export interface TypeSelectedEvent {
  type: Type
  // TODO : add something for passing in newly created types that have been created during the import process, but dom't exist in the schema yet
  source: 'schema' | 'new';
}
