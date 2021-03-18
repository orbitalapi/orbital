import React from "react"
import { MDXProvider } from "@mdx-js/react"
import { BiInfoCircle } from "react-icons/bi"
import styled from "@emotion/styled"

const divCommons = `
  position: relative;
  font-size: 1.125rem;
  padding: 30px 50px;
  background-color: #F5F7F9;
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
`

const floatingCommons = {
  display: "block",
  position: "absolute",
  top: 30,
  left: 16,
  fontSize: 24,
  padding: 0,
}

const HintDiv = styled.div`
  ${divCommons}
  border-left: "4px solid rgb(56, 132, 255)";

  & > *:last-child {
    margin: 0;
  }
`

const FloatingHintDiv = styled.div({
  ...floatingCommons,
  color: "rgb(56, 132, 255)",
})

const DiscourageDiv = styled.div`
  ${divCommons}
  border-left: "4px solid rgb(247, 125, 5)";
`

const FloatingDiscourageDiv = styled.div({
  ...floatingCommons,
  color: "rgb(247, 125, 5)",
})

export function Prefer({ props }) {
  return (
    <div>
      <MDXProvider>
        ```taxi
        {props.children}
        ```
      </MDXProvider>
    </div>
  )
}
export function Discourage({ type, ...props }) {
  return (
    <DiscourageDiv>
      <FloatingDiscourageDiv>
        <BiInfoCircle />
      </FloatingDiscourageDiv>
      <MDXProvider>{props.children}</MDXProvider>
    </DiscourageDiv>
  )
}
export function Hint({ type, ...props }) {
  return (
    <HintDiv>
      <FloatingHintDiv>
        <BiInfoCircle />
      </FloatingHintDiv>
      <MDXProvider>{props.children}</MDXProvider>
    </HintDiv>
  )
}
