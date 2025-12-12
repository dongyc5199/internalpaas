import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Stepper, type Step } from '../../../shared/components/Stepper';
import { Button } from '../../../shared/components/Button';
import { Input } from '../../../shared/components/Input';
import { serverApi } from '../../../shared/api/serverApi';
import styles from './SSHImportWizard.module.css';

interface SSHImportWizardProps {
  onSuccess: () => void;
  onCancel: () => void;
}

type ImportMethod = 'manual' | 'config-file' | 'bulk';

interface SSHConfig {
  name: string;
  host: string;
  port: number;
  username: string;
  password: string;
  tags: string[];
}

const steps: Step[] = [
  {
    id: 'method',
    label: '选择导入方式',
    description: '选择如何导入SSH配置',
  },
  {
    id: 'config',
    label: '配置信息',
    description: '填写服务器连接信息',
  },
  {
    id: 'test',
    label: '连接测试',
    description: '测试SSH连接',
  },
  {
    id: 'confirm',
    label: '确认导入',
    description: '确认并保存配置',
  },
];

/**
 * SSHImportWizard 组件
 *
 * SSH配置导入向导
 *
 * 功能:
 * - 多步骤导入流程
 * - 支持手动输入/配置文件/批量导入
 * - SSH连接测试
 * - 配置验证
 *
 * @example
 * ```tsx
 * <SSHImportWizard
 *   onSuccess={() => {
 *     console.log('Import successful');
 *     navigate('/admin/servers');
 *   }}
 *   onCancel={() => navigate('/admin/servers')}
 * />
 * ```
 */
