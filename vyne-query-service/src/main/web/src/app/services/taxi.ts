export class PrimitiveTypeNames {
  static BOOLEAN = PrimitiveTypeNames.primitiveTypeOf('Boolean');
  static STRING = PrimitiveTypeNames.primitiveTypeOf('String');
  static INTEGER = PrimitiveTypeNames.primitiveTypeOf('Int');
  static DECIMAL = PrimitiveTypeNames.primitiveTypeOf('Decimal');
  static LOCAL_DATE = PrimitiveTypeNames.primitiveTypeOf('Date');
  static TIME = PrimitiveTypeNames.primitiveTypeOf('Time');
  static INSTANT = PrimitiveTypeNames.primitiveTypeOf('Instant');
  static ARRAY = PrimitiveTypeNames.primitiveTypeOf('Array');
  static ANY = PrimitiveTypeNames.primitiveTypeOf('Any');
  static DOUBLE = PrimitiveTypeNames.primitiveTypeOf('Double');
  static VOID = PrimitiveTypeNames.primitiveTypeOf('Void');

  private static primitiveTypeOf(name: string): string {
    return `lang.taxi.${name}`;
  }
}
