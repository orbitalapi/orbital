import {FunctionComponent} from 'react';
import styled from 'styled-components';
import {FiPlusCircle, FiMinusCircle} from 'react-icons/fi';
import colors from './colors'

interface ProConListProps {
   pros: string[]
   cons: string[]
}

const StyledProConList = styled.div`
   ul {
      margin-left: 0;
      svg {
         margin-right: 1rem;
      }

      li {
         display: flex;
         align-items: center;
      }
      li.pro {
         color: green
      }
      li.con {
         color: red
      }
   }
`

export const ProConList: FunctionComponent<ProConListProps> = (props) => (
   <StyledProConList>
      <h5>Pros / Cons</h5>
      <ul>
         { props.pros.map(pro => <li key={pro} className='pro'><FiPlusCircle color={colors.green['600']} /> {pro}</li>)}
         { props.cons.map(con => <li key={con} className='con'><FiMinusCircle color={colors.red['600']} />{con}</li>)}
      </ul>
   </StyledProConList>
)
