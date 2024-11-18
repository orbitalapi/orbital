import {QualifiedName} from "./schema";
import {isNullOrUndefined} from "../utils/utils";

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
  static LONG = PrimitiveTypeNames.primitiveTypeOf('Long');
  static VOID = PrimitiveTypeNames.primitiveTypeOf('Void');

  private static primitiveTypeOf(name: string): string {
    return `lang.taxi.${name}`;
  }
}

const DATE_TYPE_NAMES = [PrimitiveTypeNames.INSTANT, PrimitiveTypeNames.LOCAL_DATE]; // not time types
const NUMERIC_TYPE_NAMES = [PrimitiveTypeNames.INTEGER, PrimitiveTypeNames.LONG, PrimitiveTypeNames.DOUBLE, PrimitiveTypeNames.DECIMAL]

export function isDateType(name: QualifiedName | null): boolean {
  if (isNullOrUndefined(name)) return false;
  return DATE_TYPE_NAMES.includes(name.fullyQualifiedName)
}

export function isNumericType(name: QualifiedName | null): boolean {
  if (isNullOrUndefined(name)) return false;
  return NUMERIC_TYPE_NAMES.includes(name.fullyQualifiedName);
}
