import {FunctionComponent} from 'react';
import styled from 'styled-components';
import {FiPlusCircle, FiMinusCircle} from 'react-icons/fi';

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
         list-style-type: none;

         &:before {
            display:none;
         }
      }
      .pro {
         color: green
      }
      .con {
         color: red
      }
   }
`

export const ProConList: FunctionComponent<ProConListProps> = (props) => (
   <StyledProConList>
      <h5>Pros / Cons</h5>
      <ul>
         { props.pros.map(pro => <li key={pro} ><span className='pro'><FiPlusCircle /></span>{pro}</li>)}
         { props.cons.map(con => <li key={con} ><span className='con'><FiMinusCircle /></span>{con}</li>)}
      </ul>
   </StyledProConList>
)
