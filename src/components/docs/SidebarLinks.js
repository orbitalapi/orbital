
export const SidebarLinks = [
   {
      title: 'Introduction',
      links: [
         {title: 'Quick start', href: '/docs'},
         {title: 'Overview', href: '/docs/introduction'},
      ],
   },
   {
      title: 'Connecting data sources',
      links: [
         {title: 'Overview', href: '/docs/connecting-data-sources/overview'},
         {title: 'Pulling API specs from git', href: '/docs/connecting-data-sources/connecting-a-git-repo'},
         {title: 'Using Taxi', href: '/docs/publishing-data-sources/using-taxi'},
         {title: 'Using existing API specs', href: '/docs/publishing-data-sources/existing-api-specs'},
         {title: 'Connecting databases', href: '/docs/publishing-data-sources/connecting-databases'},
         {title: 'Connecting message brokers', href: '/docs/publishing-data-sources/connecting-message-brokers'},
         {title: 'Publishing from applications', href: '/docs/connecting-data-sources/publishing-from-applications'},
         {title: 'Pushing directly to Orbital', href: '/docs/connecting-data-sources/publishing-direct-to-orbital'},
      ],
   },
  {
    title: 'Querying for data',
    links: [
      { title: 'TaxiQL tutorial', href: '/docs/sample' },
      { title: `Querying using Orbital's API`, href: '/docs/sample' },
      { title: `Using an SDK`, href: '/docs/sample' }
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
      { title: 'Production deployments', href: '/docs/sample' },
      { title: 'Configuring Orbital', href: '/docs/deploying/configuring-orbital' },
      { title: 'Authenticating to other services', href: '/docs/sample' },
      {
        title: 'Authorization within Orbital',
        href: '/docs/sample'
      },
         {
            title: 'Standalone analytics server',
            href: '/docs/sample',
         },
         {
            title: 'Distributing work across a cluster',
            href: '/docs/sample',
         },
      ],
   },
   {
      title: 'Tutorials',
      links: [
        { title: 'Integrating APIs, DBs and Kafka', href: '/docs/writing-plugins' }
        // {title: 'Composing APIs', href: '/docs/writing-plugins'},
        // {title: 'Building a Data mesh', href: '/docs/writing-plugins'},
      ],
   },
  {
    title: 'Background',
    links: [
      { title: 'Introduction to Semantic Integration', href: '/background/intro-to-semantic-integration' },
      { title: 'Tips on building Taxonomies', href: '/background/tips-on-taxonomies' }
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
