import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CodeEditor } from '../../../shared/components/CodeEditor/CodeEditor';
import { Button } from '../../../shared/components/Button/Button';
import { Modal } from '../../../shared/components/Modal/Modal';
import { Toast } from '../../../shared/components/Toast/Toast';
import type { editor } from 'monaco-editor';
import styles from './ConfigEditorPage.module.css';

interface ApplicationConfig {
  id: string;
  applicationId: string;
  configType: 'jvm' | 'env' | 'spring' | 'yaml' | 'properties';
  content: string;
  description: string;
  version: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ConfigTemplate {
  id: string;
  name: string;
  description: string;
  content: string;
  type: string;
}

export const ConfigEditorPage: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // 状态管理
  const [activeTab, setActiveTab] = useState<'jvm' | 'env' | 'spring' | 'debug' | 'jmx' | 'advanced'>('jvm');
  const [configContent, setConfigContent] = useState<Record<string, string>>({
    jvm: '',
    env: '{}',
    spring: '',
    debug: '',
    jmx: '',
    advanced: '',
  });
  const [validationErrors, setValidationErrors] = useState<editor.IMarker[]>([]);
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [showSaveTemplateModal, setShowSaveTemplateModal] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // 获取当前配置
  const { data: currentConfig, isLoading } = useQuery<ApplicationConfig>({
    queryKey: ['applicationConfig', applicationId],
    queryFn: async () => {
      const response = await fetch(`/api/applications/${applicationId}/config/active`);
      if (!response.ok) throw new Error('获取配置失败');
      return response.json();
    },
    enabled: !!applicationId,
  });

  // 获取配置模板
  const { data: templates } = useQuery<ConfigTemplate[]>({
    queryKey: ['configTemplates'],
    queryFn: async () => {
      const response = await fetch(`/api/applications/${applicationId}/config/templates`);
      if (!response.ok) throw new Error('获取模板失败');
      return response.json();
    },
  });

  // 获取配置历史
  const { data: configHistory } = useQuery<ApplicationConfig[]>({
    queryKey: ['configHistory', applicationId],
    queryFn: async () => {
      const response = await fetch(`/api/applications/${applicationId}/config/history`);
      if (!response.ok) throw new Error('获取历史失败');
      return response.json();
    },
  });

  // 加载配置到编辑器
  useEffect(() => {
    if (currentConfig) {
      setConfigContent((prev) => ({
        ...prev,
        [currentConfig.configType]: currentConfig.content,
      }));
    }
  }, [currentConfig]);

