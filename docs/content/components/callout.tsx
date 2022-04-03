import * as React from 'react'
import {MDXProvider} from '@mdx-js/react'
import styled from 'styled-components';
import colors from './colors'
import {FiAlertOctagon, FiAlertTriangle, FiInfo} from 'react-icons/fi';

const components = {}

export type CalloutType = 'info' | 'hint' | 'warning' | 'danger';
export type CalloutTheme = {
   [key in CalloutType]: CalloutColors;
}

export interface CalloutColors {
   header: string;
   border: string;
   background: string;
   icon: any;
}

function colorScheme(colors: any, icon: any): CalloutColors {
   return {
      header: colors['100'],
      border: colors['400'],
      background: colors['50'],
      icon
   }
}

const calloutTheme: CalloutTheme = {
   info: colorScheme(colors.gray, null),
   hint: colorScheme(colors.blue, FiInfo({color: colors.blue['600']})),
   warning: colorScheme(colors.amber, FiAlertTriangle({color: colors.amber['600']})),
   danger: colorScheme(colors.red, FiAlertOctagon({color: colors.red['600']}))
}

const Header = styled.div`
    background-color: ${(props: CalloutProps) => calloutTheme[props.type].header};
    font-weight: bold;
    padding: 0.5rem 1rem;
    svg {
      margin-right: 1rem
   }
   display: flex;
   align-items: center;

`
const StyledCallout = styled.div`
   border-left: 4px solid ${(props: CalloutProps) => calloutTheme[props.type].border};
   border-radius: 4px;
   background-color: #f3f3f355;
   margin-bottom: 2rem;
`

const CalloutBody = styled.div`
  padding: 1rem;
  & > *:last-child {
    margin-bottom: 0;
  }
  &.large p {
   font-size: 1.3rem !important;
  }
`

interface CalloutProps {
   title: string
   type: CalloutType
   size?: 'normal' | 'large';
}

export const Callout: React.FunctionComponent<CalloutProps> = (props) => (
   <StyledCallout type={props.type}>
      <Header type={props.type}>
         {calloutTheme[props.type].icon}
         <span>{props.title}</span>
      </Header>
      <CalloutBody className={props.size || ''}>
         <MDXProvider components={components}>
            {props.children}
         </MDXProvider>
      </CalloutBody>
   </StyledCallout>
)
