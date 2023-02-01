export const plans = [
   {
      title: 'Starter',
      featured: false,
      description: 'Get started for free',
      hasPrice: false,
      annualPrice: 'Free',
      mainFeatures: [
         {id: 1, value: 'Query REST APIs and Databases'},
         {id: 2, value: 'Build dynamic ETL pipelines that automatically adapt'},
         {id: 4, value: 'Integration that automatically adapts'},
         {id: 5, value: 'Powerful data catalog'},
         {id: 3, value: 'Up to 5 endpoints included'}
      ],
      cta: 'Get Started',
      ctaLink: 'https://docs.vyne.co/tutorials/api-db-integration/rest-db-integration/',
   },
   {
      title: 'Platform',
      featured: true,
      description: 'Unlock the data across your organization',
      hasPrice: true,
      annualPrice: '$99',
      mainFeatures: [
         {id: 1, value: 'Everything in free'},
         {id: 4, value: 'Support for Lambdas, Kafka, SQS, SNS'},
         {id: 2, value: 'Adaptive infrastructure'},
         {id: 3, value: 'Database and streaming data sources'},
      ],
      cta: 'Get Started',
      ctaLink: 'https://docs.vyne.co/tutorials/api-db-integration/rest-db-integration/',
   },
   {
      title: 'Enterprise',
      featured: false,
      description: 'Everything you need for large datasets, and bespoke logic',
      hasPrice: false,
      annualPrice: 'Contact Us',
      mainFeatures: [
         {id: 1, value: 'Everything in platform'},
         {id: 3, value: 'OpenID Authentication & Role base security'},
         {id: 2, value: 'Stream large datasets from databases'},
         {id: 4, value: 'Extend with custom functions'},
         {id: 6, value: 'No-code CSV-to-API with Casks'},
         {id: 5, value: 'Rich security policies, applied everywhere'},
      ],
      cta: 'Contact Us',
      ctaLink: `mailto:hello@vyne.co?subject=Tell me more about Vyne!&body=Hi Vyne team, <enter expression of interest / encouragement / delight / fascination / astoundment here>.`
   },
]
