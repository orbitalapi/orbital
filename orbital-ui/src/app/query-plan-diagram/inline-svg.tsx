import * as React from 'react';
import { useEffect, useState } from 'react';

interface InlineSvgProps {
  src: string;
  className?: string;
  width?: number | string;
  height?: number | string;
}

/**
 * Loads and renders an external SVG inline so it can inherit CSS properties like currentColor.
 */
export function InlineSvg({ src, className, width, height }: InlineSvgProps): React.JSX.Element | null {
  const [svg, setSvg] = useState<string | null>(null);

  useEffect(() => {
    fetch(src)
      .then(res => res.text())
      .then(setSvg);
  }, [src]);

  const style: React.CSSProperties = {};
  if (width !== undefined) style.width = typeof width === 'number' ? `${width}px` : width;
  if (height !== undefined) style.height = typeof height === 'number' ? `${height}px` : height;
  style.display = 'flex'
  style.alignItems = 'center'

  return svg
    ? <div className={className} style={style} dangerouslySetInnerHTML={{ __html: svg }} />
    : null;
}
