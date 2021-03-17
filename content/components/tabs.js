import React, { useState, useEffect } from "react"
import { v4 as uuidv4 } from "uuid"
import { MDXProvider } from "@mdx-js/react"
import tw, { styled } from "twin.macro"

const classCommons = ` py-4 px-6 block hover:text-blue-500 focus:outline-none`
const ContainerDiv = tw.div`bg-white mt-5 mb-4`
const TabsNav = tw.nav`flex flex-col sm:flex-row`
const TabsButton = styled.button`
  ${({ isActive }) => [
    !isActive && `
      color: #text-gray-600;
      background: rgba(244,246,248, 0.2);
      border: 1px solid #f4f6f8;`,
    isActive &&
      `color: #text-gray-600;
      background: #F4F6F8;
      border-color: #F4F6F8;
      border-bottom: 2px solid rgb(56,132,255);`,
    tw`${classCommons}`,
    isActive && tw`${classCommons} border-b-2 font-bold`,
  ]}
`
const Content = styled.div`
  background: #f4f6f8;
  ${tw`flex py-5 px-4 overflow-x-scroll`}
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
        {activeTab && ((activeTab.length > 1 && children[activeTab.indexOf(true)]) || children)}
      </div>
    </ContainerDiv>
  )
}
