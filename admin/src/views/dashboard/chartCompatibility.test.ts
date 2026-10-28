import { describe, expect, it } from 'vitest'
import { init, use, type EChartsCoreOption } from 'echarts/core'
import { SVGRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'

use([SVGRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent])

describe('dashboard chart runtime compatibility', () => {
  it.each(['line', 'bar', 'pie'])('renders a real %s chart using the patched ECharts runtime', (type) => {
    const chart = init(null, undefined, { renderer: 'svg', ssr: true, width: 640, height: 320 })
    try {
      const cartesian = type !== 'pie'
      const option: EChartsCoreOption = {
        animation: false,
        ...(cartesian ? { xAxis: { type: 'category', data: ['Mon', 'Tue', 'Wed'] }, yAxis: { type: 'value' } } : {}),
        series: [{ type, data: cartesian ? [1, 3, 2] : [{ name: 'Active', value: 3 }, { name: 'Other', value: 2 }] }],
      }
      chart.setOption(option)
      const svg = chart.renderToSVGString()
      expect(svg).toContain('<svg')
      expect(svg).toContain('<path')
      expect(svg).not.toContain('NaN')
      if (cartesian) expect(svg).toContain('Mon')
      else expect(svg).toContain('Active')
    } finally {
      chart.dispose()
    }
  })
})
