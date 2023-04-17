import * as React from 'react';

import dynamic from 'next/dynamic';
import { Widont } from '../Widont';
import { Paragraph } from './common';

const DynamicWhereToUseOrbital = dynamic(() => import('@/components/home/WhereToUseDiagram'), {
  loading: () => <p>Loading...</p>
})


function WhereToUseSection(props) {

  return (
    <section id="where-to-use" className={`relative dark:bg-midnight-blue hidden lg:block ${props.className}`}>
      <h2 className="mt-2 mb-10 text-3xl font-bold tracking-tight text-white sm:text-4xl text-center">
        <Widont>Where Orbital fits in your stack</Widont>
      </h2>
      <DynamicWhereToUseOrbital className="inline-block w-full" />
    </section>
  )
}

export default WhereToUseSection
