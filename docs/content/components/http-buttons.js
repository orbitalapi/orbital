import React from "react"
import styled from "@emotion/styled"

const Heading = styled.div`
  display: flex;
  align-items: center;
`

//`flex flex-col sm:flex-row`
const Method = styled.span`
  ${({ isPost }) => [
    `display: inline-flex;
     align-items: center;
     padding: 0.5rem 0.75rem;
     border-radius: 1rem;
     color: #fff;
     font-size: 1rem;
     margin-right: 0.5rem;
     font-weight: 700;
     letter-spacing: 1px;
     text-transform: uppercase;
     box-sizing: border-box;
     outline: none;`,
    !isPost && `background: rgb(56, 132, 255);`,
    isPost && `background: #2dac4d;`,
  ]}
`

export function HeadingWrapper({ children }) {
  return <Heading>{children}</Heading>
}

export function HttpButton({ isPost = false }) {
  return <Method isPost={isPost}>{isPost ? "POST" : "GET"}</Method>
}
