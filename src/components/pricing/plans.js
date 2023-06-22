export const plans = [
  {
    title: 'Free',
    featured: false,
    description: 'Try all the features of Orbital for free, for up to 5 endpoints',
    showInFeatureList: false,
    hasPrice: false,
    annualPrice: 'Free',
    mainFeatures: [
      {id: 1, value: 'All the features of Orbital'},
      {id: 2, value: 'Up to 5 endpoints'},
      {id: 4, value: 'Community support'},
      // {id: 3, value: 'Up to 5 endpoints included'}
    ],
    cta: 'Get Started',
    ctaLink: '/docs/guides/first-integration/',
  },
  {
    title: 'Project',
    featured: false,
    description: 'Ideal for microservices, and small teams',
    showInFeatureList: true,
    hasPrice: true,
    annualPrice: '$99',
    mainFeatures: [
      {id: 1, value: 'Query REST APIs and OSS Databases'},
      {id: 2, value: 'Build dynamic ETL pipelines that automatically adapt'},
      {id: 4, value: 'Integration that automatically adapts'},
      {id: 5, value: 'Powerful data catalog'},
      // {id: 3, value: 'Up to 5 endpoints included'}
    ],
    cta: 'Get Started',
    ctaLink: '/docs/guides/first-integration/',
  },
  {
    title: 'Platform',
    featured: true,
    description: 'Unlock the data across your organization',
    showInFeatureList: true,
    hasPrice: true,
    annualPrice: '$199',
    mainFeatures: [
      {id: 1, value: 'Everything in free'},
      {id: 4, value: 'Support for Lambdas, Kafka, SQS, SNS'},
      {id: 3, value: 'Database and streaming data sources'},
      {id: 2, value: '1-click deploy of queries as HTTP endpoints'},
      {id: 5, value: 'Query caching'},
    ],
    cta: 'Get Started',
    ctaLink: '/docs/guides/first-integration/',
  },
  {
    title: 'Enterprise',
    featured: false,
    description: 'Everything you need for large datasets, and bespoke logic',
    showInFeatureList: true,
    hasPrice: false,
    annualPrice: 'Contact Us',
    mainFeatures: [
      {id: 1, value: 'Everything in platform'},
      {id: 3, value: 'OpenID Authentication & Role base security'},
      {id: 2, value: 'Stream large datasets from databases'},
      {id: 4, value: 'Extend with custom functions'},
      {id: 5, value: 'Rich security policies, applied everywhere'},
    ],
    cta: 'Contact Us',
    ctaLink: `mailto:hello@orbitalhq.com?subject=Tell me more about Orbital!&body=Hi Orbital team, <enter expression of interest / encouragement / delight / fascination / astoundment here>.`
  },
]
