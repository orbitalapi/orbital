import {HttpClient} from '@angular/common/http';
import {Inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {Environment, ENVIRONMENT} from '../services/environment';

@Injectable({
  providedIn: 'root'
})
export class ClusterMembersService {
  nodeClusterDetails: Observable<ClusterInfo>

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    private httpClient: HttpClient
  ) {
    this.getLicenseDetails()
  }

  private getLicenseDetails(): Observable<ClusterInfo> {
    return this.nodeClusterDetails = this.httpClient.get<ClusterInfo>(`${this.environment.serverUrl}/api/config/cluster`)
  }
}

type ClusterInfo = {
  members: string[]
}
