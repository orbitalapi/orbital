import React from 'react';
import {CheckIcon, ChevronDownIcon, ChevronRightIcon, XMarkIcon} from '@heroicons/react/24/solid'
import {plans} from '@/components/pricing/plans'
import {featureGroups} from '@/components/pricing/features'
import {Header} from "@/components/Header";
import {Footer} from "@/components/home/Footer";


function classNames(...classes) {
   return classes.filter(Boolean).join(' ')
}

export class PricingPageTemplate extends React.Component {

   constructor(props) {
      super(props);
      this.state = {
         plans: plans,
         featureGroups: featureGroups
      };
   }

   toggleFeature(groupIndex, featureIndex) {
      console.log("toggle");
      let featureGroups = this.state.featureGroups;
      featureGroups[groupIndex].features[featureIndex].visible = !featureGroups[groupIndex].features[featureIndex].visible

      this.setState({
         featureGroups: featureGroups
      })
   }

   render() {
      return (
         <div>
            <div className="relative dark:bg-midnight-blue text-gray-100">
               {/* Overlapping background */}
               <div aria-hidden="true" className="hidden absolute w-full h-6 bottom-0 lg:block"/>

               <div className="relative max-w-2xl mx-auto pt-16 px-4 text-center sm:pt-32 sm:px-6 lg:max-w-7xl lg:px-8">
                  <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-6xl">
                     <span className="block lg:inline"><span className="text-citrus">Rediscover</span> your data.</span>
                  </h1>
                  <p className="mt-4 text-xl text-gray-100">
                     Get started building now, then add a plan to go live.
                  </p>
               </div>

               <h2 className="sr-only">Plans</h2>

               {/* Cards */}
               <div className="relative max-w-2xl mx-auto mt-20 px-4 pb-8 sm:px-6 lg:max-w-7xl lg:px-8 lg:pb-0">
                  {/* Decorative background */}
                  <div
                     aria-hidden="true"
                     className="hidden absolute top-4 bottom-6 left-8 right-8 inset-0 bg-darkblue-700 rounded-tl-lg rounded-tr-lg lg:block"
                  />

               <div className="relative space-y-6 lg:space-y-0 lg:grid lg:grid-cols-3 lg:gap-6">
                     {plans.map((plan) => (
                        <div
                           key={plan.title}
                           className={classNames(
                              plan.featured ? 'bg-white/5 ring-aqua ring-2 shadow-lg shadow-aqua/50' : 'bg-darkblue-600 lg:bg-transparent border-white border',
                              'pt-6 px-6 pb-9 rounded-3xl lg:pt-12'
                           )}
                        >
                           <div>
                              <h3
                                 className={classNames(
                                    plan.featured ? 'text-darkblue-700' : 'text-aqua',
                                    'text-sm font-semibold uppercase tracking-wide'
                                 )}
                              >
                                 {plan.title}
                              </h3>
                              <div className={plan.featured
                                 ? "mt-3 flex items-center text-white "
                                 : "mt-3 flex items-center text-white"
                              }>
                                 <p className=" text-4xl font-extrabold tracking-tight">{plan.annualPrice}</p>
                                 {plan.hasPrice &&
                                    <div className="ml-4">
                                       <p className="text-sm">USD / endpoint   / mo</p>
                                       <p className="text-sm">Billed yearly</p>
                                    </div>}
                              </div>
                              <div
                                 className="flex flex-col items-start sm:flex-row sm:items-center sm:justify-between lg:flex-col lg:items-start mt-4">
                                 <a
                                    href={plan.ctaLink}
                                    className={classNames(
                                       plan.featured
                                          ? 'bg-citrus text-midnight-blue hover:bg-citrus-300'
                                          : 'border-white text-white hover:bg-gray-400',
                                       'mt-6 w-full inline-block py-2 px-8 border border-transparent rounded-md shadow-sm text-center text-sm font-medium sm:mt-0 sm:w-auto lg:mt-6 lg:w-full'
                                    )}
                                 >
                                    {plan.cta}
                                 </a>
                              </div>
                           </div>
                           <h4 className="sr-only">Features</h4>
                           <ul
                              className={classNames(
                                 plan.featured
                                    ? 'border-gray-500 divide-gray-500'
                                    : 'border-gray-500 divide-gray-500 divide-opacity-75',
                                 'mt-7 border-t divide-y lg:border-t-0'
                              )}
                           >
                              {plan.mainFeatures.map((mainFeature) => (
                                 <li key={mainFeature.id} className="py-3 flex items-center">
                                    <CheckIcon
                                       className={classNames(
                                          plan.featured ? 'text-lightblue-500' : 'text-gray-300',
                                          'w-5 h-5 flex-shrink-0'
                                       )}
                                       aria-hidden="true"
                                    />
                                    <span
                                       className={classNames(
                                          plan.featured ? 'text-darkblue-700' : 'text-white',
                                          'ml-3 text-sm font-medium'
                                       )}
                                    >
                          {mainFeature.value}
                        </span>
                                 </li>
                              ))}
                           </ul>
                        </div>
                     ))}
                  </div>
               </div>
            </div>

            {/* Feature comparison (up to lg) */}
            <section aria-labelledby="mobileComparisonHeading" className="lg:hidden">
               <h2 id="mobileComparisonHeading" className="sr-only">
                  Feature comparison
               </h2>

               <div className="max-w-2xl mx-auto py-16 px-4 space-y-16 sm:px-6">
                  {this.state.plans.map((plan, mobilePlanIndex) => (
                     <div key={plan.title} className="border-t border-gray-200">
                        <div
                           className={classNames(
                              plan.featured ? 'border-gray-600' : 'border-transparent',
                              '-mt-px pt-6 border-t-2 sm:w-1/2'
                           )}
                        >
                           <h3
                              className={classNames(plan.featured ? 'text-citrus' : 'text-white', 'text-sm font-bold')}>
                              {plan.title}
                           </h3>
                           <p className="mt-2 text-sm text-gray-500">{plan.description}</p>
                        </div>
                        {/* <h4 className="mt-10 text-sm font-bold text-gray-900">{featureGroups[0].title}</h4> */}

                        <div className="mt-6 relative">
                           {this.state.featureGroups.map((group, groupIndex) => (
                              <span key={group.title}>
                      {
                         groupIndex != 0 &&
                         <h4 className="mt-10 text-sm font-bold text-aqua">{group.title}</h4>
                      }

                                 <div className={"relative" + (groupIndex != 0 ? " mt-6" : "")}>
                        {/* Fake card background */}
                                    <div aria-hidden="true"
                                         className="hidden absolute inset-0 pointer-events-none sm:block">
                          <div
                             className={classNames(
                                plan.featured ? 'shadow-md' : 'shadow',
                                'absolute right-0 w-1/2 h-full bg-white/5 rounded-lg'
                             )}
                          />
                        </div>

                        <div
                           className={classNames(
                              plan.featured ? 'ring-2 ring-lightblue-400 shadow-md' : 'ring-1 ring-black ring-opacity-5 shadow',
                              'relative py-3 px-4 bg-white/5 rounded-lg sm:p-0 sm:bg-transparent sm:rounded-none sm:ring-0 sm:shadow-none'
                           )}
                        >
                          <dl className="divide-y divide-gray-200">
                            {group.features.map((feature) => (
                               <div key={feature.title} className="py-3 flex justify-between sm:grid sm:grid-cols-2">
                                  <dt className="text-sm font-medium text-white sm:pr-4">{feature.title}</dt>
                                  <dd className="flex items-center justify-end sm:px-4 sm:justify-center">
                                     {typeof feature.tiers[mobilePlanIndex].value === 'string' ? (
                                        <span
                                           className={classNames(
                                              feature.tiers[mobilePlanIndex].featured ? 'text-white' : 'text-white',
                                              'text-sm font-medium'
                                           )}
                                        >
                                      {feature.tiers[mobilePlanIndex].value}
                                    </span>
                                     ) : (
                                        <>
                                           {feature.tiers[mobilePlanIndex].value === true ? (
                                              <CheckIcon className="mx-auto h-5 w-5 text-white"
                                                         aria-hidden="true"/>
                                           ) : (
                                              <XMarkIcon className="mx-auto h-5 w-5 text-grey-200" aria-hidden="true"/>
                                           )}

                                           <span className="sr-only">
                                        {feature.tiers[mobilePlanIndex].value === true ? 'Yes' : 'No'}
                                      </span>
                                        </>
                                     )}
                                  </dd>
                               </div>
                            ))}
                          </dl>
                        </div>

                                    {/* Fake card border */}
                                    <div aria-hidden="true"
                                         className="hidden absolute inset-0 pointer-events-none sm:block">
                          <div
                             className={classNames(
                                plan.featured ? 'ring-2 ring-gray-600' : 'ring-1 ring-black ring-opacity-5',
                                'absolute right-0 w-1/2 h-full rounded-lg'
                             )}
                          />
                        </div>
                      </div>
                    </span>
                           ))}
                        </div>
                     </div>
                  ))}
               </div>
            </section>

            {/* Feature comparison (lg+) */}
            <section aria-labelledby="comparisonHeading" className="hidden lg:block">
               <h2 id="comparisonHeading" className="sr-only">
                  Feature comparison
               </h2>

               <div className="max-w-7xl mx-auto py-24 px-8">
                  <div className="w-full border-t border-gray-200 flex items-stretch">
                     <div className="-mt-px w-1/4 py-6 pr-4 flex items-end">
                        {/* <h3 className="mt-auto text-sm font-bold text-gray-900">{featureGroups[0].title}</h3> */}
                     </div>
                     {this.state.plans.map((plan, planIdx) => (
                        <div
                           key={plan.title}
                           aria-hidden="true"
                           className={classNames(planIdx === this.state.plans.length ? '' : 'pr-4', '-mt-px pl-4 w-1/4')}
                        >
                           <div
                              className={classNames(plan.featured ? 'border-lightblue-600' : 'border-transparent', 'py-6 border-t-2')}
                           >
                              <p className={classNames(plan.featured ? 'text-aqua' : 'text-lightblue-600', 'text-sm font-bold')}>
                                 {plan.title}
                              </p>
                              <p className="mt-2 text-sm text-gray-500">{plan.description}</p>
                           </div>
                        </div>
                     ))}
                  </div>
                  <div className="relative">
                     {this.state.featureGroups.map((group, groupIndex) => (
                        <span key={group.title}>
                  {
                     groupIndex != 0 && group.features.length > 1 &&
                     <h3 className={"mt-10 text-sm font-bold text-aqua"}>{group.title}</h3>
                  }
                           <div className={"relative" + (groupIndex != 0 ? " mt-6" : "")}>
                    {/* Fake card backgrounds */}
                              <div className="absolute inset-0 flex items-stretch pointer-events-none"
                                   aria-hidden="true">
                      <div className="w-1/4 pr-4" />
                      <div className="w-1/4 px-4">
                        <div className="w-full h-full bg-slate-700 rounded-lg shadow"/>
                      </div>
                      <div className="w-1/4 px-4">
                        <div className="w-full h-full bg-slate-700 rounded-lg shadow-md"/>
                      </div>
                      <div className="w-1/4 pl-4">
                        <div className="w-full h-full bg-slate-700 rounded-lg shadow"/>
                      </div>
                    </div>

                    <table className="relative w-full">
                      <caption className="sr-only">{group.title} comparison</caption>
                      <thead>
                        <tr className="text-left">
                          <th scope="col">
                            <span className="sr-only">{group.title}</span>
                          </th>
                           {this.state.plans.map((plan) => (
                              <th key={plan.title} scope="col">
                                 <span className="sr-only">{plan.title} plan</span>
                              </th>
                           ))}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-600">
                        {group.features.map((feature, featureIndex) => (
                           <tr key={feature.title}>
                              <th scope="row" onClick={() => this.toggleFeature(groupIndex, featureIndex)}
                                  className={classNames(
                              "w-1/4 py-3 pr-4 text-left text-sm font-medium",
                                     group.features.length == 1 ? 'font-bold text-aqua' : 'text-slate-400',
                                     feature.description ? 'cursor-pointer' : ''
                                  )}>
                                 {feature.title}
                                 {
                                    feature.description && (!!feature.visible ?
                                       <ChevronDownIcon className="h-5 w-5 inline pb-0.5" aria-hidden="true"/> :
                                       <ChevronRightIcon className="h-5 w-5 inline pb-0.5" aria-hidden="true"/>)
                                 }

                                 <div className={classNames(
                                    "text-sm font text-gray-500 mt-2",
                                    feature.visible && feature.description ? '' : 'hidden'
                                 )}>
                                    {feature.description}
                                 </div>
                              </th>
                              {feature.tiers.map((tier, tierIdx) => (
                                 <td
                                    key={tier.title}
                                    className={classNames(
                                       tierIdx === feature.tiers.length - 1 ? 'pl-4' : 'px-4',
                                  'relative w-1/4 py-0 text-center'
                                    )}
                                 >
                                <span className="relative w-full h-full py-3">
                                  {typeof tier.value === 'string' ? (
                                     <span
                                        className={classNames(
                                           tier.featured ? 'text-slate-200' : 'text-slate-400',
                                           'text-sm font-medium'
                                        )}
                                     >
                                      {tier.value}
                                    </span>
                                  ) : (
                                     <>
                                        {tier.value === true ? (
                                           <CheckIcon className="mx-auto h-5 w-5 text-lightblue-400"
                                                      aria-hidden="true"/>
                                        ) : (
                                           <XMarkIcon className="mx-auto h-5 w-5 text-gray-400" aria-hidden="true"/>
                                        )}

                                        <span className="sr-only">{tier.value === true ? 'Yes' : 'No'}</span>
                                     </>
                                  )}
                                </span>
                                 </td>
                              ))}
                           </tr>
                        ))}
                      </tbody>
                    </table>

                              {/* Fake card borders */}
                              <div className="absolute inset-0 flex items-stretch pointer-events-none"
                                   aria-hidden="true">
                      <div className="w-1/4 pr-4" />
                      <div className="w-1/4 px-4">
                        <div className="w-full h-full rounded-lg ring-1 ring-black ring-opacity-5"/>
                      </div>
                      <div className="w-1/4 px-4">
                        <div className="w-full h-full rounded-lg ring-1 ring-aqua shadow-lg shadow-aqua/50 ring-opacity-100"/>
                      </div>
                      <div className="w-1/4 pl-4">
                        <div className="w-full h-full rounded-lg ring-1 ring-black ring-opacity-5"/>
                      </div>
                    </div>
                  </div>
                </span>
                     ))}
                  </div>
               </div>
            </section>
         </div>
      );
   }
}

export default function PricingPage() {
   return (
      <div>
         <PricingPageTemplate />
         <Footer />
      </div>
   );
}


PricingPage.layoutProps = {
  meta: {
    title: 'Orbital - Pricing',
  },
}
