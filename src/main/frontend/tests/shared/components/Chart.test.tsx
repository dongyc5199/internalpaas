import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Chart, type ChartDataPoint } from '../../../src/shared/components/Chart/Chart';
import '@testing-library/jest-dom';

// Mock Recharts components
vi.mock('recharts', () => ({
  LineChart: ({ children }: any) => <div data-testid="line-chart">{children}</div>,
  Line: ({ dataKey }: any) => <div data-testid={`line-${dataKey}`} />,
  BarChart: ({ children }: any) => <div data-testid="bar-chart">{children}</div>,
  Bar: ({ dataKey }: any) => <div data-testid={`bar-${dataKey}`} />,
  AreaChart: ({ children }: any) => <div data-testid="area-chart">{children}</div>,
  Area: ({ dataKey }: any) => <div data-testid={`area-${dataKey}`} />,
  PieChart: ({ children }: any) => <div data-testid="pie-chart">{children}</div>,
  Pie: ({ dataKey }: any) => <div data-testid={`pie-${dataKey}`} />,
  Cell: ({ fill }: any) => <div data-testid="pie-cell" data-fill={fill} />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
  Legend: () => <div data-testid="legend" />,
  ResponsiveContainer: ({ children }: any) => <div data-testid="responsive-container">{children}</div>,
}));

