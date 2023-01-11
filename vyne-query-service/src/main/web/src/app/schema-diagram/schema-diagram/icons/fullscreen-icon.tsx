import * as React from 'react';

function FullScreenIcon({
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
    <polyline points="16 4 20 4 20 8"></polyline>
    <line x1="14" y1="10" x2="20" y2="4"></line>
    <polyline points="8 20 4 20 4 16"></polyline>
    <line x1="4" y1="20" x2="10" y2="14"></line>
    <polyline points="16 20 20 20 20 16"></polyline>
    <line x1="14" y1="14" x2="20" y2="20"></line>
    <polyline points="8 4 4 4 4 8"></polyline>
    <line x1="4" y1="4" x2="10" y2="10"></line>
  </svg>;
}

export default FullScreenIcon;
