import React from "react"
import styled from "@emotion/styled"

//`flex flex-col sm:flex-row`
const Method = styled.span`
  ${({ isPost }) => [
    `display: inline-flex;
     align-items: center;
     padding: 2px 8px;
     border-radius: 9999px;
     color: #fff;
     font-size: inherit;
     font-weight: 700;
     letter-spacing: 1px;
     text-transform: uppercase;
     box-sizing: border-box;
     outline: none;`,
    !isPost && `background: rgb(56, 132, 255);`,
    isPost && `background: #2dac4d;`,
  ]}
`

export function HttpButton({ isPost = false }) {
  return <Method isPost={isPost}>{isPost ? "POST" : "GET"}</Method>
}