export function SSHImportWizard({ onSuccess, onCancel }: SSHImportWizardProps): React.JSX.Element {
  const [currentStep, setCurrentStep] = useState(0);
  const [importMethod, setImportMethod] = useState<ImportMethod>('manual');
  const [configFile, setConfigFile] = useState<string>('');
  const [sshConfigs, setSSHConfigs] = useState<SSHConfig[]>([
    {
      name: '',
      host: '',
      port: 22,
      username: '',
      password: '',
      tags: [],
    },
  ]);
  const [testResults, setTestResults] = useState<Record<number, 'success' | 'error' | 'pending'>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Create server mutation
  const createMutation = useMutation({
    mutationFn: async (configs: SSHConfig[]) => {
      const promises = configs.map((config) =>
        serverApi.createServer({
          name: config.name,
          host: config.host,
          port: config.port,
          username: config.username,
          password: config.password,
          tags: config.tags,
        })
      );
      return Promise.all(promises);
    },
    onSuccess: () => {
      onSuccess();
    },
    onError: (error) => {
      setErrors({ submit: error instanceof Error ? error.message : '导入失败' });
    },
  });

  // Test connection mutation
  const testConnectionMutation = useMutation({
    mutationFn: async (config: SSHConfig) => {
      const response = await fetch('/api/admin/servers/test-connection', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config),
      });
      if (!response.ok) throw new Error('连接测试失败');
      return response.json();
    },
  });

  /**
   * Parse SSH config file
   */
  const parseSSHConfigFile = (content: string): SSHConfig[] => {
    const configs: SSHConfig[] = [];
    const lines = content.split('\n');
    let currentConfig: Partial<SSHConfig> | null = null;

    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('Host ') && !trimmed.includes('*')) {
        if (currentConfig && currentConfig.host) {
          configs.push({
            name: currentConfig.name || currentConfig.host,
            host: currentConfig.host,
            port: currentConfig.port || 22,
            username: currentConfig.username || 'root',
            password: '',
            tags: currentConfig.tags || [],
          });
        }
        currentConfig = {
          name: trimmed.substring(5).trim(),
          port: 22,
          tags: [],
        };
      } else if (currentConfig) {
        if (trimmed.startsWith('HostName ')) {
          currentConfig.host = trimmed.substring(9).trim();
        } else if (trimmed.startsWith('Port ')) {
          currentConfig.port = parseInt(trimmed.substring(5).trim(), 10);
        } else if (trimmed.startsWith('User ')) {
          currentConfig.username = trimmed.substring(5).trim();
        }
      }
    }

    // Add last config
    if (currentConfig && currentConfig.host) {
      configs.push({
        name: currentConfig.name || currentConfig.host,
        host: currentConfig.host,
        port: currentConfig.port || 22,
        username: currentConfig.username || 'root',
        password: '',
        tags: currentConfig.tags || [],
      });
    }

    return configs;
  };

  /**
   * Handle file upload
   */
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>): void => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      setConfigFile(content);

      try {
        const parsedConfigs = parseSSHConfigFile(content);
        if (parsedConfigs.length > 0) {
          setSSHConfigs(parsedConfigs);
          setErrors({});
        } else {
          setErrors({ file: '无法解析配置文件，请检查格式' });
        }
      } catch (error) {
        setErrors({ file: '配置文件解析失败' });
      }
    };
    reader.readAsText(file);
  };

  /**
   * Update config at index
   */
  const updateConfig = (index: number, field: keyof SSHConfig, value: string | number | string[]): void => {
    setSSHConfigs((prev) =>
      prev.map((config, i) =>
        i === index
          ? {
              ...config,
              [field]: value,
            }
          : config
      )
    );
  };

  /**
   * Add new config
   */
  const addConfig = (): void => {
    setSSHConfigs((prev) => [
      ...prev,
      {
        name: '',
        host: '',
        port: 22,
        username: '',
        password: '',
        tags: [],
      },
    ]);
  };

  /**
   * Remove config
   */
  const removeConfig = (index: number): void => {
    setSSHConfigs((prev) => prev.filter((_, i) => i !== index));
    setTestResults((prev) => {
      const next = { ...prev };
      delete next[index];
      return next;
    });
  };

  /**
   * Test connection for config
   */
  const testConnection = async (index: number): Promise<void> => {
    const config = sshConfigs[index];
    if (!config) {
      setTestResults((prev) => ({ ...prev, [index]: 'error' }));
      return;
    }
    setTestResults((prev) => ({ ...prev, [index]: 'pending' }));

    try {
      await testConnectionMutation.mutateAsync(config);
      setTestResults((prev) => ({ ...prev, [index]: 'success' }));
    } catch (error) {
      setTestResults((prev) => ({ ...prev, [index]: 'error' }));
    }
  };

  /**
   * Test all connections
   */
  const testAllConnections = async (): Promise<void> => {
    for (let i = 0; i < sshConfigs.length; i++) {
      await testConnection(i);
    }
  };

  /**
   * Validate current step
   */
  const validateStep = (): boolean => {
    const newErrors: Record<string, string> = {};

    switch (currentStep) {
      case 0: // Method selection
        if (!importMethod) {
          newErrors.method = '请选择导入方式';
        }
        break;

      case 1: // Configuration
        sshConfigs.forEach((config, index) => {
          if (!config.name.trim()) {
            newErrors[`name-${index}`] = '请输入服务器名称';
          }
          if (!config.host.trim()) {
            newErrors[`host-${index}`] = '请输入主机地址';
          }
          if (!config.username.trim()) {
            newErrors[`username-${index}`] = '请输入用户名';
          }
          if (!config.password) {
            newErrors[`password-${index}`] = '请输入密码';
          }
        });
        break;

      case 2: // Connection test
        const allTested = sshConfigs.every((_, index) => testResults[index]);
        if (!allTested) {
          newErrors.test = '请测试所有服务器连接';
        }
        const hasErrors = Object.values(testResults).some((result) => result === 'error');
        if (hasErrors) {
          newErrors.test = '部分服务器连接失败，请检查配置';
        }
        break;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handle next step
   */
  const handleNext = async (): Promise<void> => {
    if (!validateStep()) return;

    if (currentStep === 2) {
      // Test connections before moving to confirm
      await testAllConnections();
    }

    if (currentStep < steps.length - 1) {
      setCurrentStep((prev) => prev + 1);
    }
  };

  /**
   * Handle previous step
   */
  const handlePrevious = (): void => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  /**
   * Handle submit
   */
  const handleSubmit = (): void => {
    createMutation.mutate(sshConfigs);
  };

  /**
   * Render step content
   */
  const renderStepContent = (): React.JSX.Element => {
    switch (currentStep) {
      case 0:
        return (
          <div className={styles.stepContent}>
            <h3 className={styles.stepTitle}>选择导入方式</h3>
            <p className={styles.stepDescription}>请选择如何导入SSH服务器配置</p>

            <div className={styles.methodCards}>
              <button
                type="button"
                className={`${styles.methodCard} ${importMethod === 'manual' ? styles.selected : ''}`}
                onClick={() => setImportMethod('manual')}
              >
                <i data-lucide="keyboard" className={styles.methodIcon} />
                <h4>手动输入</h4>
                <p>逐个填写服务器信息</p>
              </button>

              <button
                type="button"
                className={`${styles.methodCard} ${importMethod === 'config-file' ? styles.selected : ''}`}
                onClick={() => setImportMethod('config-file')}
              >
                <i data-lucide="file-text" className={styles.methodIcon} />
                <h4>配置文件导入</h4>
                <p>从SSH config文件导入</p>
              </button>

              <button
                type="button"
                className={`${styles.methodCard} ${importMethod === 'bulk' ? styles.selected : ''}`}
                onClick={() => setImportMethod('bulk')}
              >
                <i data-lucide="layers" className={styles.methodIcon} />
                <h4>批量导入</h4>
                <p>导入多个服务器配置</p>
              </button>
            </div>

            {importMethod === 'config-file' && (
              <div className={styles.fileUpload}>
                <label htmlFor="config-file" className={styles.fileLabel}>
                  <i data-lucide="upload" />
                  <span>选择SSH配置文件</span>
                  <input
                    id="config-file"
                    type="file"
                    accept=".config,.conf,.txt"
                    onChange={handleFileUpload}
                    className={styles.fileInput}
                  />
                </label>
                {configFile && (
                  <div className={styles.filePreview}>
                    <i data-lucide="file-check" />
                    <span>已解析 {sshConfigs.length} 个服务器配置</span>
                  </div>
                )}
                {errors.file && <p className={styles.error}>{errors.file}</p>}
              </div>
            )}
          </div>
        );

      case 1:
        return (
          <div className={styles.stepContent}>
            <h3 className={styles.stepTitle}>配置信息</h3>
            <p className={styles.stepDescription}>填写服务器SSH连接信息</p>

            <div className={styles.configList}>
              {sshConfigs.map((config, index) => (
                <div key={index} className={styles.configCard}>
                  <div className={styles.configHeader}>
                    <h4>服务器 #{index + 1}</h4>
                    {sshConfigs.length > 1 && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => removeConfig(index)}
                        aria-label="删除配置"
                      >
                        <i data-lucide="trash-2" />
                      </Button>
                    )}
                  </div>

                  <div className={styles.configForm}>
                    <div className={styles.formRow}>
                      <div className={styles.formGroup}>
                        <label>服务器名称 *</label>
                        <Input
                          value={config.name}
                          onChange={(e) => updateConfig(index, 'name', e.target.value)}
                          placeholder="例如: Production Server 1"
                          error={errors[`name-${index}`]}
                        />
                      </div>
                      <div className={styles.formGroup}>
                        <label>主机地址 *</label>
                        <Input
                          value={config.host}
                          onChange={(e) => updateConfig(index, 'host', e.target.value)}
                          placeholder="192.168.1.100"
                          error={errors[`host-${index}`]}
                        />
                      </div>
                    </div>

                    <div className={styles.formRow}>
                      <div className={styles.formGroup}>
                        <label>端口 *</label>
                        <Input
                          type="number"
                          value={String(config.port)}
                          onChange={(e) => updateConfig(index, 'port', parseInt(e.target.value, 10))}
                          placeholder="22"
                        />
                      </div>
                      <div className={styles.formGroup}>
                        <label>用户名 *</label>
                        <Input
                          value={config.username}
                          onChange={(e) => updateConfig(index, 'username', e.target.value)}
                          placeholder="root"
                          error={errors[`username-${index}`]}
                        />
                      </div>
                    </div>

                    <div className={styles.formGroup}>
                      <label>密码 *</label>
                      <Input
                        type="password"
                        value={config.password}
                        onChange={(e) => updateConfig(index, 'password', e.target.value)}
                        placeholder="请输入SSH密码"
                        error={errors[`password-${index}`]}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {importMethod === 'bulk' && (
              <Button type="button" variant="secondary" onClick={addConfig} className={styles.addButton}>
                <i data-lucide="plus" />
                添加服务器
              </Button>
            )}
          </div>
        );

      case 2:
        return (
          <div className={styles.stepContent}>
            <h3 className={styles.stepTitle}>连接测试</h3>
            <p className={styles.stepDescription}>测试SSH连接以确保配置正确</p>

            <div className={styles.testList}>
              {sshConfigs.map((config, index) => (
                <div key={index} className={styles.testCard}>
                  <div className={styles.testInfo}>
                    <h4>{config.name}</h4>
                    <p>
                      {config.username}@{config.host}:{config.port}
                    </p>
                  </div>

                  <div className={styles.testActions}>
                    {testResults[index] === 'success' && (
                      <span className={styles.testSuccess}>
                        <i data-lucide="check-circle" />
                        连接成功
                      </span>
                    )}
                    {testResults[index] === 'error' && (
                      <span className={styles.testError}>
                        <i data-lucide="x-circle" />
                        连接失败
                      </span>
                    )}
                    {testResults[index] === 'pending' && (
                      <span className={styles.testPending}>
                        <i data-lucide="loader" className={styles.spinning} />
                        测试中...
                      </span>
                    )}
                    {!testResults[index] && (
                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        onClick={() => testConnection(index)}
                      >
                        测试连接
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <Button
              type="button"
              variant="primary"
              onClick={testAllConnections}
              disabled={testConnectionMutation.isPending}
              className={styles.testAllButton}
            >
              测试所有连接
            </Button>

            {errors.test && <p className={styles.error}>{errors.test}</p>}
          </div>
        );

      case 3:
        return (
          <div className={styles.stepContent}>
            <h3 className={styles.stepTitle}>确认导入</h3>
            <p className={styles.stepDescription}>确认以下配置信息并导入</p>

            <div className={styles.confirmList}>
              {sshConfigs.map((config, index) => (
                <div key={index} className={styles.confirmCard}>
                  <div className={styles.confirmIcon}>
                    {testResults[index] === 'success' ? (
                      <i data-lucide="check-circle" className={styles.successIcon} />
                    ) : (
                      <i data-lucide="server" className={styles.serverIcon} />
                    )}
                  </div>
                  <div className={styles.confirmInfo}>
                    <h4>{config.name}</h4>
                    <p>
                      {config.username}@{config.host}:{config.port}
                    </p>
                    {config.tags.length > 0 && (
                      <div className={styles.tags}>
                        {config.tags.map((tag) => (
                          <span key={tag} className={styles.tag}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className={styles.confirmSummary}>
              <p>
                <strong>总计:</strong> {sshConfigs.length} 个服务器
              </p>
              <p>
                <strong>测试通过:</strong>{' '}
                {Object.values(testResults).filter((r) => r === 'success').length} 个
              </p>
            </div>

            {errors.submit && <p className={styles.error}>{errors.submit}</p>}
          </div>
        );

      default:
        return <div>Unknown step</div>;
    }
  };

  return (
    <div className={styles.wizard}>
      {/* Stepper */}
      <div className={styles.stepperContainer}>
        <Stepper
          steps={steps}
          currentStep={currentStep}
          onStepClick={setCurrentStep}
          allowStepClick={true}
        />
      </div>

      {/* Step Content */}
      <div className={styles.content}>{renderStepContent()}</div>

      {/* Actions */}
      <div className={styles.actions}>
        <div className={styles.actionsLeft}>
          <Button type="button" variant="ghost" onClick={onCancel}>
            取消
          </Button>
        </div>
        <div className={styles.actionsRight}>
          {currentStep > 0 && (
            <Button type="button" variant="secondary" onClick={handlePrevious}>
              上一步
            </Button>
          )}
          {currentStep < steps.length - 1 ? (
            <Button type="button" variant="primary" onClick={handleNext}>
              下一步
            </Button>
          ) : (
            <Button
              type="button"
              variant="primary"
              onClick={handleSubmit}
              loading={createMutation.isPending}
              disabled={createMutation.isPending}
            >
              确认导入
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
