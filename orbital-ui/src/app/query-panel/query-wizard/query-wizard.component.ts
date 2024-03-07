import {QueryProfileData, RemoteCall, ResponseStatus} from '../../services/query.service';
import {FailedSearchResponse} from '../../services/models';

/**
 * @deprecated use FailedSearchResponse instead
 */
export class QueryFailure implements FailedSearchResponse {
  responseStatus: ResponseStatus = ResponseStatus.ERROR;

  constructor(readonly message: string,
              readonly profilerOperation: QueryProfileData | null = null,
              readonly remoteCalls: RemoteCall[] = [],
              readonly queryResponseId: string | null = null,
              readonly  clientQueryId: string | null = null) {
  }

}

