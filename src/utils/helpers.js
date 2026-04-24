export const NODE_W = 244
export const NODE_H = 116

let _counter = 1000
export function genId() {
  return String(++_counter)
}

export function getAccent(type) {
  if (type === 'start')
    return {
      color: '#00e5aa',
      glow: 'rgba(0,229,170,0.35)',
      bg: 'rgba(0,229,170,0.08)',
      border: 'rgba(0,229,170,0.22)',
    }
  if (type === 'question')
    return {
      color: '#ffba45',
      glow: 'rgba(255,186,69,0.35)',
      bg: 'rgba(255,186,69,0.08)',
      border: 'rgba(255,186,69,0.22)',
    }
  return {
    color: '#ff6e8a',
    glow: 'rgba(255,110,138,0.35)',
    bg: 'rgba(255,110,138,0.08)',
    border: 'rgba(255,110,138,0.22)',
  }
}

export function buildNodeMap(nodes) {
  return Object.fromEntries(nodes.map((n) => [n.id, n]))
}

export function buildConnections(nodes, nodeMap) {
  const list = []
  nodes.forEach((node) => {
    node.options.forEach((opt) => {
      if (nodeMap[opt.nextId]) {
        list.push({ srcId: node.id, tgtId: opt.nextId, label: opt.label })
      }
    })
  })
  return list
}
