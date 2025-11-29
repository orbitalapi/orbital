import * as React from "react";

function LayoutIcon({
                      size = 24,
                      color = "currentColor",
                      stroke = 2,
                      ...props
                    }) {
  return <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none"
              stroke={color} strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
              className="icon icon-tabler icons-tabler-outline icon-tabler-layout">
    <path stroke="none" d="M0 0h24v24H0z" fill="none" />
    <path d="M4 4m0 2a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v1a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2z" />
    <path d="M4 13m0 2a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2z" />
    <path d="M14 4m0 2a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2z" />
  </svg>;
}

export default LayoutIcon;
