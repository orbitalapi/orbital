import * as React from 'react';

function CopyAsMarkdownIcon({
                        size = 24,
                        color = 'currentColor',
                        stroke = 2,
                        ...props
                      }) {
  return <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none"
              stroke={color} strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
              className="icon icon-tabler icons-tabler-outline icon-tabler-share">
    <path stroke="none" d="M0 0h24v24H0z" fill="none" />
    <path d="M6 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" />
    <path d="M18 6m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" />
    <path d="M18 18m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" />
    <path d="M8.7 10.7l6.6 -3.4" />
    <path d="M8.7 13.3l6.6 3.4" />
  </svg>;
}

export default CopyAsMarkdownIcon;
