import { Sankey, Tooltip, ResponsiveContainer, Rectangle, Layer } from 'recharts'

const STATUS_COLORS = {
  APPLIED:   '#3b82f6',
  SCREENING: '#f59e0b',
  INTERVIEW: '#8b5cf6',
  OFFER:     '#10b981',
  REJECTED:  '#ef4444',
  STALE:     '#9ca3af'
}

/**
 * Custom node renderer for the Sankey diagram.
 * Renders a colored rectangle with the status label.
 */
function SankeyNode({ x, y, width, height, index, payload }) {
  const name = payload?.name ?? ''
  const color = STATUS_COLORS[name] ?? '#6b7280'

  return (
    <Layer key={`node-${index}`}>
      <Rectangle
        x={x}
        y={y}
        width={width}
        height={height}
        fill={color}
        fillOpacity={0.9}
        rx={4}
        ry={4}
      />
      <text
        x={x + width / 2}
        y={y + height / 2}
        textAnchor="middle"
        dominantBaseline="central"
        fill="#fff"
        fontSize={11}
        fontWeight={600}
      >
        {name}
      </text>
    </Layer>
  )
}

/**
 * Custom link renderer with gradient coloring between source and target.
 */
function SankeyLink({ sourceX, targetX, sourceY, targetY, sourceControlX,
                       targetControlX, linkWidth, index, payload }) {
  const sourceName = payload?.source?.name ?? ''
  const targetName = payload?.target?.name ?? ''
  const sourceColor = STATUS_COLORS[sourceName] ?? '#6b7280'
  const targetColor = STATUS_COLORS[targetName] ?? '#6b7280'
  const gradientId = `link-gradient-${index}`

  return (
    <Layer key={`link-${index}`}>
      <defs>
        <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor={sourceColor} stopOpacity={0.4} />
          <stop offset="100%" stopColor={targetColor} stopOpacity={0.4} />
        </linearGradient>
      </defs>
      <path
        d={`
          M${sourceX},${sourceY + linkWidth / 2}
          C${sourceControlX},${sourceY + linkWidth / 2}
            ${targetControlX},${targetY + linkWidth / 2}
            ${targetX},${targetY + linkWidth / 2}
          L${targetX},${targetY - linkWidth / 2}
          C${targetControlX},${targetY - linkWidth / 2}
            ${sourceControlX},${sourceY - linkWidth / 2}
            ${sourceX},${sourceY - linkWidth / 2}
          Z
        `}
        fill={`url(#${gradientId})`}
        strokeWidth={0}
      />
    </Layer>
  )
}

/**
 * Sankey chart showing application status flow.
 * Expects `links` prop as array of { source: string, target: string, value: number }.
 */
export default function SankeyChart({ links }) {
  if (!links || links.length === 0) {
    return (
      <div className="h-64 flex items-center justify-center text-gray-300">
        No transition data yet — apply to some jobs first
      </div>
    )
  }

  // Build unique node list from link sources and targets
  const nodeNames = [...new Set(links.flatMap(l => [l.source, l.target]))]
  // Order nodes by pipeline stage
  const stageOrder = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE']
  nodeNames.sort((a, b) => {
    const ai = stageOrder.indexOf(a)
    const bi = stageOrder.indexOf(b)
    return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
  })

  const nodes = nodeNames.map(name => ({ name }))
  const nodeIndex = Object.fromEntries(nodeNames.map((name, i) => [name, i]))

  const sankeyLinks = links.map(l => ({
    source: nodeIndex[l.source],
    target: nodeIndex[l.target],
    value: l.value
  }))

  const data = { nodes, links: sankeyLinks }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <Sankey
        data={data}
        nodeWidth={20}
        nodePadding={24}
        margin={{ top: 16, right: 60, bottom: 16, left: 60 }}
        link={<SankeyLink />}
        node={<SankeyNode />}
      >
        <Tooltip
          formatter={(value) => [`${value} applications`, 'Count']}
          contentStyle={{
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            fontSize: '12px',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)'
          }}
        />
      </Sankey>
    </ResponsiveContainer>
  )
}
