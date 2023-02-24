import clsx from 'clsx'
import {ApiIcon} from "@/components/icons/api";
import {RingIcon} from "@/components/icons/ring";
import {RocketOneIcon} from "@/components/icons/rocket-one-icon";
import {LeftBranchIcon} from "@/components/icons/left-branch";
import {GuideIcon} from "@/components/icons/guide";

const icons = {
  connect: ApiIcon,
  query: RingIcon,
  production: RocketOneIcon,
  pipelines: LeftBranchIcon,
  guides: GuideIcon,
}

const iconStyles = {
  blue: '[--icon-foreground:theme(colors.slate.900)] [--icon-background:theme(colors.white)]',
  amber:
    '[--icon-foreground:theme(colors.amber.900)] [--icon-background:theme(colors.amber.100)]',
}

export function Icon({ color = 'blue', icon, className, ...props }) {
  let IconComponent = icons[icon]

  return (

      <IconComponent color={color} />
  )
}

const gradients = {
  blue: [
    { stopColor: '#0EA5E9' },
    { stopColor: '#22D3EE', offset: '.527' },
    { stopColor: '#818CF8', offset: 1 },
  ],
  amber: [
    { stopColor: '#FDE68A', offset: '.08' },
    { stopColor: '#F59E0B', offset: '.837' },
  ],
}

export function Gradient({ color = 'blue', ...props }) {
  return (
    <radialGradient
      cx={0}
      cy={0}
      r={1}
      gradientUnits="userSpaceOnUse"
      {...props}
    >
      {gradients[color].map((stop, stopIndex) => (
        <stop key={stopIndex} {...stop} />
      ))}
    </radialGradient>
  )
}

export function LightMode({ className, ...props }) {
  return <g className={clsx('dark:hidden', className)} {...props} />
}

export function DarkMode({ className, ...props }) {
  return <g className={clsx('hidden dark:inline', className)} {...props} />
}
