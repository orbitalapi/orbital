export const generalFeatures = [
   {
      title: 'Max endpoints',
      description: 'An "endpoint" is a database table, API, Kafka Topic, Lambda, or Pipeline Source / Sink',
      tiers: [
         {title: 'project', value: '5'},
         {title: 'platform', featured: true, value: 'Unlimited'},
         {title: 'enterprise', value: 'Unlimited'},
      ],
   },
   {
      title: '# Worker Nodes',
      description: 'The number of nodes that a query can be distributed across.  Add more nodes to improve throughput',
      tiers: [
         {title: 'project', value: '1'},
         {title: 'platform', featured: true, value: '2 Included.  Additional available'},
         {title: 'enterprise', value: 'Contact us'},
      ]
   },
]

export const automatedIntegrationFeatures = [

   {
      title: 'UI Tooling',
      description: 'Dive into a rich exploration of your data ecosystem',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Schema Server',
      description: 'Publish data schemas from a file or git repo',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },
   // {
   //    title: 'CI / CD Tooling',
   //    description: 'Upgrade confidently with automated checks of query plans',
   //    tiers: [
   //       {title: 'project', value: true},
   //       {title: 'platform', featured: true, value: true},
   //       {title: 'enterprise', value: true},
   //    ],
   // },
   {
      title: 'REST API Integration',
      description: 'Automated integration of REST APIs',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },

   {
      title: 'GraphQL Data Sources',
      description: 'Include your existing GraphQL services in Vyne\'s data layer',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: 'Coming soon'},
         {title: 'enterprise', featured: true,  value: 'Coming soon'},
      ],
   },
   {
      title: 'Federated GraphQL',
      description: 'Query all your GraphQL endpoints as one',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: 'Coming soon'},
         {title: 'enterprise', featured: true, value: 'Coming soon'},
      ],
   },
   {
      title: 'Database support',
      description: 'Queries can read and enrich data from databases',
      tiers: [
         {title: 'project', value: 'Postgres, MySQL, MariaDB'},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Streaming database queries',
      description: 'For large datasets, stream data incrementally from the database, to avoid memory problems',
      tiers: [
         {title: 'project', value: false },
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Query message queues',
      description: 'Respond to streaming data sources such as Kafka and WebSockets',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Lambda support',
      description: 'Enrich data by invoking AWS Lambdas seamlessly within your queries',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Dynamic ETL Pipelines',
      description: 'Automate and enhance your regular data transfers',
      tiers: [
         {title: 'project', value: true },
         {title: 'platform', value: true, featured: true},
         {title: 'enterprise', value: true },
      ],
   },
   {
      title: 'AWS SQS / SNS Support',
      description: 'Trigger pipelines and write results to SQS and SNS endpoints',
      tiers: [
         {title: 'project', value: true },
         {title: 'platform', value: true, featured: true},
         {title: 'enterprise', value: true },
      ],
   },
   {
      title: 'Embedded Vyne',
      description: 'For latency critical use cases, embed the full power of Vyne directly within your service (JVM only)',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Query from Spreadsheets',
      description: 'Perfect for analysts, query your data layer directly from Microsoft Excel or Google Sheets',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Custom Functions',
      description: 'Extend Vyne with your own transformation functions, and use directly within Vyne\'s query engine',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: 'Kotlin / Typescript / Python',  featured: true,},
      ],
   }

]

export const securityFeatures = [
   {
      title: 'LDAP / SSO',
      description: 'Control access to data via standard enterprise auth technologies',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Security Policies on Data',
      description: 'Author security policies for individual data points. Write once, enforce everywhere',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   }
]

export const dataToolingFeatures = [
   // {
   //    title: 'Data Quality Metrics',
   //    description: 'Inspect and track the quality of your data based on your own rule set, authored directly in Taxi',
   //    tiers: [
   //       {title: 'project', value: false},
   //       {title: 'platform', featured: true, value: false},
   //       {title: 'enterprise', value: true, },
   //    ],
   // },
   {
      title: 'Automated data catalog',
      description: 'Quickly find the data you need with an automated data catalog powered by your schemas and metadata.  Automatically updated as schemas change',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', featured: true, value: true},
      ],
   },
   {
      title: 'Automated API catalog',
      description: 'Browse and test all the APIs across your organisation, with an interactive API catalog',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', featured: true, value: true},
      ],
   },
   {
      title: 'Data lineage - System level',
      description: 'Understand how data flows between systems with rich lineage visualisations',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', featured: true, value: true},
      ],
   },
   {
      title: 'Data lineage - Value level',
      description: 'See the full lineage for every value returned and calculation performed in a query, ',
      tiers: [
         {title: 'project', value: true},
         {title: 'platform', featured: true, value: true},
         {title: 'enterprise', featured: true, value: true},
      ],
   },
   {
      title: 'Schema Heath Checks',
      description: 'Monitor the schemas being published by your services and receive alerts for misconfigurations',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: false, value: false},
         {title: 'enterprise', featured: true, value: 'Coming soon'},
      ],
   },
   {
      title: 'Data Lineage Retention',
      description: 'Track and visualise where your data actually came from. Not where you thought it was a year ago.',
      tiers: [
         {title: 'project', value: '1 hour'},
         {title: 'platform', featured: true, value: '1 week'},
         {title: 'enterprise', featured: true, value: 'Unlimited'},
      ],
   }
]

export const caskFeatures = [
   {
      title: 'Casks',
      description: 'Our data-as-a-service container makes your offline data queryable',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Cask Extensions',
      description: 'Create aggregation functions to query your Casks with additional insight',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
   {
      title: 'Streaming data updates',
      description: 'Receive updates when the data in your Casks is modified',
      tiers: [
         {title: 'project', value: false},
         {title: 'platform', featured: true, value: false},
         {title: 'enterprise', value: true},
      ],
   },
]

export const supportFeatures = [
   {
      title: 'Support',
      tiers: [
         {title: 'project', value: 'Community'},
         {title: 'platform', featured: true, value: '2 business days'},
         {title: 'enterprise', value: 'Platinum support. 1 business day', featured: true},
      ],
   },
]

export const featureGroups = [
   {
      title: 'General', // not visible
      features: generalFeatures
   },
   {
      title: 'Automated Integration',
      features: automatedIntegrationFeatures
   },
   {
      title: 'Data Tooling',
      features: dataToolingFeatures
   },
   {
      title: 'Security',
      features: securityFeatures
   },
   {
      title: 'No-code data stores',
      features: caskFeatures
   },
   {
      title: 'Support',
      features: supportFeatures
   },
]