  // 保存配置
  const saveMutation = useMutation({
    mutationFn: async (data: { content: string; description: string }) => {
      const response = await fetch(`/api/applications/${applicationId}/config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          configType: activeTab,
          content: data.content,
          description: data.description,
        }),
      });
      if (!response.ok) throw new Error('保存配置失败');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applicationConfig', applicationId] });
      queryClient.invalidateQueries({ queryKey: ['configHistory', applicationId] });
      setToast({ message: '配置保存成功', type: 'success' });
    },
    onError: (error: Error) => {
      setToast({ message: `保存失败: ${error.message}`, type: 'error' });
    },
  });

  // 应用配置
  const applyMutation = useMutation({
    mutationFn: async (configId: string) => {
      const response = await fetch(`/api/applications/${applicationId}/config/${configId}/apply`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('应用配置失败');
      return response.json();
    },
    onSuccess: () => {
      setToast({ message: '配置已应用，应用需要重启才能生效', type: 'info' });
    },
    onError: (error: Error) => {
      setToast({ message: `应用失败: ${error.message}`, type: 'error' });
    },
  });

  // 验证配置
  const validateMutation = useMutation({
    mutationFn: async (content: string) => {
      const response = await fetch(`/api/applications/${applicationId}/config/validate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ configType: activeTab, content }),
      });
      if (!response.ok) throw new Error('验证失败');
      return response.json();
    },
    onSuccess: (data) => {
      if (data.valid) {
        setToast({ message: '配置验证通过', type: 'success' });
      } else {
        setToast({ message: `配置验证失败: ${data.errors.join(', ')}`, type: 'error' });
      }
    },
  });

  // 导出配置
  const exportConfig = useCallback(
    async (format: 'json' | 'yaml') => {
      try {
        const response = await fetch(
          `/api/applications/${applicationId}/config/${currentConfig?.id}/export/${format}`
        );
        if (!response.ok) throw new Error('导出失败');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `config-${applicationId}.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);

        setToast({ message: '配置导出成功', type: 'success' });
        setShowExportModal(false);
      } catch (error) {
        setToast({ message: `导出失败: ${error instanceof Error ? error.message : '未知错误'}`, type: 'error' });
      }
    },
    [applicationId, currentConfig]
  );

  // 保存为模板
  const saveAsTemplateMutation = useMutation({
    mutationFn: async (data: { name: string; description: string }) => {
      const response = await fetch(`/api/applications/${applicationId}/config/templates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: data.name,
          description: data.description,
          content: configContent[activeTab],
          type: activeTab,
        }),
      });
      if (!response.ok) throw new Error('保存模板失败');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configTemplates'] });
      setToast({ message: '模板保存成功', type: 'success' });
      setShowSaveTemplateModal(false);
    },
  });

  // 应用模板
  const applyTemplate = useCallback((template: ConfigTemplate) => {
    setConfigContent((prev) => ({
      ...prev,
      [activeTab]: template.content,
    }));
    setShowTemplateModal(false);
    setToast({ message: `已加载模板: ${template.name}`, type: 'info' });
  }, [activeTab]);

  // 处理编辑器变化
  const handleEditorChange = useCallback((value: string) => {
    setConfigContent((prev) => ({
      ...prev,
      [activeTab]: value,
    }));
  }, [activeTab]);

  // 处理验证
  const handleValidate = useCallback((markers: editor.IMarker[]) => {
    setValidationErrors(markers);
  }, []);

  // 获取语言类型
  const getLanguage = useCallback((): 'yaml' | 'json' | 'properties' => {
    if (activeTab === 'env') return 'json';
    if (activeTab === 'spring') return 'properties';
    return 'yaml';
  }, [activeTab]);

  if (isLoading) {
    return <div className={styles.loading}>加载配置中...</div>;
  }

  return (
    <div className={styles.container}>
      {/* Header */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <Button variant="ghost" onClick={() => navigate(-1)}>
            <i className="fas fa-arrow-left"></i> 返回
          </Button>
          <div>
            <h2 className={styles.title}>配置编辑器</h2>
            <p className={styles.subtitle}>应用ID: {applicationId}</p>
          </div>
        </div>
        <div className={styles.headerRight}>
          <Button variant="outline" onClick={() => setShowTemplateModal(true)}>
            <i className="fas fa-template"></i> 加载模板
          </Button>
          <Button variant="outline" onClick={() => setShowExportModal(true)}>
            <i className="fas fa-file-export"></i> 导出
          </Button>
          <Button variant="secondary" onClick={() => saveMutation.mutate({ content: configContent[activeTab] || '', description: '' })}>
            <i className="fas fa-save"></i> 保存备份
          </Button>
        </div>
      </div>

      <div className={styles.content}>
        {/* Main Editor */}
        <div className={styles.mainPanel}>
          <div className={styles.card}>
            {/* Tabs */}
            <div className={styles.tabs}>
              <button
                className={activeTab === 'jvm' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('jvm')}
              >
                <i className="fas fa-memory"></i> JVM配置
              </button>
              <button
                className={activeTab === 'env' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('env')}
              >
                <i className="fas fa-env"></i> 环境变量
              </button>
              <button
                className={activeTab === 'spring' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('spring')}
              >
                <i className="fas fa-leaf"></i> Spring配置
              </button>
              <button
                className={activeTab === 'debug' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('debug')}
              >
                <i className="fas fa-bug"></i> 调试配置
              </button>
              <button
                className={activeTab === 'jmx' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('jmx')}
              >
                <i className="fas fa-chart-line"></i> JMX监控
              </button>
              <button
                className={activeTab === 'advanced' ? styles.tabActive : styles.tab}
                onClick={() => setActiveTab('advanced')}
              >
                <i className="fas fa-cog"></i> 高级配置
              </button>
            </div>

            {/* Editor */}
            <div className={styles.editorWrapper}>
              <CodeEditor
                value={configContent[activeTab] || ''}
                onChange={handleEditorChange}
                language={getLanguage()}
                height="500px"
                onValidate={handleValidate}
              />
              {validationErrors.length > 0 && (
                <div className={styles.validationErrors}>
                  <h4>验证错误:</h4>
                  <ul>
                    {validationErrors.map((marker, index) => (
                      <li key={index}>
                        行 {marker.startLineNumber}: {marker.message}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {/* Actions */}
            <div className={styles.actions}>
              <div className={styles.actionsLeft}>
                <Button variant="secondary" onClick={() => setConfigContent(prev => ({ ...prev, [activeTab]: currentConfig?.content || '' }))}>
                  <i className="fas fa-undo"></i> 重置
                </Button>
                <Button variant="outline" onClick={() => validateMutation.mutate(configContent[activeTab] || '')} loading={validateMutation.isPending}>
                  <i className="fas fa-check-circle"></i> 验证配置
                </Button>
              </div>
              <div className={styles.actionsRight}>
                <Button variant="primary" onClick={() => saveMutation.mutate({ content: configContent[activeTab] || '', description: '手动保存' })} loading={saveMutation.isPending}>
                  <i className="fas fa-save"></i> 保存配置
                </Button>
                <Button variant="primary" onClick={() => currentConfig && applyMutation.mutate(currentConfig.id)} loading={applyMutation.isPending}>
                  <i className="fas fa-play"></i> 应用配置
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className={styles.sidebar}>
          {/* Templates */}
          <div className={styles.card}>
            <h3 className={styles.cardTitle}>
              <i className="fas fa-template"></i> 配置模板
            </h3>
            <div className={styles.listGroup}>
              {templates?.map((template) => (
                <button
                  key={template.id}
                  className={styles.listItem}
                  onClick={() => applyTemplate(template)}
                >
                  <div className={styles.listItemTitle}>{template.name}</div>
                  <div className={styles.listItemDesc}>{template.description}</div>
                </button>
              ))}
            </div>
            <Button variant="outline" size="sm" onClick={() => setShowSaveTemplateModal(true)} className={styles.fullWidth}>
              <i className="fas fa-save"></i> 保存为模板
            </Button>
          </div>

          {/* History */}
          <div className={styles.card}>
            <h3 className={styles.cardTitle}>
              <i className="fas fa-history"></i> 配置历史
            </h3>
            <div className={styles.listGroup}>
              {configHistory?.map((config) => (
                <button
                  key={config.id}
                  className={styles.listItem}
                  onClick={() => setConfigContent(prev => ({ ...prev, [config.configType]: config.content }))}
                >
                  <div className={styles.listItemTitle}>版本 {config.version}</div>
                  <div className={styles.listItemDesc}>{new Date(config.updatedAt).toLocaleString()}</div>
                  {config.isActive && <span className={styles.badge}>当前</span>}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Modals */}
      {showTemplateModal && (
        <Modal open={showTemplateModal} title="选择配置模板" onClose={() => setShowTemplateModal(false)}>
          <div className={styles.modalContent}>
            {templates?.map((template) => (
              <div key={template.id} className={styles.templateItem}>
                <h4>{template.name}</h4>
                <p>{template.description}</p>
                <Button onClick={() => applyTemplate(template)}>应用模板</Button>
              </div>
            ))}
          </div>
        </Modal>
      )}

      {showExportModal && (
        <Modal open={showExportModal} title="导出配置" onClose={() => setShowExportModal(false)}>
          <div className={styles.modalContent}>
            <p>选择导出格式:</p>
            <div className={styles.exportButtons}>
              <Button onClick={() => exportConfig('json')}>导出为 JSON</Button>
              <Button onClick={() => exportConfig('yaml')}>导出为 YAML</Button>
            </div>
          </div>
        </Modal>
      )}

      {showSaveTemplateModal && (
        <Modal open={showSaveTemplateModal} title="保存为模板" onClose={() => setShowSaveTemplateModal(false)}>
          <form
            className={styles.modalContent}
            onSubmit={(e) => {
              e.preventDefault();
              const formData = new FormData(e.currentTarget);
              saveAsTemplateMutation.mutate({
                name: formData.get('name') as string,
                description: formData.get('description') as string,
              });
            }}
          >
            <div className={styles.formGroup}>
              <label>模板名称</label>
              <input type="text" name="name" required />
            </div>
            <div className={styles.formGroup}>
              <label>模板描述</label>
              <textarea name="description" rows={3} />
            </div>
            <Button type="submit" loading={saveAsTemplateMutation.isPending}>
              保存模板
            </Button>
          </form>
        </Modal>
      )}

      {/* Toast */}
      {toast && (
        <Toast
          id="config-editor-toast"
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default ConfigEditorPage;
