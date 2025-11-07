import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { userApi, type UserFormData } from '../../../shared/api/userApi';
import { Button, Input, Select } from '../../../shared/components';
import { UserRole, type User } from '../../../shared/types/user';
import styles from './UserForm.module.css';

/**
 * UserForm 组件属性
 */
interface UserFormProps {
  /** 用户数据（编辑模式） */
  user?: User;
  /** 成功回调 */
  onSuccess: () => void;
  /** 取消回调 */
  onCancel: () => void;
}

/**
 * UserForm 组件
 *
 * 用户创建/编辑表单
 *
 * 功能:
 * - 创建新用户
 * - 编辑现有用户
 * - 表单验证
 * - 错误处理
 */
export function UserForm({ user, onSuccess, onCancel }: UserFormProps): React.JSX.Element {
  const isEditMode = !!user;

  // 表单状态
  const [formData, setFormData] = useState<UserFormData>({
    username: user?.username ?? '',
    email: user?.email ?? '',
    password: '',
    role: user?.role ?? UserRole.DEVELOPER,
    displayName: user?.displayName ?? '',
  });

  // 错误状态
  const [errors, setErrors] = useState<Record<string, string>>({});

  /**
   * 创建用户
   */
  const createMutation = useMutation({
    mutationFn: (data: UserFormData) => userApi.createUser(data),
    onSuccess: () => {
      onSuccess();
    },
    onError: (error: Error) => {
      setErrors({ submit: error.message || '创建失败' });
    },
  });

  /**
   * 更新用户
   */
  const updateMutation = useMutation({
    mutationFn: (data: Partial<UserFormData>) => {
      if (!user?.id) throw new Error('用户ID不存在');
      return userApi.updateUser(user.id, data);
    },
    onSuccess: () => {
      onSuccess();
    },
    onError: (error: Error) => {
      setErrors({ submit: error.message || '更新失败' });
    },
  });

  /**
   * 表单验证
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    // 用户名验证
    if (!formData.username.trim()) {
      newErrors.username = '请输入用户名';
    } else if (!/^[a-zA-Z0-9_-]{3,20}$/.test(formData.username)) {
      newErrors.username = '用户名只能包含字母、数字、下划线和连字符，长度3-20位';
    }

    // 邮箱验证
    if (!formData.email.trim()) {
      newErrors.email = '请输入邮箱';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = '请输入有效的邮箱地址';
    }

    // 密码验证（仅新建时必填）
    if (!isEditMode) {
      if (!formData.password) {
        newErrors.password = '请输入密码';
      } else if (formData.password.length < 6) {
        newErrors.password = '密码长度至少6位';
      }
    } else if (formData.password && formData.password.length < 6) {
      newErrors.password = '密码长度至少6位';
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

    if (isEditMode) {
      // 编辑模式：只提交修改的字段
      const updateData: Partial<UserFormData> = {
        email: formData.email,
        role: formData.role,
        displayName: formData.displayName || undefined,
      };

      // 只在密码字段有值时才更新密码
      if (formData.password) {
        updateData.password = formData.password;
      }

      updateMutation.mutate(updateData);
    } else {
      // 创建模式：提交完整表单
      createMutation.mutate(formData);
    }
  };

  /**
   * 处理输入变化
   */
  const handleChange = (field: keyof UserFormData, value: string | UserRole): void => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // 清除对应字段的错误
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      {/* 用户名 */}
      <div className={styles.formGroup}>
        <label htmlFor="username" className={styles.label}>
          用户名 <span className={styles.required}>*</span>
        </label>
        <Input
          id="username"
          value={formData.username}
          onChange={(e) => handleChange('username', e.target.value)}
          placeholder="请输入用户名"
          disabled={isEditMode || isPending}
        />
        {errors.username && <span className={styles.error}>{errors.username}</span>}
        {!isEditMode && (
          <span className={styles.hint}>用户名只能包含字母、数字、下划线和连字符，长度3-20位</span>
        )}
      </div>

      {/* 邮箱 */}
      <div className={styles.formGroup}>
        <label htmlFor="email" className={styles.label}>
          邮箱 <span className={styles.required}>*</span>
        </label>
        <Input
          id="email"
          type="email"
          value={formData.email}
          onChange={(e) => handleChange('email', e.target.value)}
          placeholder="请输入邮箱"
          disabled={isPending}
        />
        {errors.email && <span className={styles.error}>{errors.email}</span>}
      </div>

      {/* 显示名称 */}
      <div className={styles.formGroup}>
        <label htmlFor="displayName" className={styles.label}>
          显示名称
        </label>
        <Input
          id="displayName"
          value={formData.displayName}
          onChange={(e) => handleChange('displayName', e.target.value)}
          placeholder="请输入显示名称（可选）"
          disabled={isPending}
        />
        {errors.displayName && <span className={styles.error}>{errors.displayName}</span>}
      </div>

      {/* 角色 */}
      <div className={styles.formGroup}>
        <label htmlFor="role" className={styles.label}>
          角色 <span className={styles.required}>*</span>
        </label>
        <Select
          id="role"
          value={formData.role}
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
            handleChange('role', e.target.value as UserRole)
          }
          disabled={isPending}
        >
          <option value={UserRole.DEVELOPER}>开发者</option>
          <option value={UserRole.ADMIN}>管理员</option>
          <option value={UserRole.SUPER_ADMIN}>超级管理员</option>
        </Select>
        {errors.role && <span className={styles.error}>{errors.role}</span>}
      </div>

      {/* 密码 */}
      <div className={styles.formGroup}>
        <label htmlFor="password" className={styles.label}>
          密码 {!isEditMode && <span className={styles.required}>*</span>}
        </label>
        <Input
          id="password"
          type="password"
          value={formData.password}
          onChange={(e) => handleChange('password', e.target.value)}
          placeholder={isEditMode ? '留空表示不修改密码' : '请输入密码'}
          disabled={isPending}
        />
        {errors.password && <span className={styles.error}>{errors.password}</span>}
        {!isEditMode && <span className={styles.hint}>密码长度至少6位</span>}
        {isEditMode && <span className={styles.hint}>如需修改密码，请输入新密码；否则留空</span>}
      </div>

      {/* 提交错误 */}
      {errors.submit && (
        <div className={styles.submitError}>
          <i data-lucide="alert-circle" />
          <span>{errors.submit}</span>
        </div>
      )}

      {/* 操作按钮 */}
      <div className={styles.actions}>
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isPending}>
          取消
        </Button>
        <Button type="submit" variant="primary" loading={isPending}>
          {isEditMode ? '保存' : '创建'}
        </Button>
      </div>
    </form>
  );
}
