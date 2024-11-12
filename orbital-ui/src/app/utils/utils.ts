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

/**
 * Returns true if value is an object containing the property name.
 *
 * Use this in Typescript type guard functions
 * (eg: isFoo(value: any): value is Foo)
 */
export function isObjectWithProperty(value: unknown, propertyName: string) {
  return (
    typeof value === 'object' &&
    value !== null &&
    propertyName in value &&
    value[propertyName] !== undefined
  );
}
