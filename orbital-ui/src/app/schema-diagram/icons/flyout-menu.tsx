import React, {useEffect, useState} from 'react';
import LayoutDirectionIcon from './layout-direction-icon';

const FlyoutMenu = ({ layoutDirection, onDirectionChange }) => {
  const [isMenuVisible, setMenuVisible] = useState(false);
  const [selectedDirection, setSelectedDirection] = useState(layoutDirection);
  const [hoveredDirection, setHoveredDirection] = useState(null);

  const layoutDirections = ['RIGHT', 'DOWN', 'NONE'];

  const handleDirectionClick = (direction) => {
    setSelectedDirection(direction);
    onDirectionChange(direction);
    setMenuVisible(false); // Close the menu
  };

  useEffect(() => {
    setSelectedDirection(layoutDirection);
  }, [layoutDirection]);

  const directionTooltipMap = {
    'RIGHT': 'layout horizontally',
    'DOWN': 'layout vertically',
    'NONE': 'disable auto layout (useful for manual layout and sharing diagrams with the \'copy as markdown\' button)',
  }

  return (
    <div
      style={{
        position: 'relative',
        display: 'inline-block',
        cursor: 'pointer',
      }}
      onMouseEnter={() => setMenuVisible(true)}
      onMouseLeave={() => setMenuVisible(false)}
    >
      {/* Current Selected Icon */}
      <LayoutDirectionIcon layoutDirection={selectedDirection} doPulse={true} />

      {/* Flyout Menu */}
      {isMenuVisible && (
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '-10.5px',
            transform: 'translateY(-53%)',
            display: 'flex',
            background: '#fff',
            padding: '2px',
            border: '1px solid #ccc',
            boxShadow: '0 3px 3px rgba(0, 0, 0, 0.1)',
            zIndex: 100,
          }}
        >
          {layoutDirections.map((direction) => (
            <div
              key={direction}
              title={directionTooltipMap[direction]}
              onClick={() => handleDirectionClick(direction)}
              onMouseEnter={() => setHoveredDirection(direction)}
              onMouseLeave={() => setHoveredDirection(null)}
              style={{
                width: '24px',
                height: '24px',
                padding: '2px',
                background:
                  direction === selectedDirection
                    ? '#007BFF55'
                    : direction === hoveredDirection
                      ? '#007BFF22'
                      : 'transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <LayoutDirectionIcon layoutDirection={direction} doPulse={false} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default FlyoutMenu;
