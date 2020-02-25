import {QualifiedName} from '../services/schema';


export interface SearchResult {
  qualifiedName: QualifiedName;
  typeDoc: string | null;
  matches: SearchMatch[];
}

export interface SearchMatch {
  field: SearchField;
  highlightedMatch: string;
}

export type SearchField = 'QUALIFIED_NAME' | 'NAME' | 'TYPEDOC';