describe('Chart', () => {
  const mockData: ChartDataPoint[] = [
    { date: '2024-01', value: 100 },
    { date: '2024-02', value: 120 },
    { date: '2024-03', value: 150 },
  ];

  describe('基本渲染', () => {
    it('应该渲染折线图', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
      expect(screen.getByTestId('line-value')).toBeInTheDocument();
    });

    it('应该渲染柱状图', () => {
      render(<Chart type="bar" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
      expect(screen.getByTestId('bar-value')).toBeInTheDocument();
    });

    it('应该渲染面积图', () => {
      render(<Chart type="area" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('area-chart')).toBeInTheDocument();
      expect(screen.getByTestId('area-value')).toBeInTheDocument();
    });

    it('应该渲染饼图', () => {
      render(<Chart type="pie" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('pie-chart')).toBeInTheDocument();
      expect(screen.getByTestId('pie-value')).toBeInTheDocument();
    });
  });

  describe('标题显示', () => {
    it('应该显示标题', () => {
      const title = '测试图表';
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" title={title} />);

      expect(screen.getByText(title)).toBeInTheDocument();
    });

    it('应该在没有标题时不显示', () => {
      const { container } = render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" />
      );

      const titleElement = container.querySelector('.chartTitle');
      expect(titleElement).not.toBeInTheDocument();
    });
  });

  describe('加载状态', () => {
    it('应该显示加载骨架屏', () => {
      const { container } = render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" loading={true} />
      );

      expect(container.querySelector('.skeleton')).toBeInTheDocument();
      expect(container.querySelector('.skeletonPulse')).toBeInTheDocument();
    });

    it('应该在加载时不渲染图表', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" loading={true} />);

      expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument();
    });

    it('应该在加载时显示标题', () => {
      const title = '加载中的图表';
      render(
        <Chart
          type="line"
          data={mockData}
          xKey="date"
          yKey="value"
          title={title}
          loading={true}
        />
      );

      expect(screen.getByText(title)).toBeInTheDocument();
    });
  });

  describe('空数据处理', () => {
    it('应该显示空数据提示', () => {
      render(<Chart type="line" data={[]} xKey="date" yKey="value" />);

      expect(screen.getByText('暂无数据')).toBeInTheDocument();
    });

    it('应该在数据为null时显示空数据提示', () => {
      render(<Chart type="line" data={null as any} xKey="date" yKey="value" />);

      expect(screen.getByText('暂无数据')).toBeInTheDocument();
    });

    it('应该在空数据时显示标题', () => {
      const title = '空数据图表';
      render(<Chart type="line" data={[]} xKey="date" yKey="value" title={title} />);

      expect(screen.getByText(title)).toBeInTheDocument();
    });

    it('应该在空数据时显示图标', () => {
      const { container } = render(
        <Chart type="line" data={[]} xKey="date" yKey="value" />
      );

      expect(container.querySelector('[data-lucide="bar-chart-3"]')).toBeInTheDocument();
    });
  });

  describe('多数据系列', () => {
    const multiSeriesData = [
      { date: '2024-01', cpu: 50, memory: 60 },
      { date: '2024-02', cpu: 65, memory: 70 },
      { date: '2024-03', cpu: 80, memory: 75 },
    ];

    it('应该渲染多条折线', () => {
      render(
        <Chart type="line" data={multiSeriesData} xKey="date" yKey={['cpu', 'memory']} />
      );

      expect(screen.getByTestId('line-cpu')).toBeInTheDocument();
      expect(screen.getByTestId('line-memory')).toBeInTheDocument();
    });

    it('应该渲染多个柱状图', () => {
      render(
        <Chart type="bar" data={multiSeriesData} xKey="date" yKey={['cpu', 'memory']} />
      );

      expect(screen.getByTestId('bar-cpu')).toBeInTheDocument();
      expect(screen.getByTestId('bar-memory')).toBeInTheDocument();
    });

    it('应该渲染多个面积图', () => {
      render(
        <Chart type="area" data={multiSeriesData} xKey="date" yKey={['cpu', 'memory']} />
      );

      expect(screen.getByTestId('area-cpu')).toBeInTheDocument();
      expect(screen.getByTestId('area-memory')).toBeInTheDocument();
    });
  });

  describe('图表配置选项', () => {
    it('应该默认显示网格', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('cartesian-grid')).toBeInTheDocument();
    });

    it('应该在showGrid为false时隐藏网格', () => {
      render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" showGrid={false} />
      );

      expect(screen.queryByTestId('cartesian-grid')).not.toBeInTheDocument();
    });

    it('应该默认显示图例', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('legend')).toBeInTheDocument();
    });

    it('应该在showLegend为false时隐藏图例', () => {
      render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" showLegend={false} />
      );

      expect(screen.queryByTestId('legend')).not.toBeInTheDocument();
    });

    it('应该默认显示工具提示', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });

    it('应该在showTooltip为false时隐藏工具提示', () => {
      render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" showTooltip={false} />
      );

      expect(screen.queryByTestId('tooltip')).not.toBeInTheDocument();
    });
  });

  describe('X轴和Y轴', () => {
    it('应该在有xKey时显示X轴', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('x-axis')).toBeInTheDocument();
    });

    it('应该在没有xKey时不显示X轴', () => {
      render(<Chart type="line" data={mockData} yKey="value" />);

      expect(screen.queryByTestId('x-axis')).not.toBeInTheDocument();
    });

    it('应该始终显示Y轴', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('y-axis')).toBeInTheDocument();
    });
  });

  describe('自定义高度', () => {
    it('应该使用默认高度300px', () => {
      const { container } = render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" />
      );

      const responsiveContainer = screen.getByTestId('responsive-container');
      expect(responsiveContainer).toBeInTheDocument();
    });

    it('应该应用自定义高度', () => {
      const customHeight = 500;
      render(
        <Chart type="line" data={mockData} xKey="date" yKey="value" height={customHeight} />
      );

      const responsiveContainer = screen.getByTestId('responsive-container');
      expect(responsiveContainer).toBeInTheDocument();
    });
  });

  describe('自定义颜色', () => {
    it('应该使用自定义颜色', () => {
      const customColors = ['#ff0000', '#00ff00', '#0000ff'];
      render(
        <Chart
          type="line"
          data={mockData}
          xKey="date"
          yKey="value"
          colors={customColors}
        />
      );

      expect(screen.getByTestId('line-value')).toBeInTheDocument();
    });
  });

  describe('饼图特殊处理', () => {
    it('应该为饼图渲染单元格', () => {
      render(<Chart type="pie" data={mockData} xKey="date" yKey="value" />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(mockData.length);
    });
  });

  describe('ResponsiveContainer', () => {
    it('应该包裹所有图表在ResponsiveContainer中', () => {
      render(<Chart type="line" data={mockData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    });
  });

  describe('边界情况', () => {
    it('应该处理单个数据点', () => {
      const singleDataPoint = [{ date: '2024-01', value: 100 }];
      render(<Chart type="line" data={singleDataPoint} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });

    it('应该处理大量数据点', () => {
      const largeData = Array.from({ length: 100 }, (_, i) => ({
        date: `2024-${i + 1}`,
        value: Math.random() * 100,
      }));

      render(<Chart type="line" data={largeData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });

    it('应该处理字符串类型的数值', () => {
      const stringValueData = [
        { date: '2024-01', value: '100' },
        { date: '2024-02', value: '120' },
      ];

      render(
        <Chart type="line" data={stringValueData as any} xKey="date" yKey="value" />
      );

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });

    it('应该处理负数值', () => {
      const negativeData = [
        { date: '2024-01', value: -50 },
        { date: '2024-02', value: 100 },
        { date: '2024-03', value: -30 },
      ];

      render(<Chart type="line" data={negativeData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });

    it('应该处理零值', () => {
      const zeroData = [
        { date: '2024-01', value: 0 },
        { date: '2024-02', value: 0 },
        { date: '2024-03', value: 0 },
      ];

      render(<Chart type="line" data={zeroData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });
  });

  describe('类型安全', () => {
    it('应该接受泛型数据点类型', () => {
      interface CustomDataPoint extends ChartDataPoint {
        customField: string;
      }

      const customData: CustomDataPoint[] = [
        { date: '2024-01', value: 100, customField: 'test' },
        { date: '2024-02', value: 120, customField: 'test2' },
      ];

      render(<Chart type="line" data={customData} xKey="date" yKey="value" />);

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });
  });
});
