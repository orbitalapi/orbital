import { isNullOrUndefined } from 'src/app/utils/utils';

export function methodClassFromName(method: string) {
  if (isNullOrUndefined(method)) {
    return null;
  }
  switch (method.toUpperCase()) {
    case 'GET':
      return 'get-method';
    case 'POST':
      return 'post-method';
    case 'PUT':
      return 'put-method';
    case 'DELETE':
      return 'delete-method';
    default:
      return 'other-method';
  }
}
