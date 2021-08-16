module.exports = {
   pathPrefix: '/',
   plugins: [
      {
         resolve: 'gatsby-theme-apollo-docs',
         options: {
            root: __dirname,
            siteName: 'Vyne',
            description: 'Discover and integrate data automatically with Vyne',
            pageTitle: 'Vyne docs',
            menuTitle: 'Vyne',
            gaTrackingId: 'UA-74643563-13',
            algoliaApiKey: '2bbaa3f0c47dccb0c461c65c02943ca6',
            algoliaIndexName: 'taxidocs',
            githubRepo: 'vyneco/docs',
            baseUrl: 'https://docs.vyne.co',
            twitterHandle: 'apollographql',
            spectrumHandle: 'vyne-dec',
            youtubeUrl: 'https://www.youtube.com/channel/UC0pEW_GOrMJ23l8QcrGdKSw',
            logoLink: 'https://docs.vyne.co/',
            ffWidgetId: '3131c43c-bfb5-44e6-9a72-b4094f7ec028',
            subtitle: 'Vyne',
            spectrumPath: '/',
            shareImageConfig: {
               cloudName: 'notional',
               imagePublicID: 'taxi/dark-blue-background',
               titleFont: 'Source%20Sans%20Pro',
               taglineFont: 'Source%20Sans%20Pro',
               textColor: 'ffffff',
               titleExtraConfig: '_bold',
               textAreaWidth: 1120,
               titleFontSize: 80,
               taglineFontSize: 52,
               textLeftOffset: 80,
            },
            sidebarCategories: {
               null: ['index', 'testing-with-vynetest'],
               Overview: [
                  'overview/README',
                  'overview/publishing-and-sharing-schemas',
               ],
               'Developers Guide': [
                  'developers-guide/README',
                  'developers-guide/basic-walkthrough-hello-world',
                  'developers-guide/setting-up-vyne-locally',
                  'developers-guide/employees-on-payroll-demo',
                  'developers-guide/example-rewards-cards',
                  'developers-guide/exploring-vynes-problem-solving',
                  'developers-guide/polymorphic-behaviour-discovery',
                  'developers-guide/storing-data-in-cask',
                  'developers-guide/setting-up-vyne-using-multicasting',
               ],
               'Querying with vyne': [
                  'querying-with-vyne/README',
                  'querying-with-vyne/authentication-to-services',
                  'querying-with-vyne/query-history',
                  'querying-with-vyne/query-profiler',
                  'querying-with-vyne/data-lineage',
               ],
               'Schema Server': ['schema-server'],
               'Running a local taxonomy editor environment': [
                  'running-a-local-taxonomy-editor-environment/README',
                  'running-a-local-taxonomy-editor-environment/starting-vyne',
                  'running-a-local-taxonomy-editor-environment/editing-a-taxonomy',
                  'running-a-local-taxonomy-editor-environment/setting-visual-studio-code',
                  'running-a-local-taxonomy-editor-environment/building-and-testing-a-mapping',
                  'running-a-local-taxonomy-editor-environment/ui',
               ],
               Casks: [
                  'casks/README',
                  'casks/cask-server-1',
                  'casks/cask-ingestion',
                  'casks/configuration/README',
                  'casks/configuration/cask-operations',
                  'casks/cask-server',
               ],
               Pipelines: [
                  'pipelines/README',
                  'pipelines/pipeline-orchestrator',
                  'pipelines/pipelines-orchestrator-api',
                  'pipelines/pipeline-runner',
               ],
               Deployment: [
                  'deployment/README',
                  'deployment/docker-images-configuration',
                  'deployment/sample-docker-compose',
                  'deployment/advanced-configuration',
               ],
               Testing: [
                  'testing/testing-with-vynetest'
               ],
               'Excel Plugin': [
                  'excel-plugin/installing',
                  'excel-plugin/how-to-use'
               ],
               'Release Notes': [
                  'release-notes/README'
               ]
            },
         },
      },

   ],
}
