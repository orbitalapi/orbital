import {AppConfig} from "../app/services/app-info.service";
import {SidebarElement} from '../app/sidenav/sidenav.component';
import {DocsLinks} from "./docs-links";

const orbitalDocsLinks : DocsLinks = {
  configureMetricsReporting : 'https://orbitalhq.com/docs/querying/observability#configuring-prometheus',
  publishQueriesAsEndpoints : 'https://orbitalhq.com/docs/querying/queries-as-endpoints',
  workspaceConfigFile : 'https://orbitalhq.com/docs/workspace/overview#workspace-conf-file',
  authenticationToServices : 'https://orbitalhq.com/docs/describing-data-sources/authentication-to-services',
  managingSecrets : 'https://orbitalhq.com/docs/deploying/managing-secrets',
  authentication : 'https://orbitalhq.com/docs/deploying/authentication',
  dynamoDbConnection : 'https://orbitalhq.com/docs/describing-data-sources/aws-services#dynamo-db',
  lambdaDbConnection : 'https://orbitalhq.com/docs/describing-data-sources/aws-services#lambda' ,
  s3Connection : 'https://orbitalhq.com/docs/describing-data-sources/aws-services#s3' ,
  sqsConnection : 'https://orbitalhq.com/docs/describing-data-sources/aws-services#sqs' ,
  dataPolicies : 'https://orbitalhq.com/docs/deploying/data-policies' ,
  docsHome : 'https://orbitalhq.com/docs' ,
}

export const UiCustomisations = {
  landingPageWelcomeText: 'Welcome to Orbital',
  productName: 'Orbital',
  customSidebarElements: function (appConfig: AppConfig): SidebarElement[] {
    return [];
  },
  docsLinks: orbitalDocsLinks
}
