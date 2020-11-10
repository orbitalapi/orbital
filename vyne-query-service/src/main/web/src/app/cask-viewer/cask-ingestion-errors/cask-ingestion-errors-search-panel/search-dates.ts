export interface SearchDates {
  searchStart: Date;
  searchEnd: Date;
}

export function today(): Date {
  return new Date();
}

export function yesterday(): Date {
  const now = new Date();
  now.setDate(now.getDate() - 1);
  return now;
}
