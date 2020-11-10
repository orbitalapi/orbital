import {SearchDates} from '../cask-ingestion-errors-search-panel/search-dates';

interface CaskTable {
  tableName: string;
}

export type SearchInput = SearchDates & CaskTable;
