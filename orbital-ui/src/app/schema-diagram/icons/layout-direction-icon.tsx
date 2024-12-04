import * as React from 'react';

function LayoutDirectionIcon({
                        layoutDirection,
                        doPulse,
                        size = 24,
                        color = 'currentColor',
                        stroke = 2,
                        ...props
                      }) {
  // only applied to the NONE icon
  const pulsingStyle = {
    animation: 'pulse 1.25s infinite ease-in-out',
  };

  return layoutDirection === 'DOWN' ? (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color}
         strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
         className="icon icon-tabler icons-tabler-outline icon-tabler-arrows-vertical">
      <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
      <path d="M8 7l4 -4l4 4"/>
      <path d="M8 17l4 4l4 -4"/>
      <path d="M12 3l0 18"/>
    </svg>
    ) : layoutDirection === 'RIGHT' ?
    (
      <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color}
           strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
           className="icon icon-tabler icons-tabler-outline icon-tabler-arrows-horizontal">
        <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
        <path d="M7 8l-4 4l4 4"/>
        <path d="M17 8l4 4l-4 4"/>
        <path d="M3 12l18 0"/>
      </svg>
    ) :
    ( // NONE
      <>
        <style>
          {`
            @keyframes pulse {
              0%, 100% {
                transform: scale(1);
                color: ${color}
              }
              50% {
                transform: scale(1.15);
                color: #C76B02;
              }
            }
          `}
        </style>
        <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color}
             strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
             className="icon icon-tabler icons-tabler-outline icon-tabler-layout-off"
             style={doPulse ? pulsingStyle : null}>
          <path stroke="none" d="M0 0h24v24H0z" fill="none" />
          <path
            d="M8 4a2 2 0 0 1 2 2m-1.162 2.816a1.993 1.993 0 0 1 -.838 .184h-2a2 2 0 0 1 -2 -2v-1c0 -.549 .221 -1.046 .58 -1.407" />
          <path d="M4 13m0 2a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2z" />
          <path
            d="M14 10v-4a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v10m-.595 3.423a2 2 0 0 1 -1.405 .577h-2a2 2 0 0 1 -2 -2v-4" />
          <path d="M3 3l18 18" />
        </svg>
      </>
    );
}

export default LayoutDirectionIcon;
