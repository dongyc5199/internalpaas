/**
 * Policy Type Definitions
 *
 * Represents access control policies and permissions in the Deploy Platform.
 * Used for managing user/role-based access to applications and resources.
 */

/**
 * Policy Action
 * Defines the operations that can be controlled by policies
 */
export type PolicyAction =
  | 'read'         // View resource details
  | 'write'        // Create/update resources
  | 'delete'       // Delete resources
  | 'deploy'       // Deploy applications
  | 'approve'      // Approve releases
  | 'configure'    // Modify configurations
  | 'manage'       // Full administrative access
  | '*';           // All actions (wildcard)

/**
 * Policy Effect
 * Whether the policy allows or denies access
 */
export type PolicyEffect = 'allow' | 'deny';

/**
 * Policy Resource Type
 * Types of resources that can be controlled by policies
 */
export type PolicyResourceType =
  | 'application'
  | 'release'
  | 'server'
  | 'user'
  | 'policy'
  | 'config'
  | 'logs'
  | '*'; // All resources

/**
 * Policy Subject Type
 * Who the policy applies to
 */
export type PolicySubjectType = 'user' | 'role' | 'group';

/**
 * Policy Status
 */
export type PolicyStatus = 'active' | 'inactive' | 'expired';

/**
 * Policy Entity
 * Main interface representing an access control policy
 */
export interface Policy {
  /** Unique identifier */
  id: string;

  /** Policy name/title */
  name: string;

  /** Detailed description of the policy's purpose */
  description?: string;

  /** Current status */
  status: PolicyStatus;

  /** Effect - allow or deny */
  effect: PolicyEffect;

  /** Subject type (user, role, group) */
  subjectType: PolicySubjectType;

  /** Subject IDs (user IDs, role names, group IDs) */
  subjectIds: string[];

  /** Subject display names (denormalized for UI) */
  subjectNames?: string[];

  /** Resource type this policy applies to */
  resourceType: PolicyResourceType;

  /** Specific resource IDs (empty array = all resources of type) */
  resourceIds: string[];

  /** Resource names (denormalized for UI) */
  resourceNames?: string[];

  /** Actions allowed/denied by this policy */
  actions: PolicyAction[];

  /** Additional conditions (JSON-based rules) */
  conditions?: PolicyCondition;

  /** Policy priority (higher number = higher priority) */
  priority: number;

  /** User ID who created this policy */
  createdBy: string;

  /** Username (denormalized) */
  createdByUsername?: string;

  /** ISO 8601 timestamp of creation */
  createdAt: string;

  /** ISO 8601 timestamp of last update */
  updatedAt: string;

  /** ISO 8601 timestamp when policy expires (optional) */
  expiresAt?: string;

  /** Tags for categorization/filtering */
  tags?: string[];

  /** Whether this is a system-managed policy (cannot be deleted) */
  isSystem?: boolean;
}

/**
 * Policy Condition
 * Advanced conditions for policy evaluation
 */
export interface PolicyCondition {
  /** IP address whitelist/blacklist */
  ipAddresses?: {
    type: 'whitelist' | 'blacklist';
    addresses: string[]; // CIDR notation supported
  };

  /** Time-based restrictions */
  timeRestriction?: {
    startTime: string; // HH:mm format (e.g., "09:00")
    endTime: string;   // HH:mm format (e.g., "18:00")
    timezone?: string; // IANA timezone (e.g., "Asia/Shanghai")
    daysOfWeek?: number[]; // 0-6 (Sunday=0)
  };

  /** Environment restrictions */
  environments?: string[]; // e.g., ["production", "staging"]

  /** Custom JSON-based conditions */
  custom?: Record<string, unknown>;
}

/**
 * Policy Summary (for list views)
 * Lightweight version for list/grid displays
 */
export interface PolicySummary {
  id: string;
  name: string;
  status: PolicyStatus;
  effect: PolicyEffect;
  subjectType: PolicySubjectType;
  subjectCount: number;
  resourceType: PolicyResourceType;
  resourceCount: number;
  createdAt: string;
  expiresAt?: string;
}

/**
 * Policy Creation Request
 * DTO for creating a new policy
 */
export interface CreatePolicyRequest {
  name: string;
  description?: string;
  effect: PolicyEffect;
  subjectType: PolicySubjectType;
  subjectIds: string[];
  resourceType: PolicyResourceType;
  resourceIds: string[];
  actions: PolicyAction[];
  conditions?: PolicyCondition;
  priority?: number;
  expiresAt?: string;
  tags?: string[];
}

/**
 * Policy Update Request
 * DTO for updating an existing policy
 */
export interface UpdatePolicyRequest {
  name?: string;
  description?: string;
  status?: PolicyStatus;
  effect?: PolicyEffect;
  subjectIds?: string[];
  resourceIds?: string[];
  actions?: PolicyAction[];
  conditions?: PolicyCondition;
  priority?: number;
  expiresAt?: string;
  tags?: string[];
}

/**
 * Policy Query Filters
 * For filtering policy lists
 */
export interface PolicyFilters {
  status?: PolicyStatus[];
  effect?: PolicyEffect[];
  subjectType?: PolicySubjectType[];
  subjectId?: string;
  resourceType?: PolicyResourceType[];
  resourceId?: string;
  actions?: PolicyAction[];
  tags?: string[];
  includeExpired?: boolean;
}

/**
 * Policy List Response
 * Paginated response for policy list API
 */
export interface PolicyListResponse {
  policies: PolicySummary[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Policy Evaluation Request
 * Request to check if a user has permission
 */
export interface PolicyEvaluationRequest {
  userId: string;
  action: PolicyAction;
  resourceType: PolicyResourceType;
  resourceId?: string;
  context?: {
    ipAddress?: string;
    timestamp?: string;
    environment?: string;
    [key: string]: unknown;
  };
}

/**
 * Policy Evaluation Result
 * Response from policy evaluation
 */
export interface PolicyEvaluationResult {
  allowed: boolean;
  reason?: string;
  matchedPolicies?: string[]; // Policy IDs that matched
  denyPolicies?: string[];    // Policy IDs that denied access
  effectivePermissions?: PolicyAction[];
}

/**
 * Policy Assignment
 * Links a policy to subjects (users/roles/groups)
 */
export interface PolicyAssignment {
  policyId: string;
  policyName: string;
  subjectType: PolicySubjectType;
  subjectId: string;
  subjectName?: string;
  assignedBy: string;
  assignedAt: string;
  expiresAt?: string;
}

/**
 * User Permissions Summary
 * Aggregated view of a user's effective permissions
 */
export interface UserPermissions {
  userId: string;
  username: string;
  effectivePolicies: Policy[];
  permissionsByResource: Record<PolicyResourceType, PolicyAction[]>;
  isAdmin: boolean;
  lastEvaluatedAt: string;
}
