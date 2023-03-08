
export const SidebarLinks = [
  {
    title: 'Introduction',
    links: [
      { title: 'Quick start', href: '/docs' },
      { title: 'Overview', href: '/docs/introduction' },
    ],
  },
  {
    title: 'Guides',
    links: [
      { title: 'First integration', href: '/docs/guides/first-integration' }
    ],
  },
  {
    title: 'Connecting data sources',
    links: [
      { title: 'Overview', href: '/docs/connecting-data-sources/overview' },
      { title: 'Publishing schemas to Orbital', href: '/docs/connecting-data-sources/schema-publication-methods' },
      { title: 'Pulling schemas from git', href: '/docs/connecting-data-sources/connecting-a-git-repo' },
      // TODO
      //  {title: 'Using Taxi', href: '/docs/publishing-data-sources/using-taxi'},
      // TODO
      //  {title: 'Using existing API specs', href: '/docs/publishing-data-sources/existing-api-specs'},
      // COPIED
      // COPIED
      { title: 'Connecting a Database', href: '/docs/connecting-data-sources/connecting-a-database' },
      { title: 'Connecting a Kafka topic', href: '/docs/connecting-data-sources/connect-kafka-topic' },

      // TODO: Copied over the Kafka specific page instead, this one would be a better overview
      //  {title: 'Connecting message brokers', href: '/docs/publishing-data-sources/connecting-message-brokers'},
      // TODO: More specific version of what we've got
      //  {title: 'Publishing from applications', href: '/docs/connecting-data-sources/publishing-from-applications'},
      // TODO: Not sure how this is different from publishing from applications?
      //  {title: 'Publishing directly to Orbital', href: '/docs/connecting-data-sources/publishing-direct-to-orbital'},
    ],
  },
  {
    title: 'Describing data sources',
    links: [
      { title: 'Using OpenAPI', href: '/docs/describing-data-sources/open-api' },
      { title: 'Using Protobuf', href: '/docs/describing-data-sources/protobuf' },
      // Note: This used to be 'Connecting databases'
      { title: 'Databases', href: '/docs/describing-data-sources/databases' },
      // { title: 'Using Taxi', href: '/docs/describing-data- sources/taxi'}
      { title: 'Enable UI Schema Editing', href: '/docs/describing-data-sources/enable-ui-schema-editing'},
    ]
  },
  {
    title: 'Querying',
    links: [
      { title: 'Writing queries', href: '/docs/querying/writing-queries' },
      // { title: 'TaxiQL tutorial', href: '/docs/sample' },
      // TODO: This needs to be more step by step rather than the existing docs which describes how to write TaxiQL
      // { title: `Querying using Orbital's API`, href: '/docs/sample' },
      // { title: `Using an SDK`, href: '/docs/sample' }
    ]
  },
  // {
  //    title: 'Data pipelines',
  //    links: [
  //       {title: 'Building a pipeline', href: '/docs/sample'},
  //    ],
  // },
  {
    title: 'Deploying Orbital',
    links: [
      { title: 'Production deployments', href: '/docs/deploying/production-deployments' },
      { title: 'Configuring Orbital', href: '/docs/deploying/configuring-orbital' },
      { title: 'Configuring Connections', href: '/docs/deploying/configuring-connections'},
      { title: 'Configuring the Schema Server', href: '/docs/deploying/schema-server'},
      { title: 'Authenticating to other services', href: '/docs/deploying/authentication-to-services' },
      { title: 'Authentication to Orbital', href: '/docs/deploying/authentication'},
      { title: 'Authorization within Orbital', href: '/docs/deploying/authorization'},
      // TODO: Needs to be written
      // {
      //   title: 'Standalone analytics server',
      //   href: '/docs/sample',
      // },
      {
        title: 'Distributing work across a cluster',
        href: '/docs/deploying/distributing-work-on-a-cluster',
      },
    ],
  },
  //  {
  //     title: 'Tutorials',
  //     links: [
  //       // TODO: Not sure what this is supposed to be
  //       // { title: 'Integrating APIs, DBs and Kafka', href: '/docs/writing-plugins' }
  //       // {title: 'Composing APIs', href: '/docs/writing-plugins'},
  //       // {title: 'Building a Data mesh', href: '/docs/writing-plugins'},
  //     ],
  //  },
  {
    title: 'Background',
    links: [
      { title: 'Introduction to Semantic Integration', href: '/docs/background/intro-to-semantic-integration' },
      { title: 'Tips on building Taxonomies', href: '/docs/background/tips-on-taxonomies' }
    ]
  },
  // {
  //    title: 'Troubleshooting',
  //    links: [
  //       {title: 'Mac M1 & Docker', href: '/troubleshooting/mac-m1-and-docker'},
  //    ],
  // },
  {
    title: 'Release notes',
    links: [
      { title: '0.21.x', href: '/docs/release-notes/0.21' },
      { title: '0.20.x', href: '/docs/release-notes/0.20' },
      { title: '0.19.x', href: '/docs/release-notes/0.19' },
      { title: '0.18.x', href: '/docs/release-notes/0.18' }
    ]
  }
]
