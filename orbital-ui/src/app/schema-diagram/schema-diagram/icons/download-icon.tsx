import * as React from 'react';

function DownloadIcon({
                        size = 24,
                        color = 'currentColor',
                        stroke = 2,
                        ...props
                      }) {
  return <svg xmlns="http://www.w3.org/2000/svg" className="icon icon-tabler icon-tabler-download"
              width={size}
              height={size}
              viewBox="0 0 24 24" strokeWidth={stroke} stroke={color} fill="none" strokeLinecap="round"
              strokeLinejoin="round">
    <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
    <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2"></path>
    <polyline points="7 11 12 16 17 11"></polyline>
    <line x1="12" y1="4" x2="12" y2="16"></line>
  </svg>;
}

export default DownloadIcon;
