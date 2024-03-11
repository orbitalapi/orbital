export function isNullOrUndefined(v:any | null | undefined):boolean {
  return v === null || v === undefined;
}

export function sanitiseNamespace(name: string): string {
  name = name.toLowerCase();
  // Remove non-alphanumeric characters
  name = name.replace(/[^a-z0-9.]+/g, '');
  // Remove leading and trailing underscores
  name = name.replace(/^_+|_+$/g, '');
  return name;
}
