import type { ComponentType, LazyExoticComponent } from 'react';
import type { UserRole } from '../shared/types/user';

export type RouteComponent = ComponentType<unknown> | LazyExoticComponent<ComponentType<unknown>>;

export interface AppRoute {
  path: string;
  component: RouteComponent;
  requiredRoles?: UserRole[];
  suspenseText?: string;
}
