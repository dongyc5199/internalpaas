/**
 * Table Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Table, TableColumn } from '../../../react-app/components/Table';
import styles from '../../../react-app/components/Table/Table.module.css';

interface TestData {
  id: string;
  name: string;
  email: string;
  age: number;
}

const testData: TestData[] = [
  { id: '1', name: 'John Doe', email: 'john@example.com', age: 30 },
  { id: '2', name: 'Jane Smith', email: 'jane@example.com', age: 25 },
  { id: '3', name: 'Bob Johnson', email: 'bob@example.com', age: 35 },
];

const testColumns: TableColumn<TestData>[] = [
  { key: 'name', label: 'Name', dataKey: 'name' },
  { key: 'email', label: 'Email', dataKey: 'email' },
  { key: 'age', label: 'Age', dataKey: 'age' },
];

describe('Table', () => {
  describe('Rendering', () => {
    it('应该渲染基本表格', () => {
      render(<Table columns={testColumns} data={testData} />);
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('应该渲染所有列标题', () => {
      render(<Table columns={testColumns} data={testData} />);
      expect(screen.getByText('Name')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Age')).toBeInTheDocument();
    });

    it('应该渲染所有数据行', () => {
      render(<Table columns={testColumns} data={testData} />);
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
      expect(screen.getByText('Bob Johnson')).toBeInTheDocument();
    });

    it('应该渲染默认size为md', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/size-md/);
    });

    it('应该渲染默认variant为default', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/variant-default/);
    });
  });

  describe('Caption', () => {
    it('应该渲染caption', () => {
      render(<Table columns={testColumns} data={testData} caption="User Table" />);
      expect(screen.getByText('User Table')).toBeInTheDocument();
    });

    it('应该不渲染caption当未提供', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      expect(container.querySelector('caption')).not.toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it('应该渲染small size', () => {
      const { container } = render(<Table columns={testColumns} data={testData} size="sm" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/size-sm/);
    });

    it('应该渲染medium size', () => {
      const { container } = render(<Table columns={testColumns} data={testData} size="md" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/size-md/);
    });

    it('应该渲染large size', () => {
      const { container } = render(<Table columns={testColumns} data={testData} size="lg" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/size-lg/);
    });
  });

  describe('Variants', () => {
    it('应该渲染default variant', () => {
      const { container } = render(<Table columns={testColumns} data={testData} variant="default" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/variant-default/);
    });

    it('应该渲染striped variant', () => {
      const { container } = render(<Table columns={testColumns} data={testData} variant="striped" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/variant-striped/);
    });

    it('应该渲染bordered variant', () => {
      const { container } = render(<Table columns={testColumns} data={testData} variant="bordered" />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/variant-bordered/);
    });
  });

  describe('Hover', () => {
    it('应该默认启用hover效果', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/hover/);
    });

    it('应该禁用hover效果当hover为false', () => {
      const { container } = render(<Table columns={testColumns} data={testData} hover={false} />);
      const table = container.querySelector('table');
      expect(table?.className).not.toMatch(/hover/);
    });
  });

  describe('Column Configuration', () => {
    it('应该使用dataKey渲染单元格', () => {
      render(<Table columns={testColumns} data={testData} />);
      expect(screen.getByText('john@example.com')).toBeInTheDocument();
    });

    it('应该使用render函数渲染单元格', () => {
      const columns: TableColumn<TestData>[] = [
        {
          key: 'name',
          label: 'Name',
          render: (row) => <strong>{row.name}</strong>,
        },
      ];
      render(<Table columns={columns} data={testData} />);
      const cell = screen.getByText('John Doe');
      expect(cell.tagName).toBe('STRONG');
    });

    it('应该设置列宽', () => {
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', width: '200px' },
      ];
      const { container } = render(<Table columns={columns} data={testData} />);
      const th = container.querySelector('th') as HTMLElement;
      expect(th.style.width).toBe('200px');
    });

    it('应该设置列对齐', () => {
      const columns: TableColumn<TestData>[] = [
        { key: 'age', label: 'Age', dataKey: 'age', align: 'right' },
      ];
      const { container } = render(<Table columns={columns} data={testData} />);
      const th = container.querySelector('th') as HTMLElement;
      expect(th.className).toMatch(/align-right/);
    });
  });

  describe('Sorting', () => {
    it('应该渲染sortable列', () => {
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', sortable: true },
      ];
      const { container } = render(<Table columns={columns} data={testData} />);
      const th = container.querySelector('th') as HTMLElement;
      expect(th.className).toMatch(/sortable/);
    });

    it('应该在点击sortable列时调用onSortChange', async () => {
      const user = userEvent.setup();
      const handleSortChange = vi.fn();
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', sortable: true },
      ];
      render(
        <Table
          columns={columns}
          data={testData}
          onSortChange={handleSortChange}
        />
      );

      await user.click(screen.getByText('Name'));
      expect(handleSortChange).toHaveBeenCalledWith('name', 'asc');
    });

    it('应该切换排序方向', async () => {
      const user = userEvent.setup();
      const handleSortChange = vi.fn();
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', sortable: true },
      ];
      render(
        <Table
          columns={columns}
          data={testData}
          sortConfig={{ key: 'name', direction: 'asc' }}
          onSortChange={handleSortChange}
        />
      );

      await user.click(screen.getByText('Name'));
      expect(handleSortChange).toHaveBeenCalledWith('name', 'desc');
    });
  });

  describe('Selection', () => {
    it('应该渲染选择列', () => {
      render(<Table columns={testColumns} data={testData} selectable />);
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes.length).toBe(testData.length + 1); // +1 for select all
    });

    it('应该在点击行checkbox时调用onSelectionChange', async () => {
      const user = userEvent.setup();
      const handleSelectionChange = vi.fn();
      render(
        <Table
          columns={testColumns}
          data={testData}
          selectable
          rowKey={(row) => row.id}
          onSelectionChange={handleSelectionChange}
        />
      );

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // First data row
      expect(handleSelectionChange).toHaveBeenCalledWith(['1']);
    });

    it('应该支持全选', async () => {
      const user = userEvent.setup();
      const handleSelectionChange = vi.fn();
      render(
        <Table
          columns={testColumns}
          data={testData}
          selectable
          rowKey={(row) => row.id}
          onSelectionChange={handleSelectionChange}
        />
      );

      const selectAllCheckbox = screen.getByLabelText('Select all rows');
      await user.click(selectAllCheckbox);
      expect(handleSelectionChange).toHaveBeenCalledWith(['1', '2', '3']);
    });

    it('应该高亮选中的行', () => {
      const { container } = render(
        <Table
          columns={testColumns}
          data={testData}
          selectable
          selectedKeys={['1']}
          rowKey={(row) => row.id}
        />
      );
      const tbody = container.querySelector('tbody');
      const rows = tbody?.querySelectorAll('tr');
      expect(rows?.[0].className).toMatch(/selected/);
    });
  });

  describe('Row Click', () => {
    it('应该在点击行时调用onRowClick', async () => {
      const user = userEvent.setup();
      const handleRowClick = vi.fn();
      render(
        <Table
          columns={testColumns}
          data={testData}
          onRowClick={handleRowClick}
        />
      );

      await user.click(screen.getByText('John Doe'));
      expect(handleRowClick).toHaveBeenCalledWith(testData[0], 0);
    });

    it('应该为可点击行添加clickable样式', () => {
      const { container } = render(
        <Table
          columns={testColumns}
          data={testData}
          onRowClick={() => {}}
        />
      );
      const tbody = container.querySelector('tbody');
      const row = tbody?.querySelector('tr') as HTMLElement;
      expect(row.className).toMatch(/clickable/);
    });
  });

  describe('Empty State', () => {
    it('应该显示空状态消息', () => {
      render(<Table columns={testColumns} data={[]} />);
      expect(screen.getByText('No data available')).toBeInTheDocument();
    });

    it('应该显示自定义空状态消息', () => {
      render(
        <Table
          columns={testColumns}
          data={[]}
          emptyMessage="No users found"
        />
      );
      expect(screen.getByText('No users found')).toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('应该显示loading状态', () => {
      render(<Table columns={testColumns} data={testData} loading />);
      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('应该在loading时不显示数据', () => {
      render(<Table columns={testColumns} data={testData} loading />);
      expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
    });
  });

  describe('Full Width', () => {
    it('应该默认使用fullWidth', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const containerElem = container.querySelector(`.${styles.container}`);
      expect(containerElem?.className).toMatch(/fullWidth/);
    });

    it('应该不使用fullWidth当prop为false', () => {
      const { container } = render(<Table columns={testColumns} data={testData} fullWidth={false} />);
      const containerElem = container.querySelector(`.${styles.container}`);
      expect(containerElem?.className).not.toMatch(/fullWidth/);
    });
  });

  describe('Sticky Header', () => {
    it('应该支持sticky header', () => {
      const { container } = render(<Table columns={testColumns} data={testData} stickyHeader />);
      const table = container.querySelector('table');
      expect(table?.className).toMatch(/stickyHeader/);
    });

    it('应该默认不使用sticky header', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const table = container.querySelector('table');
      expect(table?.className).not.toMatch(/stickyHeader/);
    });
  });

  describe('Custom Props', () => {
    it('应该传递自定义className到container', () => {
      const { container } = render(
        <Table columns={testColumns} data={testData} className="custom-table" />
      );
      const containerElem = container.querySelector(`.${styles.container}`);
      expect(containerElem?.className).toContain('custom-table');
    });

    it('应该传递自定义tableClassName', () => {
      const { container } = render(
        <Table columns={testColumns} data={testData} tableClassName="custom-table-element" />
      );
      const table = container.querySelector('table');
      expect(table?.className).toContain('custom-table-element');
    });

    it('应该传递自定义headerClassName', () => {
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', headerClassName: 'custom-header' },
      ];
      const { container } = render(<Table columns={columns} data={testData} />);
      const th = container.querySelector('th') as HTMLElement;
      expect(th.className).toContain('custom-header');
    });

    it('应该传递自定义cellClassName', () => {
      const columns: TableColumn<TestData>[] = [
        { key: 'name', label: 'Name', dataKey: 'name', cellClassName: 'custom-cell' },
      ];
      const { container } = render(<Table columns={columns} data={testData} />);
      const tbody = container.querySelector('tbody');
      const td = tbody?.querySelector('td') as HTMLElement;
      expect(td.className).toContain('custom-cell');
    });
  });

  describe('Row Key', () => {
    it('应该使用自定义rowKey渲染正确数量的行', () => {
      const { container } = render(
        <Table
          columns={testColumns}
          data={testData}
          rowKey={(row) => row.id}
        />
      );
      const tbody = container.querySelector('tbody');
      const rows = tbody?.querySelectorAll('tr');
      expect(rows?.length).toBe(testData.length);
    });

    it('应该使用默认rowKey (index)', () => {
      const { container } = render(<Table columns={testColumns} data={testData} />);
      const tbody = container.querySelector('tbody');
      const rows = tbody?.querySelectorAll('tr');
      expect(rows?.length).toBe(testData.length);
    });
  });

  describe('Accessibility', () => {
    it('应该有role为table', () => {
      render(<Table columns={testColumns} data={testData} />);
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('应该为checkbox设置aria-label', () => {
      render(<Table columns={testColumns} data={testData} selectable />);
      expect(screen.getByLabelText('Select all rows')).toBeInTheDocument();
      expect(screen.getByLabelText('Select row 1')).toBeInTheDocument();
    });

    it('应该渲染caption以提供上下文', () => {
      render(<Table columns={testColumns} data={testData} caption="User Data" />);
      const caption = screen.getByText('User Data');
      expect(caption.tagName).toBe('CAPTION');
    });
  });

  describe('Complex Scenarios', () => {
    it('应该处理空列数组', () => {
      render(<Table columns={[]} data={testData} />);
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('应该处理空数据和空列', () => {
      render(<Table columns={[]} data={[]} />);
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('应该正确渲染带自定义渲染的复杂表格', () => {
      const complexColumns: TableColumn<TestData>[] = [
        {
          key: 'name',
          label: 'Name',
          render: (row) => <a href={`/user/${row.id}`}>{row.name}</a>,
        },
        {
          key: 'age',
          label: 'Age',
          dataKey: 'age',
          align: 'right',
        },
      ];
      render(<Table columns={complexColumns} data={testData} />);
      expect(screen.getByText('John Doe').tagName).toBe('A');
    });
  });
});
