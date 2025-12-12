import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Stepper, type Step } from '../../../src/shared/components/Stepper/Stepper';
import '@testing-library/jest-dom';

describe('Stepper', () => {
  const mockSteps: Step[] = [
    { id: '1', label: '基本信息', description: '填写服务器基本信息' },
    { id: '2', label: '连接测试', description: '测试SSH连接' },
    { id: '3', label: '完成', description: '确认并创建' },
  ];

  it('应该正确渲染步骤', () => {
    render(<Stepper steps={mockSteps} currentStep={0} />);

    expect(screen.getByText('基本信息')).toBeInTheDocument();
    expect(screen.getByText('连接测试')).toBeInTheDocument();
    expect(screen.getByText('完成')).toBeInTheDocument();
  });

  it('应该显示步骤描述', () => {
    render(<Stepper steps={mockSteps} currentStep={0} />);

    expect(screen.getByText('填写服务器基本信息')).toBeInTheDocument();
    expect(screen.getByText('测试SSH连接')).toBeInTheDocument();
    expect(screen.getByText('确认并创建')).toBeInTheDocument();
  });

  it('应该正确显示当前步骤', () => {
    render(<Stepper steps={mockSteps} currentStep={1} />);

    const steps = screen.getAllByRole('button');
    expect(steps[1]).toHaveAttribute('aria-current', 'step');
  });

  it('应该为已完成的步骤显示勾选图标', () => {
    const { container } = render(<Stepper steps={mockSteps} currentStep={2} />);

    const checkIcons = container.querySelectorAll('[data-lucide="check"]');
    expect(checkIcons.length).toBeGreaterThanOrEqual(2); // Steps 0 and 1 are completed
  });

  it('应该在当前步骤显示步骤编号', () => {
    render(<Stepper steps={mockSteps} currentStep={1} />);

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('应该在待处理步骤显示步骤编号', () => {
    render(<Stepper steps={mockSteps} currentStep={0} />);

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('应该支持点击跳转到之前的步骤', () => {
    const handleStepClick = vi.fn();
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={handleStepClick}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    fireEvent.click(steps[0]); // Click on first step

    expect(handleStepClick).toHaveBeenCalledWith(0);
  });

  it('应该禁止点击跳转到未来的步骤', () => {
    const handleStepClick = vi.fn();
    render(
      <Stepper
        steps={mockSteps}
        currentStep={0}
        onStepClick={handleStepClick}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    fireEvent.click(steps[2]); // Try to click on future step

    expect(handleStepClick).not.toHaveBeenCalled();
  });

  it('应该支持键盘导航（Enter键）', () => {
    const handleStepClick = vi.fn();
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={handleStepClick}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    fireEvent.keyDown(steps[0], { key: 'Enter' });

    expect(handleStepClick).toHaveBeenCalledWith(0);
  });

  it('应该支持键盘导航（Space键）', () => {
    const handleStepClick = vi.fn();
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={handleStepClick}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    fireEvent.keyDown(steps[0], { key: ' ' });

    expect(handleStepClick).toHaveBeenCalledWith(0);
  });

  it('应该在禁用点击时不触发回调', () => {
    const handleStepClick = vi.fn();
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={handleStepClick}
        allowStepClick={false}
      />
    );

    const steps = screen.getAllByRole('button');
    fireEvent.click(steps[0]);

    expect(handleStepClick).not.toHaveBeenCalled();
  });

  it('应该为可点击的步骤设置正确的tabIndex', () => {
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={vi.fn()}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    expect(steps[0]).toHaveAttribute('tabIndex', '0'); // Completed step - clickable
    expect(steps[1]).toHaveAttribute('tabIndex', '0'); // Completed step - clickable
    expect(steps[2]).toHaveAttribute('tabIndex', '-1'); // Current step - not clickable
  });

  it('应该为不可点击的步骤设置aria-disabled', () => {
    render(
      <Stepper
        steps={mockSteps}
        currentStep={2}
        onStepClick={vi.fn()}
        allowStepClick={true}
      />
    );

    const steps = screen.getAllByRole('button');
    expect(steps[0]).toHaveAttribute('aria-disabled', 'false');
    expect(steps[2]).toHaveAttribute('aria-disabled', 'true');
  });

  it('应该支持水平布局', () => {
    const { container } = render(
      <Stepper steps={mockSteps} currentStep={0} variant="horizontal" />
    );

    const stepper = container.querySelector('div[role="navigation"]');
    expect(stepper).toHaveClass('horizontal');
  });

  it('应该支持垂直布局', () => {
    const { container } = render(
      <Stepper steps={mockSteps} currentStep={0} variant="vertical" />
    );

    const stepper = container.querySelector('div[role="navigation"]');
    expect(stepper).toHaveClass('vertical');
  });

  it('应该默认使用水平布局', () => {
    const { container } = render(<Stepper steps={mockSteps} currentStep={0} />);

    const stepper = container.querySelector('div[role="navigation"]');
    expect(stepper).toHaveClass('horizontal');
  });

  it('应该正确处理单个步骤', () => {
    const singleStep: Step[] = [{ id: '1', label: '唯一步骤' }];
    render(<Stepper steps={singleStep} currentStep={0} />);

    expect(screen.getByText('唯一步骤')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('应该正确处理没有描述的步骤', () => {
    const stepsWithoutDesc: Step[] = [
      { id: '1', label: '步骤1' },
      { id: '2', label: '步骤2' },
    ];
    render(<Stepper steps={stepsWithoutDesc} currentStep={0} />);

    expect(screen.getByText('步骤1')).toBeInTheDocument();
    expect(screen.getByText('步骤2')).toBeInTheDocument();
  });

  it('应该渲染连接线', () => {
    const { container } = render(<Stepper steps={mockSteps} currentStep={0} />);

    const connectors = container.querySelectorAll('[aria-hidden="true"]');
    // Should have 2 connectors for 3 steps (between each pair)
    expect(connectors.length).toBeGreaterThanOrEqual(2);
  });

  it('应该为已完成步骤之间的连接线添加completed样式', () => {
    const { container } = render(<Stepper steps={mockSteps} currentStep={2} />);

    const connectors = container.querySelectorAll('[aria-hidden="true"]');
    expect(connectors[0]).toHaveClass('completed');
    expect(connectors[1]).toHaveClass('completed');
  });

  it('应该设置正确的导航标签', () => {
    render(<Stepper steps={mockSteps} currentStep={0} />);

    const navigation = screen.getByRole('navigation');
    expect(navigation).toHaveAttribute('aria-label', '进度步骤');
  });
});
