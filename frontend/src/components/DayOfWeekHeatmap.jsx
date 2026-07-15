/**
 * Day-of-week heatmap — shows which days the user applies most.
 * Pure CSS/React component with HSL color intensity scaling.
 *
 * Expects `data` prop as array of { day: string, count: number } (7 items, Mon-Sun).
 */
export default function DayOfWeekHeatmap({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="h-32 flex items-center justify-center text-gray-300">
        No application data yet
      </div>
    )
  }

  const maxCount = Math.max(...data.map(d => d.count), 1)

  // HSL blue-based intensity: low count → light, high count → saturated
  function getCellColor(count) {
    if (count === 0) return '#f3f4f6' // gray-100
    const intensity = count / maxCount
    const lightness = 92 - (intensity * 47) // range: 92% (lightest) → 45% (darkest)
    const saturation = 40 + (intensity * 50) // range: 40% → 90%
    return `hsl(217, ${saturation}%, ${lightness}%)`
  }

  function getTextColor(count) {
    if (count === 0) return '#9ca3af'
    const intensity = count / maxCount
    return intensity > 0.55 ? '#ffffff' : '#1f2937'
  }

  const dayAbbreviations = {
    Monday: 'Mon', Tuesday: 'Tue', Wednesday: 'Wed',
    Thursday: 'Thu', Friday: 'Fri', Saturday: 'Sat', Sunday: 'Sun'
  }

  return (
    <div>
      <div className="grid grid-cols-7 gap-2">
        {data.map(({ day, count }) => (
          <div
            key={day}
            className="relative rounded-lg p-3 text-center transition-all duration-200 hover:scale-105 hover:shadow-md"
            style={{ backgroundColor: getCellColor(count) }}
          >
            <div
              className="text-xs font-medium mb-1"
              style={{ color: getTextColor(count), opacity: 0.8 }}
            >
              {dayAbbreviations[day] ?? day}
            </div>
            <div
              className="text-lg font-bold"
              style={{ color: getTextColor(count) }}
            >
              {count}
            </div>
          </div>
        ))}
      </div>

      {/* Color legend */}
      <div className="flex items-center justify-end gap-2 mt-3">
        <span className="text-xs text-gray-400">Less</span>
        <div className="flex gap-0.5">
          {[0, 0.2, 0.4, 0.6, 0.8, 1.0].map((intensity, i) => {
            const lightness = 92 - (intensity * 47)
            const saturation = 40 + (intensity * 50)
            const bg = intensity === 0
              ? '#f3f4f6'
              : `hsl(217, ${saturation}%, ${lightness}%)`
            return (
              <div
                key={i}
                className="w-4 h-4 rounded-sm"
                style={{ backgroundColor: bg }}
              />
            )
          })}
        </div>
        <span className="text-xs text-gray-400">More</span>
      </div>
    </div>
  )
}
