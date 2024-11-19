import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {VyneServicesModule} from '../../../services/vyne-services.module';
import {environment} from 'src/environments/environment';

@Injectable({
  providedIn: VyneServicesModule
})
export class S3ConnectionService {
  constructor(private http: HttpClient) {
  }

  listBuckets(packageUri: string, connectionName: string): Observable<ListBucketsResponse> {
    return this.http.get<ListBucketsResponse>(`${environment.serverUrl}/api/packages/${packageUri}/connections/aws/${connectionName}/s3/buckets`);
  }

  listFilenames(packageUri: string, connectionName: string, bucketName: string, pattern: string): Observable<ListObjectsResponse> {
    return this.http.get<ListObjectsResponse>(`${environment.serverUrl}/api/packages/${packageUri}/connections/aws/${connectionName}/s3/buckets/${bucketName}?pattern=${pattern}`);
  }
}

export type ListBucketsResponse = {
  bucketNames: string[]
}

export type ListObjectsResponse = {
  objectNames: string[]
}

