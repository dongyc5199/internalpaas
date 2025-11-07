import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { serverApi, type ServerFormData } from '../../../shared/api/serverApi';
import { Button } from '../../../shared/components/Button';
import { Input } from '../../../shared/components/Input';
import type { Server } from '../../../shared/types/server';
import styles from './ServerForm.module.css';

interface ServerFormProps {
  server?: Server;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * ServerForm 组件
 *
 * 服务器创建/编辑表单
 *
 * 功能:
 * - 表单验证
 * - 创建/更新服务器
 * - 连接测试
 */
export function ServerForm({ server, onSuccess, onCancel }: ServerFormProps): React.JSX.Element {
  const isEdit = !!server;

  // 表单状态
  const [formData, setFormData] = useState<ServerFormData>({
    name: server?.name ?? '',
    host: server?.host ?? '',
    port: server?.port ?? 22,
    username: server?.username ?? '',
    password: '',
    tags: server?.tags ?? [],
  });

  const [tagInput, setTagInput] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  /**
   * 创建服务器
   */
  const createMutation = useMutation({
    mutationFn: (data: ServerFormData) => serverApi.createServer(data),
    onSuccess: () => {
      onSuccess();
    },
    onError: (error) => {
      setErrors({ submit: error instanceof Error ? error.message : '创建失败' });
    },
  });

  /**
   * 更新服务器
   */
  const updateMutation = useMutation({
    mutationFn: (data: ServerFormData) => {
      if (!server) throw new Error('Server not found');
      return serverApi.updateServer(server.id, data);
    },
    onSuccess: () => {
      onSuccess();
    },
    onError: (error) => {
      setErrors({ submit: error instanceof Error ? error.message : '更新失败' });
    },
  });

  /**
   * 表单验证
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = '请输入服务器名称';
    }

    if (!formData.host.trim()) {
      newErrors.host = '请输入主机地址';
    }

    if (!formData.port || formData.port < 1 || formData.port > 65535) {
      newErrors.port = '请输入有效的端口号 (1-65535)';
    }

    if (!formData.username.trim()) {
      newErrors.username = '请输入用户名';
    }

    if (!isEdit && !formData.password) {
      newErrors.password = '请输入密码';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * 处理提交
   */
  const handleSubmit = (e: React.FormEvent): void => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    const mutation = isEdit ? updateMutation : createMutation;
    mutation.mutate(formData);
  };

  /**
   * 处理字段变化
   */
  const handleChange = (field: keyof ServerFormData, value: string | number): void => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // 清除该字段的错误
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  /**
   * 添加标签
   */
  const handleAddTag = (): void => {
    const tag = tagInput.trim();
    if (tag && !formData.tags?.includes(tag)) {
      setFormData((prev) => ({
        ...prev,
        tags: [...(prev.tags ?? []), tag],
      }));
      setTagInput('');
    }
  };

  /**
   * 删除标签
   */
  const handleRemoveTag = (tag: string): void => {
    setFormData((prev) => ({
      ...prev,
      tags: prev.tags?.filter((t) => t !== tag) ?? [],
    }));
  };

  /**
   * 处理标签输入回车
   */
  const handleTagKeyDown = (e: React.KeyboardEvent): void => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddTag();
    }
  };

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      {/* 提交错误 */}
      {errors.submit && (
        <div className={styles.errorAlert}>
          <i data-lucide="alert-circle" />
          <span>{errors.submit}</span>
        </div>
      )}

      {/* 服务器名称 */}
      <div className={styles.formGroup}>
        <label htmlFor="name" className={styles.label}>
          服务器名称 <span className={styles.required}>*</span>
        </label>
        <Input
          id="name"
          value={formData.name}
          onChange={(e) => handleChange('name', e.target.value)}
          placeholder="例如: Production Server 1"
          error={errors.name}
          disabled={isSubmitting}
        />
      </div>

      {/* 主机地址 */}
      <div className={styles.formGroup}>
        <label htmlFor="host" className={styles.label}>
          主机地址 <span className={styles.required}>*</span>
        </label>
        <Input
          id="host"
          value={formData.host}
          onChange={(e) => handleChange('host', e.target.value)}
          placeholder="例如: 192.168.1.100 或 server.example.com"
          error={errors.host}
          disabled={isSubmitting}
        />
      </div>

      {/* 端口和用户名 */}
      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label htmlFor="port" className={styles.label}>
            SSH 端口 <span className={styles.required}>*</span>
          </label>
          <Input
            id="port"
            type="number"
            value={String(formData.port)}
            onChange={(e) => handleChange('port', parseInt(e.target.value, 10))}
            placeholder="22"
            error={errors.port}
            disabled={isSubmitting}
          />
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="username" className={styles.label}>
            用户名 <span className={styles.required}>*</span>
          </label>
          <Input
            id="username"
            value={formData.username}
            onChange={(e) => handleChange('username', e.target.value)}
            placeholder="例如: root"
            error={errors.username}
            disabled={isSubmitting}
          />
        </div>
      </div>

      {/* 密码 */}
      <div className={styles.formGroup}>
        <label htmlFor="password" className={styles.label}>
          密码 {!isEdit && <span className={styles.required}>*</span>}
        </label>
        <Input
          id="password"
          type="password"
          value={formData.password}
          onChange={(e) => handleChange('password', e.target.value)}
          placeholder={isEdit ? '留空表示不修改密码' : '请输入密码'}
          error={errors.password}
          disabled={isSubmitting}
        />
        {isEdit && (
          <p className={styles.hint}>留空表示不修改密码</p>
        )}
      </div>

      {/* 标签 */}
      <div className={styles.formGroup}>
        <label htmlFor="tags" className={styles.label}>
          标签
        </label>
        <div className={styles.tagInput}>
          <Input
            id="tags"
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={handleTagKeyDown}
            placeholder="输入标签后按回车添加"
            disabled={isSubmitting}
          />
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={handleAddTag}
            disabled={!tagInput.trim() || isSubmitting}
          >
            添加
          </Button>
        </div>
        {formData.tags && formData.tags.length > 0 && (
          <div className={styles.tags}>
            {formData.tags.map((tag) => (
              <span key={tag} className={styles.tag}>
                {tag}
                <button
                  type="button"
                  className={styles.tagRemove}
                  onClick={() => handleRemoveTag(tag)}
                  disabled={isSubmitting}
                >
                  <i data-lucide="x" />
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* 表单操作 */}
      <div className={styles.formActions}>
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          取消
        </Button>
        <Button type="submit" variant="primary" loading={isSubmitting} disabled={isSubmitting}>
          {isEdit ? '保存' : '创建'}
        </Button>
      </div>
    </form>
  );
}
