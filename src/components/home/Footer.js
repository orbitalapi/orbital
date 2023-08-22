import Link from 'next/link';
import { Logo } from '@/components/Logo';

const footerNav = [
  {
    'Getting Started': [
      { title: 'Quick start', href: '/docs' },
      // { title: 'Connecting OpenAPI Services', href: '/docs' },
      // { title: 'Connecting Message Queues', href: '/docs' },
      { title: 'Connecting a Kafka topic', href: '/docs/connecting-data-sources/connect-kafka-topic' },
      { title: 'Connecting Databases', href: '/docs/connecting-data-sources/connecting-a-database' },
      { title: 'Querying for data', href: '/docs/querying/writing-queries' },
      // { title: 'Building data pipelines', href: '/docs' }
    ],
    Guides: [
      { title: 'APIs, DBs and Queues - A flyby of Orbital ', href: '/docs/guides/first-integration' },
      // { title: 'Building a data mesh with Orbital', href: '/docs' },
      // { title: 'Taxonomy best practices', href: '/docs' },
      // { title: 'Zero code data normalization', href: '/docs' }
    ],
    'Community and Tools': [
      { title: 'Voyager', href: 'https://voyager.vyne.co' },
      { title: 'Taxi', href: 'https://taxilang.org' },

      { title: 'Slack', href: 'https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg' },
      { title: 'GitHub', href: 'https://github.com/orbitalapi' },
      { title: 'Twitter', href: 'https://twitter.com/orbitalapi' },
      { title: 'LinkedIn', href: 'https://www.linkedin.com/company/orbitalhq/' }
    ],
  },
]

export function Footer() {
  return (
    <footer className="pb-16 text-sm leading-6">
      <div className="max-w-7xl mx-auto divide-y divide-slate-200 px-4 sm:px-6 md:px-8 dark:divide-slate-700">
        <div className="flex">
          {footerNav.map((sections) => (
            <div
              key={Object.keys(sections).join(',')}
              className="flex-none w-1/2 space-y-10 sm:space-y-8 lg:flex lg:space-y-0"
            >
              {Object.entries(sections).map(([title, items]) => (
                <div key={title} className="lg:flex-none lg:w-1/2">
                  <h2 className="font-semibold text-slate-900 dark:text-slate-100">{title}</h2>
                  <ul className="mt-3 space-y-2">
                    {items.map((item) => (
                      <li key={item.href}>
                        <Link href={item.href}>
                          <a className="hover:text-slate-900 dark:hover:text-slate-300">
                            {item.title}
                          </a>
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          ))}
        </div>
        <div className="mt-16 pt-10">
          <Logo className="w-auto h-6"/>
        </div>
      </div>
    </footer>
  )
}
