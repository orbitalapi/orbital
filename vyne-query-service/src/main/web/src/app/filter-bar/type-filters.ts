import {Type} from '../services/schema';
import {isNullOrUndefined} from 'util';

// Common utils for building a filter bar using types
export function displayTypeName(type: Type): string {
  if (isNullOrUndefined(type)) {
    return null;
  } else {
    return `${type.name.name} (${type.name.fullyQualifiedName})`;
  }
}

export function filterTypeByName(subjects: Type[], filter: string): Type[] {
  if (typeof filter === 'string') {
    return subjects.filter(type => {
      return type.name.fullyQualifiedName.toLowerCase().includes(filter.toLowerCase());
    });
  } else {
    // Looks like we get called with the selected value once a selection event has happened
    return [filter];
  }
}

