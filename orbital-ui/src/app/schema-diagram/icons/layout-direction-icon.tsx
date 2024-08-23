import * as React from 'react';

function LayoutDirectionIcon({
                        size = 24,
                        color = 'currentColor',
                        stroke = 2,
                        layoutDirection = 'DOWN',
                        ...props
                      }) {
  return layoutDirection === 'DOWN' ? (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color}
         stroke-width={stroke} stroke-linecap="round" stroke-linejoin="round"
         className="icon icon-tabler icons-tabler-outline icon-tabler-arrows-vertical">
      <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
      <path d="M8 7l4 -4l4 4"/>
      <path d="M8 17l4 4l4 -4"/>
      <path d="M12 3l0 18"/>
    </svg>)
  :
  (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color}
         stroke-width={stroke} stroke-linecap="round" stroke-linejoin="round"
         className="icon icon-tabler icons-tabler-outline icon-tabler-arrows-horizontal">
      <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
      <path d="M7 8l-4 4l4 4"/>
      <path d="M17 8l4 4l-4 4"/>
      <path d="M3 12l18 0"/>
    </svg>
  );
}

export default LayoutDirectionIcon;
