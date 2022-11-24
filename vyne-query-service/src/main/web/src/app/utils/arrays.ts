export function arraysEqual(a: any[], b: any[]): boolean {
  if (a === b) return true;
  if (a == null || b == null) return false;
  if (a.length !== b.length) return false;

  return a.every((val, idx) => val === b[idx])
}

export function paginate<T>(array: T[], pageSize: number):T[][] {
  if (array === null || array === undefined) {
    return []
  }
  // https://stackoverflow.com/a/61074088/59015
  return array.reduce((acc, val, i) => {
    let idx = Math.floor(i / pageSize)
    let page = acc[idx] || (acc[idx] = [])
    page.push(val)

    return acc
  }, [])
}
