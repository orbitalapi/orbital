import { PackageIdentifier } from 'src/app/package-viewer/packages.service';

export class GitRepositoryConfig {
  public name: string;
  public uri: string;
  public branch: string;

  public pullRequestConfig: GitPullRequestConfig | null = new GitPullRequestConfig();
  public isEditable: boolean = false;

  public path: string = '/'

  public loader: PackageLoaderSpec = new TaxiPackageLoaderSpec();
}

export interface CreateEmptyProjectRequest {
  newProjectIdentifier: PackageIdentifier
}
export class FileSystemPackageSpec {
  public path: string;
  public loader: PackageLoaderSpec = new TaxiPackageLoaderSpec();
  public isEditable: boolean = true;

  // Part of CreateFileRepositoryRequest, but shoe-horning here, as these
  // classes are otherwise identical
  newProjectIdentifier: PackageIdentifier | null = null
}
export type LoadablePackageType = 'OpenApi' | 'Taxi' | 'Protobuf' | 'JsonSchema' | 'Avro';

export interface PackageLoaderSpec {
  packageType: LoadablePackageType;
}

export class GitPullRequestConfig {
  public branchPrefix: string = 'schema-updates/';
  public hostingProvider: GitHostingProvider = 'Github';
}
export type GitHostingProvider = 'Github' | 'Gitlab';

export class TaxiPackageLoaderSpec implements PackageLoaderSpec {
  readonly packageType = 'Taxi';
}

export class OpenApiPackageLoaderSpec implements PackageLoaderSpec {
  readonly packageType = 'OpenApi'
  identifier: PackageIdentifier = {
    name: null,
    organisation: null,
    version: '1.0.0',
    id: null,
    unversionedId: null
  }
  defaultNamespace: string;
  serviceBasePath: string;
}

export class AvroPackageLoaderSpec  implements PackageLoaderSpec  {
  readonly packageType = 'Avro'
  identifier: PackageIdentifier = {
    name: null,
    organisation: null,
    version: '1.0.0',
    id: null,
    unversionedId: null
  }
}

