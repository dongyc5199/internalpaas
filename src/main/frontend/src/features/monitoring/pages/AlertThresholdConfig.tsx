import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Input, Loading } from '../../../shared/components';
import type { AlertThreshold } from '../../../shared/types';
import styles from './AlertThresholdConfig.module.css';

/**
 * 指标类型定义
 */
type MetricType = 'CPU' | 'MEMORY' | 'DISK' | 'NETWORK';

/**
 * 阈值表单数据
 */
interface ThresholdFormData {
  metricType: MetricType;
  warningThreshold: number;
  criticalThreshold: number;
  enabled: boolean;
  notifyEmail?: string;
}

/**
 * AlertThresholdConfig 组件
 *
 * 告警阈值配置页面 - 为服务器配置监控告警阈值
 */
export function AlertThresholdConfig(): React.JSX.Element {
  const { serverId } = useParams<{ serverId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editingMetric, setEditingMetric] = useState<MetricType | null>(null);
  const [formData, setFormData] = useState<ThresholdFormData>({
    metricType: 'CPU',
    warningThreshold: 80,
    criticalThreshold: 90,
    enabled: true,
  });

  // 获取服务器信息
  const { data: server } = useQuery({
    queryKey: [QUERY_KEYS.SERVER, serverId],
    queryFn: () => serverApi.getServer(Number(serverId)),
    enabled: !!serverId,
  });

  // 获取告警阈值配置
  const { data: thresholds, isLoading } = useQuery({
    queryKey: [QUERY_KEYS.ALERT_THRESHOLDS, serverId],
    queryFn: () => monitoringApi.getAlertThresholds(Number(serverId)),
    enabled: !!serverId,
  });

  // 保存阈值配置
  const saveMutation = useMutation({
    mutationFn: (data: AlertThreshold) => {
      if (data.id) {
        return monitoringApi.updateAlertThreshold(data.id, data);
      } else {
        return monitoringApi.saveAlertThreshold(data);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERT_THRESHOLDS, serverId] });
      setEditingMetric(null);
    },
  });

  // 删除阈值配置
  const deleteMutation = useMutation({
    mutationFn: (thresholdId: number) => monitoringApi.deleteAlertThreshold(thresholdId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERT_THRESHOLDS, serverId] });
    },
  });

  // 获取指定指标的阈值配置
  const getThresholdByMetric = (metricType: MetricType): AlertThreshold | undefined => {
    return thresholds?.find((t) => t.metricType === metricType);
  };

  // 开始编辑
  const handleEdit = (metricType: MetricType): void => {
    const threshold = getThresholdByMetric(metricType);
    if (threshold) {
      setFormData({
        metricType: threshold.metricType,
        warningThreshold: threshold.warningThreshold,
        criticalThreshold: threshold.criticalThreshold,
        enabled: threshold.enabled,
        notifyEmail: threshold.notifyEmail,
      });
    } else {
      // 新建默认值
      const defaults: Record<MetricType, { warning: number; critical: number }> = {
        CPU: { warning: 80, critical: 90 },
        MEMORY: { warning: 85, critical: 95 },
        DISK: { warning: 80, critical: 90 },
        NETWORK: { warning: 100, critical: 200 },
      };
      setFormData({
        metricType,
        warningThreshold: defaults[metricType].warning,
        criticalThreshold: defaults[metricType].critical,
        enabled: true,
      });
    }
    setEditingMetric(metricType);
  };

  // 取消编辑
  const handleCancel = (): void => {
    setEditingMetric(null);
  };

  // 保存配置
  const handleSave = async (): Promise<void> => {
    if (!serverId) return;

    const threshold = getThresholdByMetric(formData.metricType);
    const data: AlertThreshold = {
      id: threshold?.id,
      serverId: Number(serverId),
      metricType: formData.metricType,
      warningThreshold: formData.warningThreshold,
      criticalThreshold: formData.criticalThreshold,
      enabled: formData.enabled,
      notifyEmail: formData.notifyEmail,
    };

    await saveMutation.mutateAsync(data);
  };

  // 删除配置
  const handleDelete = async (thresholdId: number): Promise<void> => {
    if (window.confirm('确定要删除此告警阈值配置吗?')) {
      await deleteMutation.mutateAsync(thresholdId);
    }
  };

  // 切换启用状态
  const handleToggleEnabled = async (threshold: AlertThreshold): Promise<void> => {
    if (!threshold.id) return;
    await saveMutation.mutateAsync({
      ...threshold,
      enabled: !threshold.enabled,
    });
  };

  // 指标配置列表
  const metrics: Array<{ type: MetricType; label: string; unit: string; icon: string }> = [
    { type: 'CPU', label: 'CPU使用率', unit: '%', icon: 'cpu' },
    { type: 'MEMORY', label: '内存使用率', unit: '%', icon: 'memory-stick' },
    { type: 'DISK', label: '磁盘使用率', unit: '%', icon: 'hard-drive' },
    { type: 'NETWORK', label: '网络流量', unit: 'MB/s', icon: 'network' },
  ];

  if (isLoading) {
    return <Loading text="加载阈值配置..." fullScreen />;
  }

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate('/monitoring')}
              className={styles.backBtn}
            >
              <i data-lucide="arrow-left" />
              返回
            </Button>
            <div>
              <h1 className={styles.title}>告警阈值配置</h1>
              <p className={styles.subtitle}>
                {server?.name ? `服务器: ${server.name}` : '配置监控告警阈值'}
              </p>
            </div>
          </div>
        </div>
      </header>

      {/* 阈值配置列表 */}
      <div className={styles.metricsGrid}>
        {metrics.map((metric) => {
          const threshold = getThresholdByMetric(metric.type);
          const isEditing = editingMetric === metric.type;

          return (
            <div key={metric.type} className={styles.metricCard}>
              <div className={styles.metricHeader}>
                <div className={styles.metricInfo}>
                  <i data-lucide={metric.icon} className={styles.metricIcon} />
                  <div>
                    <h3 className={styles.metricLabel}>{metric.label}</h3>
                    <p className={styles.metricUnit}>单位: {metric.unit}</p>
                  </div>
                </div>
                {threshold && (
                  <div className={styles.metricStatus}>
                    <label className={styles.switchLabel}>
                      <input
                        type="checkbox"
                        checked={threshold.enabled}
                        onChange={() => handleToggleEnabled(threshold)}
                        className={styles.switch}
                      />
                      <span className={styles.switchText}>
                        {threshold.enabled ? '已启用' : '已禁用'}
                      </span>
                    </label>
                  </div>
                )}
              </div>

              {isEditing ? (
                /* 编辑模式 */
                <div className={styles.editForm}>
                  <div className={styles.formGroup}>
                    <label className={styles.label}>警告阈值 ({metric.unit})</label>
                    <Input
                      type="number"
                      value={formData.warningThreshold}
                      onChange={(e) =>
                        setFormData({ ...formData, warningThreshold: Number(e.target.value) })
                      }
                    />
                  </div>

                  <div className={styles.formGroup}>
                    <label className={styles.label}>严重阈值 ({metric.unit})</label>
                    <Input
                      type="number"
                      value={formData.criticalThreshold}
                      onChange={(e) =>
                        setFormData({ ...formData, criticalThreshold: Number(e.target.value) })
                      }
                    />
                  </div>

                  <div className={styles.formGroup}>
                    <label className={styles.label}>
                      通知邮箱 <span className={styles.optional}>(可选)</span>
                    </label>
                    <Input
                      type="email"
                      value={formData.notifyEmail ?? ''}
                      onChange={(e) => setFormData({ ...formData, notifyEmail: e.target.value })}
                      placeholder="alerts@example.com"
                    />
                  </div>

                  <div className={styles.formActions}>
                    <Button onClick={handleCancel} variant="secondary" size="sm">
                      取消
                    </Button>
                    <Button
                      onClick={handleSave}
                      size="sm"
                      disabled={
                        saveMutation.isPending ||
                        formData.warningThreshold >= formData.criticalThreshold
                      }
                    >
                      保存
                    </Button>
                  </div>
                  {formData.warningThreshold >= formData.criticalThreshold && (
                    <p className={styles.errorText}>警告阈值必须小于严重阈值</p>
                  )}
                </div>
              ) : threshold ? (
                /* 显示模式 - 已配置 */
                <div className={styles.thresholdDisplay}>
                  <div className={styles.thresholdItem}>
                    <span className={styles.thresholdLabel}>警告阈值:</span>
                    <span className={`${styles.thresholdValue} ${styles.warning}`}>
                      {threshold.warningThreshold} {metric.unit}
                    </span>
                  </div>
                  <div className={styles.thresholdItem}>
                    <span className={styles.thresholdLabel}>严重阈值:</span>
                    <span className={`${styles.thresholdValue} ${styles.critical}`}>
                      {threshold.criticalThreshold} {metric.unit}
                    </span>
                  </div>
                  {threshold.notifyEmail && (
                    <div className={styles.thresholdItem}>
                      <span className={styles.thresholdLabel}>通知邮箱:</span>
                      <span className={styles.emailText}>{threshold.notifyEmail}</span>
                    </div>
                  )}
                  <div className={styles.cardActions}>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleEdit(metric.type)}
                    >
                      编辑
                    </Button>
                    {threshold.id && (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleDelete(threshold.id!)}
                        disabled={deleteMutation.isPending}
                      >
                        删除
                      </Button>
                    )}
                  </div>
                </div>
              ) : (
                /* 显示模式 - 未配置 */
                <div className={styles.emptyState}>
                  <p className={styles.emptyText}>未配置告警阈值</p>
                  <Button size="sm" onClick={() => handleEdit(metric.type)}>
                    配置阈值
                  </Button>
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
          <p className={styles.helpTitle}>阈值说明</p>
          <ul className={styles.helpList}>
            <li>警告阈值: 当监控指标超过此值时,将触发警告级别的告警</li>
            <li>严重阈值: 当监控指标超过此值时,将触发严重级别的告警</li>
            <li>通知邮箱: 配置后,触发告警时会向该邮箱发送通知(需要配置邮件服务)</li>
            <li>可以随时启用/禁用告警,禁用后不会触发新的告警</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
