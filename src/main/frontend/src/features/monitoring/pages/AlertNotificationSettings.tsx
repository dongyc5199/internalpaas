import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
// import { monitoringApi } from '../../../shared/api/monitoringApi'; // TODO: Implement API methods
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Input, Loading } from '../../../shared/components';
import styles from './AlertNotificationSettings.module.css';

/**
 * 通知渠道类型
 */
type NotificationChannel = 'EMAIL' | 'WEBHOOK' | 'SMS';

/**
 * 通知配置
 */
interface NotificationConfig {
  id?: number;
  channel: NotificationChannel;
  enabled: boolean;
  // Email配置
  emailRecipients?: string[];
  emailSubjectTemplate?: string;
  // Webhook配置
  webhookUrl?: string;
  webhookHeaders?: Record<string, string>;
  // SMS配置
  smsRecipients?: string[];
}

/**
 * AlertNotificationSettings 组件
 *
 * 告警通知设置页面 - 配置告警通知渠道和接收人
 */
export function AlertNotificationSettings(): React.JSX.Element {
  const queryClient = useQueryClient();
  const [editingChannel, setEditingChannel] = useState<NotificationChannel | null>(null);
  const [emailInput, setEmailInput] = useState('');
  const [smsInput, setSmsInput] = useState('');
  const [webhookUrl, setWebhookUrl] = useState('');
  const [webhookHeaderKey, setWebhookHeaderKey] = useState('');
  const [webhookHeaderValue, setWebhookHeaderValue] = useState('');
  const [webhookHeaders, setWebhookHeaders] = useState<Record<string, string>>({});

  // 获取通知配置（模拟数据 - 实际应从API获取）
  const { data: notificationConfigs, isLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'notification-settings'],
    queryFn: async () => {
      // 模拟API调用 - 实际应使用 monitoringApi.getNotificationSettings()
      return [
        {
          id: 1,
          channel: 'EMAIL' as NotificationChannel,
          enabled: true,
          emailRecipients: ['admin@example.com', 'ops@example.com'],
          emailSubjectTemplate: '[告警] {level} - {serverName}',
        },
        {
          id: 2,
          channel: 'WEBHOOK' as NotificationChannel,
          enabled: false,
          webhookUrl: 'https://hooks.example.com/alerts',
          webhookHeaders: { 'Content-Type': 'application/json' },
        },
        {
          id: 3,
          channel: 'SMS' as NotificationChannel,
          enabled: false,
          smsRecipients: [],
        },
      ] as NotificationConfig[];
    },
  });

  // 保存通知配置
  const saveMutation = useMutation({
    mutationFn: async (config: NotificationConfig) => {
      // 模拟API调用 - 实际应使用 monitoringApi.saveNotificationConfig(config)
      return config;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.MONITORING, 'notification-settings'] });
      setEditingChannel(null);
      resetForm();
    },
  });

  // 切换启用状态
  const toggleMutation = useMutation({
    mutationFn: async (params: { channel: NotificationChannel; enabled: boolean }) => {
      // 模拟API调用
      return params;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.MONITORING, 'notification-settings'] });
    },
  });

  // 重置表单
  const resetForm = (): void => {
    setEmailInput('');
    setSmsInput('');
    setWebhookUrl('');
    setWebhookHeaderKey('');
    setWebhookHeaderValue('');
    setWebhookHeaders({});
  };

  // 获取指定渠道的配置
  const getChannelConfig = (channel: NotificationChannel): NotificationConfig | undefined => {
    return notificationConfigs?.find((c) => c.channel === channel);
  };

  // 开始编辑
  const handleEdit = (channel: NotificationChannel): void => {
    const config = getChannelConfig(channel);
    setEditingChannel(channel);

    if (config) {
      if (channel === 'EMAIL') {
        // Email配置已加载
      } else if (channel === 'WEBHOOK') {
        setWebhookUrl(config.webhookUrl ?? '');
        setWebhookHeaders(config.webhookHeaders ?? {});
      } else if (channel === 'SMS') {
        // SMS配置已加载
      }
    }
  };

  // 取消编辑
  const handleCancel = (): void => {
    setEditingChannel(null);
    resetForm();
  };

  // 保存配置
  const handleSave = async (channel: NotificationChannel): Promise<void> => {
    const existingConfig = getChannelConfig(channel);
    let config: NotificationConfig;

    if (channel === 'EMAIL') {
      config = {
        ...existingConfig,
        channel: 'EMAIL',
        enabled: existingConfig?.enabled ?? true,
        emailRecipients: existingConfig?.emailRecipients ?? [],
        emailSubjectTemplate: '[告警] {level} - {serverName}',
      };
    } else if (channel === 'WEBHOOK') {
      config = {
        ...existingConfig,
        channel: 'WEBHOOK',
        enabled: existingConfig?.enabled ?? false,
        webhookUrl,
        webhookHeaders,
      };
    } else {
      config = {
        ...existingConfig,
        channel: 'SMS',
        enabled: existingConfig?.enabled ?? false,
        smsRecipients: existingConfig?.smsRecipients ?? [],
      };
    }

    await saveMutation.mutateAsync(config);
  };

  // 切换启用状态
  const handleToggleEnabled = async (
    channel: NotificationChannel,
    enabled: boolean
  ): Promise<void> => {
    await toggleMutation.mutateAsync({ channel, enabled: !enabled });
  };

  // 添加邮箱
  const handleAddEmail = (channel: NotificationChannel): void => {
    if (!emailInput.trim()) return;

    const config = getChannelConfig(channel);
    const updatedRecipients = [...(config?.emailRecipients ?? []), emailInput.trim()];

    saveMutation.mutate({
      ...config,
      channel,
      emailRecipients: updatedRecipients,
    } as NotificationConfig);

    setEmailInput('');
  };

  // 删除邮箱
  const handleRemoveEmail = (channel: NotificationChannel, email: string): void => {
    const config = getChannelConfig(channel);
    const updatedRecipients = (config?.emailRecipients ?? []).filter((e) => e !== email);

    saveMutation.mutate({
      ...config,
      channel,
      emailRecipients: updatedRecipients,
    } as NotificationConfig);
  };

  // 添加手机号
  const handleAddSms = (channel: NotificationChannel): void => {
    if (!smsInput.trim()) return;

    const config = getChannelConfig(channel);
    const updatedRecipients = [...(config?.smsRecipients ?? []), smsInput.trim()];

    saveMutation.mutate({
      ...config,
      channel,
      smsRecipients: updatedRecipients,
    } as NotificationConfig);

    setSmsInput('');
  };

  // 删除手机号
  const handleRemoveSms = (channel: NotificationChannel, phone: string): void => {
    const config = getChannelConfig(channel);
    const updatedRecipients = (config?.smsRecipients ?? []).filter((p) => p !== phone);

    saveMutation.mutate({
      ...config,
      channel,
      smsRecipients: updatedRecipients,
    } as NotificationConfig);
  };

  // 添加Webhook Header
  const handleAddHeader = (): void => {
    if (!webhookHeaderKey.trim() || !webhookHeaderValue.trim()) return;

    setWebhookHeaders({
      ...webhookHeaders,
      [webhookHeaderKey.trim()]: webhookHeaderValue.trim(),
    });

    setWebhookHeaderKey('');
    setWebhookHeaderValue('');
  };

  // 删除Webhook Header
  const handleRemoveHeader = (key: string): void => {
    const { [key]: _, ...rest } = webhookHeaders;
    setWebhookHeaders(rest);
  };

  // 渠道配置
  const channels = [
    {
      type: 'EMAIL' as NotificationChannel,
      label: '邮件通知',
      icon: 'mail',
      description: '通过邮件发送告警通知',
    },
    {
      type: 'WEBHOOK' as NotificationChannel,
      label: 'Webhook通知',
      icon: 'webhook',
      description: '通过HTTP POST发送告警到自定义URL',
    },
    {
      type: 'SMS' as NotificationChannel,
      label: '短信通知',
      icon: 'smartphone',
      description: '通过短信发送告警通知',
    },
  ];

  if (isLoading) {
    return <Loading text="加载通知设置..." fullScreen />;
  }

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>告警通知设置</h1>
            <p className={styles.subtitle}>配置告警通知渠道和接收人</p>
          </div>
        </div>
      </header>

      {/* 通知渠道列表 */}
      <div className={styles.channelsGrid}>
        {channels.map((channelInfo) => {
          const config = getChannelConfig(channelInfo.type);
          const isEditing = editingChannel === channelInfo.type;
          const isEnabled = config?.enabled ?? false;

          return (
            <div key={channelInfo.type} className={styles.channelCard}>
              <div className={styles.channelHeader}>
                <div className={styles.channelInfo}>
                  <i data-lucide={channelInfo.icon} className={styles.channelIcon} />
                  <div>
                    <h3 className={styles.channelLabel}>{channelInfo.label}</h3>
                    <p className={styles.channelDescription}>{channelInfo.description}</p>
                  </div>
                </div>
                <label className={styles.switchLabel}>
                  <input
                    type="checkbox"
                    checked={isEnabled}
                    onChange={() => handleToggleEnabled(channelInfo.type, isEnabled)}
                    className={styles.switch}
                  />
                  <span className={styles.switchText}>{isEnabled ? '已启用' : '已禁用'}</span>
                </label>
              </div>

              {/* 邮件配置 */}
              {channelInfo.type === 'EMAIL' && (
                <div className={styles.channelContent}>
                  <div className={styles.recipientsList}>
                    <div className={styles.recipientsHeader}>
                      <span className={styles.recipientsLabel}>接收邮箱</span>
                      <span className={styles.recipientsCount}>
                        {config?.emailRecipients?.length ?? 0} 个
                      </span>
                    </div>
                    <div className={styles.recipients}>
                      {config?.emailRecipients?.map((email) => (
                        <div key={email} className={styles.recipientTag}>
                          <span>{email}</span>
                          <button
                            onClick={() => handleRemoveEmail(channelInfo.type, email)}
                            className={styles.removeBtn}
                          >
                            <i data-lucide="x" />
                          </button>
                        </div>
                      ))}
                    </div>
                    <div className={styles.addRecipient}>
                      <Input
                        type="email"
                        value={emailInput}
                        onChange={(e) => setEmailInput(e.target.value)}
                        placeholder="输入邮箱地址"
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            handleAddEmail(channelInfo.type);
                          }
                        }}
                      />
                      <Button size="sm" onClick={() => handleAddEmail(channelInfo.type)}>
                        添加
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {/* Webhook配置 */}
              {channelInfo.type === 'WEBHOOK' && (
                <div className={styles.channelContent}>
                  {isEditing ? (
                    <div className={styles.editForm}>
                      <div className={styles.formGroup}>
                        <label className={styles.label}>Webhook URL</label>
                        <Input
                          type="url"
                          value={webhookUrl}
                          onChange={(e) => setWebhookUrl(e.target.value)}
                          placeholder="https://hooks.example.com/alerts"
                        />
                      </div>

                      <div className={styles.formGroup}>
                        <label className={styles.label}>HTTP Headers</label>
                        <div className={styles.headersList}>
                          {Object.entries(webhookHeaders).map(([key, value]) => (
                            <div key={key} className={styles.headerItem}>
                              <span className={styles.headerKey}>{key}:</span>
                              <span className={styles.headerValue}>{value}</span>
                              <button
                                onClick={() => handleRemoveHeader(key)}
                                className={styles.removeBtn}
                              >
                                <i data-lucide="x" />
                              </button>
                            </div>
                          ))}
                        </div>
                        <div className={styles.addHeader}>
                          <Input
                            type="text"
                            value={webhookHeaderKey}
                            onChange={(e) => setWebhookHeaderKey(e.target.value)}
                            placeholder="Header名称"
                          />
                          <Input
                            type="text"
                            value={webhookHeaderValue}
                            onChange={(e) => setWebhookHeaderValue(e.target.value)}
                            placeholder="Header值"
                          />
                          <Button size="sm" onClick={handleAddHeader}>
                            添加
                          </Button>
                        </div>
                      </div>

                      <div className={styles.formActions}>
                        <Button variant="secondary" size="sm" onClick={handleCancel}>
                          取消
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => handleSave(channelInfo.type)}
                          disabled={saveMutation.isPending || !webhookUrl.trim()}
                        >
                          保存
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className={styles.displayContent}>
                      {config?.webhookUrl ? (
                        <>
                          <div className={styles.infoRow}>
                            <span className={styles.infoLabel}>URL:</span>
                            <span className={styles.infoValue}>{config.webhookUrl}</span>
                          </div>
                          {config.webhookHeaders && Object.keys(config.webhookHeaders).length > 0 && (
                            <div className={styles.infoRow}>
                              <span className={styles.infoLabel}>Headers:</span>
                              <span className={styles.infoValue}>
                                {Object.keys(config.webhookHeaders).length} 个
                              </span>
                            </div>
                          )}
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleEdit(channelInfo.type)}
                          >
                            编辑配置
                          </Button>
                        </>
                      ) : (
                        <div className={styles.emptyConfig}>
                          <p>未配置Webhook</p>
                          <Button size="sm" onClick={() => handleEdit(channelInfo.type)}>
                            配置Webhook
                          </Button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* SMS配置 */}
              {channelInfo.type === 'SMS' && (
                <div className={styles.channelContent}>
                  <div className={styles.recipientsList}>
                    <div className={styles.recipientsHeader}>
                      <span className={styles.recipientsLabel}>接收手机号</span>
                      <span className={styles.recipientsCount}>
                        {config?.smsRecipients?.length ?? 0} 个
                      </span>
                    </div>
                    <div className={styles.recipients}>
                      {config?.smsRecipients?.map((phone) => (
                        <div key={phone} className={styles.recipientTag}>
                          <span>{phone}</span>
                          <button
                            onClick={() => handleRemoveSms(channelInfo.type, phone)}
                            className={styles.removeBtn}
                          >
                            <i data-lucide="x" />
                          </button>
                        </div>
                      ))}
                    </div>
                    <div className={styles.addRecipient}>
                      <Input
                        type="tel"
                        value={smsInput}
                        onChange={(e) => setSmsInput(e.target.value)}
                        placeholder="输入手机号"
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            handleAddSms(channelInfo.type);
                          }
                        }}
                      />
                      <Button size="sm" onClick={() => handleAddSms(channelInfo.type)}>
                        添加
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* 说明文字 */}
      <div className={styles.helpText}>
        <i data-lucide="info" />
        <div>
          <p className={styles.helpTitle}>通知说明</p>
          <ul className={styles.helpList}>
            <li>邮件通知: 告警触发时会向配置的邮箱发送通知邮件</li>
            <li>Webhook通知: 通过HTTP POST将告警数据发送到自定义URL，支持自定义Headers</li>
            <li>短信通知: 向配置的手机号发送短信告警（需要配置短信服务）</li>
            <li>可以同时启用多个通知渠道，告警会通过所有启用的渠道发送</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
