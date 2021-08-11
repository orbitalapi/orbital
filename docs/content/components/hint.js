import React from "react"
import { MDXProvider } from "@mdx-js/react"
import { BiInfoCircle } from "react-icons/bi"
import styled from "@emotion/styled"

const divCommons = `
  position: relative;
  font-size: 1.125rem;
  padding: 1.5rem 3.5rem 1.5rem 2.8rem;
  background-color: #F5F7F9;
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
`

const floatingCommons = {
  display: "block",
  position: "absolute",
  lineHeight: 1.7,
  top: "1.5rem",
  left: "1rem",
  fontSize: "1.45rem",
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

export function Discourage({ children }) {
  return (
    <DiscourageDiv>
      <FloatingDiscourageDiv>
        <BiInfoCircle />
      </FloatingDiscourageDiv>
      <MDXProvider>{children}</MDXProvider>
    </DiscourageDiv>
  )
}

export function Hint({ children }) {
  return (
    <HintDiv>
      <FloatingHintDiv>
        <BiInfoCircle />
      </FloatingHintDiv>
      <MDXProvider>{children}</MDXProvider>
    </HintDiv>
  )
}
