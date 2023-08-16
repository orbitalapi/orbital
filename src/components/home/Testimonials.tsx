import * as React from 'react';

import JeyProfile from './img/testimonials/jey.png';
import Musicflow from './img/testimonials/musicflow.png';
import MarkusProfile from './img/testimonials/markus.png';
import Quadcorps from './img/testimonials/qc-logo-crop.png';

import Image from 'next/future/image';

function Testimonials(props) {
  return (
    <section className="bg-gray-900 py-24 sm:py-32">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mx-auto grid max-w-2xl grid-cols-1 lg:mx-0 lg:max-w-none lg:grid-cols-2">
          <div className="flex flex-col pb-10 sm:pb-16 lg:pb-0 lg:pr-8 xl:pr-20">
            <Image src={Musicflow} alt='' className='h-8 w-auto self-start'></Image>
            {/*<img className="h-12 self-start" src="https://tailwindui.com/img/logos/tuple-logo-white.svg" alt=""/>*/}
            <figure className="mt-10 flex flex-auto flex-col justify-between">
              <blockquote className="text-lg leading-8 text-white">
                <p>
                  “Easily the best decision I've made was building our startup on Orbital.  We got our platform built and operational in record time, with data pipelines and bespoke APIs for connecting our client data feeds.”
                </p>
              </blockquote>
              <figcaption className="mt-10 flex items-center gap-x-6">
                <Image
                  className="h-14 w-14 rounded-full bg-gray-800"
                  src={MarkusProfile}
                  alt=""
                />
                <div className="text-base">
                  <a href='https://musicflow.co'>
                    <div className="font-semibold text-white">Markus Buhmann</div>
                    <div className="mt-1 text-gray-400">Co-founder of Musicflow</div>
                  </a>
                </div>
              </figcaption>
            </figure>
          </div>
          <div
            className="flex flex-col border-t border-white/10 pt-10 sm:pt-16 lg:border-l lg:border-t-0 lg:pl-8 lg:pt-0 xl:pl-20">
            <Image className="h-6 w-auto self-start" src={Quadcorps} alt=""/>
            <figure className="mt-10 flex flex-auto flex-col justify-between">
              <blockquote className="text-lg leading-8 text-white">
                <p>
                  “Orbital is one of the most exciting pieces of tech in the integration space I've seen in a long time.  Customers can build integration in a fraction of the time, and the self-repairing is a game-changer.”
                </p>
              </blockquote>
              <figcaption className="mt-10 flex items-center gap-x-6">
                <Image
                  className="h-14 w-14 rounded-full bg-gray-800"
                  src={JeyProfile}
                  alt=""
                />
                <div className="text-base">
                  <a href='https://quadcorps.co.uk'>
                    <div className="font-semibold text-white">Jey Deivachandran</div>
                    <div className="mt-1 text-gray-400">CEO of QuadCorps</div>
                  </a>
                </div>
              </figcaption>
            </figure>
          </div>
        </div>
      </div>
    </section>)
}

export default Testimonials
