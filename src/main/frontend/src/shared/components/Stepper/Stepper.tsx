import React from 'react';
import styles from './Stepper.module.css';

export interface Step {
  id: string;
  label: string;
  description?: string;
  icon?: string;
}

export interface StepperProps {
  steps: Step[];
  currentStep: number;
  onStepClick?: (stepIndex: number) => void;
  allowStepClick?: boolean;
  variant?: 'horizontal' | 'vertical';
}

/**
 * Stepper 组件
 *
 * 多步骤流程指示器
 *
 * 功能:
 * - 显示多步骤流程进度
 * - 支持水平/垂直布局
 * - 可选步骤点击跳转
 * - 状态指示（完成/当前/待处理）
 *
 * @example
 * ```tsx
 * <Stepper
 *   steps={[
 *     { id: '1', label: '基本信息', description: '填写服务器基本信息' },
 *     { id: '2', label: '连接测试', description: '测试SSH连接' },
 *     { id: '3', label: '完成', description: '确认并创建' },
 *   ]}
 *   currentStep={0}
 *   onStepClick={(index) => setCurrentStep(index)}
 *   allowStepClick={true}
 * />
 * ```
 */
export const Stepper: React.FC<StepperProps> = ({
  steps,
  currentStep,
  onStepClick,
  allowStepClick = false,
  variant = 'horizontal',
}) => {
  const handleStepClick = (index: number): void => {
    if (allowStepClick && onStepClick && index < currentStep) {
      onStepClick(index);
    }
  };

  const getStepStatus = (index: number): 'completed' | 'current' | 'pending' => {
    if (index < currentStep) return 'completed';
    if (index === currentStep) return 'current';
    return 'pending';
  };

  return (
    <div className={`${styles.stepper} ${styles[variant]}`} role="navigation" aria-label="进度步骤">
      {steps.map((step, index) => {
        const status = getStepStatus(index);
        const isClickable = allowStepClick && index < currentStep;
        const isLast = index === steps.length - 1;

        return (
          <React.Fragment key={step.id}>
            <div
              className={`${styles.step} ${styles[status]} ${isClickable ? styles.clickable : ''}`}
              onClick={() => handleStepClick(index)}
              onKeyDown={(e) => {
                if ((e.key === 'Enter' || e.key === ' ') && isClickable) {
                  e.preventDefault();
                  handleStepClick(index);
                }
              }}
              role="button"
              tabIndex={isClickable ? 0 : -1}
              aria-current={status === 'current' ? 'step' : undefined}
              aria-disabled={!isClickable}
            >
              {/* Step Number/Icon */}
              <div className={styles.stepIndicator}>
                {status === 'completed' ? (
                  <div className={styles.stepIcon}>
                    <i data-lucide="check" />
                  </div>
                ) : (
                  <div className={styles.stepNumber}>{index + 1}</div>
                )}
              </div>

              {/* Step Content */}
              <div className={styles.stepContent}>
                <div className={styles.stepLabel}>{step.label}</div>
                {step.description && (
                  <div className={styles.stepDescription}>{step.description}</div>
                )}
              </div>
            </div>

            {/* Connector Line */}
            {!isLast && (
              <div
                className={`${styles.connector} ${index < currentStep ? styles.completed : ''}`}
                aria-hidden="true"
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};
