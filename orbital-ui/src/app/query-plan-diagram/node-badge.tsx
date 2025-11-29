import * as React from 'react';
import { getNodeIcon } from '../schema-diagram/diagram-nodes/node-icon-mapping';
import { InlineSvg } from './inline-svg';

interface NodeBadgeProps {
  label: string;
  cssClass: string;
  iconId?: string | null;
}

/**
 * Reusable badge component for diagram nodes.
 * Displays a label with an optional icon, using a flex layout.
 */
export function NodeBadge({ label, cssClass, iconId }: NodeBadgeProps): React.JSX.Element {
  const iconPath = getNodeIcon(iconId);

  return (
    <span className={`badge ${cssClass}`}>
      {iconPath && <InlineSvg src={iconPath} className={'badge-icon'} width={14} height={14} />}
      {label}
    </span>
  );
}
