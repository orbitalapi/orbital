export function outerRectangle(width: number, height: number): string {
  return roundedRectangle(0, 0, width, height, 4, 4, 4, 4);
}

export function innerRectangle(width: number, height: number): string {
  return roundedRectangle(0, 0, width, height, 4, 0, 0, 4);
}

export function roundedRectangle(x: number, y: number, width: number, height: number, tl: number, tr: number, br: number, bl: number) {

  const TR = [1, 1];
  const BR = [-1, 1];
  const BL = [-1, -1];
  const TL = [1, -1];

  function arc(rad: number, corner) {
    // If the radius is 0 (ie., no arc), then just bail.
    if (rad === 0) {
      return '';
    }
    // Convert radius to -radius, depending on corner we're in.
    const arcX = rad * corner[0];
    const arcY = rad * corner[1];
    return `a${rad},${rad} 0 0 1 ${arcX},${arcY}`;
  }

  function horizontal(s: number) {
    return `h${s}`;
  }

  function vertical(s: number) {
    return `v${s}`;
  }

  // From https://stackoverflow.com/a/38118843
  // M100,100
  // h200
  // a20,20 0 0 1 20,20
  // v200
  // a20,20 0 0 1 -20,20
  // h-200
  // a20,20 0 0 1 -20,-20
  // v-200
  // a20,20 0 0 1 20,-20 z
  return [
    `M${tl},0`,
    horizontal(width - (tl + tr)),
    arc(tr, TR),
    vertical(height - (tr + br)),
    arc(br, BR),
    horizontal((width * -1) + (tr + bl)),
    arc(bl, BL),
    vertical((height * -1) + (bl + tl)),
    arc(tl, TL),
    'z'
  ].join(' ');
}
