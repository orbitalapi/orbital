export interface FilterOperation {
  readonly label: string;
  readonly symbol: string;
}


export const equals: FilterOperation = {label: 'equals', symbol: '='};
// export const in: FilterOperation = {label: 'in', symbol: 'in'};
