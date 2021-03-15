import React from "react"
import { MDXProvider } from "@mdx-js/react"
import { BiInfoCircle } from "react-icons/bi"
import styled from "@emotion/styled"

const divCommons = {
  position: "relative",
  fontSize: "1.125rem",
  lineHeight: 1.7,
  padding: "24px 24px 24px 50px",
  marginBottom: "1.45rem",
  backgroundColor: "#F5F7F9",
}

const floatingCommons = {
  display: "block",
  position: "absolute",
  top: 24,
  left: 16,
  fontSize: 24,
  padding: 0,
}

const HintDiv = styled.div({
  ...divCommons,
  borderLeft: "4px solid rgb(56, 132, 255)",
})

const FloatingHintDiv = styled.div({
  ...floatingCommons,
  color: "rgb(56, 132, 255)",
})

const DiscourageDiv = styled.div({
  ...divCommons,
  borderLeft: "4px solid rgb(247, 125, 5)",
})

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
