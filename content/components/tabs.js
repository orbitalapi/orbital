import React, { useState, useEffect } from "react"
import { v4 as uuidv4 } from "uuid"
import { MDXProvider } from "@mdx-js/react"
import styled from "@emotion/styled"

const ContainerDiv = styled.div`
  background: #fff;
  margin-top: 1.25rem;
  margin-bottom: 1rem;
`
const TabsNav = styled.nav`
  display: flex;
  flex-direction: row;
`
//`flex flex-col sm:flex-row`
const TabsButton = styled.button`
  ${({ isActive }) => [
    `display: block;
     color: rgb(90, 98, 112);
     box-sizing: border-box;
     padding: 1rem 1.5rem;
     margin: 0;
     outline: none;
     border: none;
     box-shadow: none;
     -webkit-appearance: none;
     -moz-appearance: none;

     &:focus {
      outline: none;
     }

     &:hover {
      cursor: pointer;
      color: rgba(59,130,246,0.5);
     }`,
    !isActive &&
      `background: rgba(244,246,248, 0.2);
     border: 1px solid #f4f6f8;`,
    isActive &&
      `background: #F4F6F8;
       border-top: none;
       border-bottom: 2px solid rgb(56,132,255);
       font-weight: 700;`,
  ]}
`
const Content = styled.div`
  display: flex;
  flex-direction: column;
  background: #f4f6f8;
  overflow-x: scroll;
  padding: 1.25rem 1rem;

  & div:nth-last-of-type(2) {
    background: rgba(255, 255, 255, 0.8);
  }
`
export function TabsContent({ children }) {
  return (
    <Content>
      <MDXProvider>{children}</MDXProvider>
    </Content>
  )
}

export function Tabs({ tabsNav, children }) {
  const [activeTab, setActiveTab] = useState([])
  useEffect(() => {
    const newArr = new Array(tabsNav.length).fill(false)
    newArr[0] = true
    setActiveTab(newArr)
  }, [])
  const handleButtonClick = index => {
    let newArr = activeTab
    newArr = newArr.map((_, idx) => (idx == index && true) || false)
    setActiveTab(newArr)
  }
  return (
    <ContainerDiv>
      <TabsNav>
        {tabsNav &&
          tabsNav.map((title, index) => (
            <TabsButton
              key={uuidv4()}
              onClick={_ => handleButtonClick(index)}
              isActive={activeTab[index] || false}
            >
              {title}
            </TabsButton>
          ))}
      </TabsNav>
      <div className="panels-container">
        {activeTab &&
          ((activeTab.length > 1 && children[activeTab.indexOf(true)]) ||
            children)}
      </div>
    </ContainerDiv>
  )
}
