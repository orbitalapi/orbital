import * as React from 'react';

function MinimizeIcon({
                        size = 24,
                        color = 'currentColor',
                        stroke = 2,
                        ...props
                      }) {
  return <svg xmlns="http://www.w3.org/2000/svg" className="icon icon-tabler icon-tabler-arrows-maximize"
              width={size}
              height={size} viewBox="0 0 24 24"
              strokeWidth={stroke} stroke={color} fill="none" strokeLinecap="round"
              strokeLinejoin="round">
    <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
    <polyline points="5 9 9 9 9 5"></polyline>
    <line x1="3" y1="3" x2="9" y2="9"></line>
    <polyline points="5 15 9 15 9 19"></polyline>
    <line x1="3" y1="21" x2="9" y2="15"></line>
    <polyline points="19 9 15 9 15 5"></polyline>
    <line x1="15" y1="9" x2="21" y2="3"></line>
    <polyline points="19 15 15 15 15 19"></polyline>
    <line x1="15" y1="15" x2="21" y2="21"></line>
  </svg>;
}

export default MinimizeIcon;
