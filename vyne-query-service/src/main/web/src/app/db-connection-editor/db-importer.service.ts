import {QualifiedName} from '../services/schema';

export interface TableColumn {
  name: string;
  dbType: string;
  taxiType: QualifiedName;
  nullable: boolean;
}

export interface TableMetadata {
  columns: TableColumn[];
  name: string;
}
