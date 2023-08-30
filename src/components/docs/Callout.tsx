import clsx from 'clsx'

import {Icon} from '@/components/Icon'
import {ExclamationTriangleIcon, LightBulbIcon} from "@heroicons/react/24/outline";

const styles = {
  note: {
    container:
      'rounded-lg p-px bg-gradient-to-b from-sky-400 to-sky-700',
    title: 'text-sky-900 dark:text-sky-400',
    body: 'text-sky-800 [--tw-prose-background:theme(colors.sky.50)] prose-a:text-sky-900 prose-code:text-sky-900 dark:text-slate-300 dark:prose-code:text-slate-300',
  },
  warning: {
    container:
      'rounded-lg p-px bg-gradient-to-b from-amber-400 to-amber-600',
    title: 'text-amber-900 dark:text-amber-500',
    body: 'text-amber-800 [--tw-prose-underline:theme(colors.amber.400)] [--tw-prose-background:theme(colors.amber.50)] prose-a:text-amber-900 prose-code:text-amber-900 dark:text-slate-300 dark:[--tw-prose-underline:theme(colors.sky.700)] dark:prose-code:text-slate-300',
  },
}

const icons = {
  note: (props) => <LightBulbIcon {...props}></LightBulbIcon>,
  warning: (props) => <ExclamationTriangleIcon {...props}></ExclamationTriangleIcon>,
}

type CalloutProps = {
  type: 'note' | 'warning',
  title: string,
  children: any
}
export function Callout({type , title, children}:CalloutProps) {
  const calloutType = type || 'note';
  let IconComponent = icons[calloutType]
  const calloutTypeStyles = styles[calloutType]
  if (calloutTypeStyles === undefined) {
    throw new Error('No styles defined for calloutType ' + calloutType)
  }

  return (
    <div className={clsx('my-12 flex', styles[calloutType].container)}>
      <div className='bg-slate-800 rounded-lg p-6 w-full h-full'>
        <div className="flex-auto">
          { title ? (<div className='flex mb-4'>
            <IconComponent className={clsx("h-8 w-8 mr-4", styles[calloutType].title)}/>
            <p className={clsx('m-0 font-display text-xl', styles[calloutType].title)}>
              {title}
            </p>

          </div>) : (<></>)}

          <div className={clsx('prose', styles[calloutType].body)}>
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}
