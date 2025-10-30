/**
 * Release Type Definitions
 *
 * Represents application release/deployment entities in the Deploy Platform.
 * Used for managing release versions, statuses, and metadata.
 */

/**
 * Release Status Enum
 * Defines the lifecycle states of a release
 */
export type ReleaseStatus =
  | 'draft'        // Initial creation state
  | 'pending'      // Awaiting approval
  | 'approved'     // Approved for deployment
  | 'deploying'    // Currently being deployed
  | 'deployed'     // Successfully deployed
  | 'failed'       // Deployment failed
  | 'rolled_back'  // Rolled back to previous version
  | 'archived';    // Archived/deprecated

/**
 * Release Priority Level
 */
export type ReleasePriority = 'low' | 'medium' | 'high' | 'critical';

/**
 * Release Environment
 */
export type ReleaseEnvironment = 'development' | 'testing' | 'staging' | 'production';

/**
 * Release Entity
 * Main interface representing a deployment release
 */
export interface Release {
  /** Unique identifier */
  id: string;

  /** Release version (semantic versioning recommended, e.g., "1.2.3") */
  version: string;

  /** Human-readable release name/title */
  name: string;

  /** Detailed description of changes in this release */
  description?: string;

  /** Current status of the release */
  status: ReleaseStatus;

  /** Priority level for deployment scheduling */
  priority: ReleasePriority;

  /** Target deployment environment */
  environment: ReleaseEnvironment;

  /** Application ID this release belongs to */
  applicationId: string;

  /** Application name (denormalized for display) */
  applicationName?: string;

  /** User ID who created this release */
  createdBy: string;

  /** Username who created this release (denormalized) */
  createdByUsername?: string;

  /** User ID who approved this release (if applicable) */
  approvedBy?: string;

  /** Username who approved (denormalized) */
  approvedByUsername?: string;

  /** ISO 8601 timestamp of creation */
  createdAt: string;

  /** ISO 8601 timestamp of last update */
  updatedAt: string;

  /** ISO 8601 timestamp of deployment (if deployed) */
  deployedAt?: string;

  /** Release notes/changelog (Markdown supported) */
  releaseNotes?: string;

  /** Git commit SHA or tag associated with this release */
  gitCommit?: string;

  /** Build number or CI job ID */
  buildNumber?: string;

  /** Artifact URLs (JAR files, Docker images, etc.) */
  artifacts?: ReleaseArtifact[];

  /** Deployment configuration overrides */
  config?: Record<string, unknown>;

  /** Tags for categorization/filtering */
  tags?: string[];
}

/**
 * Release Artifact
 * Represents a deployable artifact (JAR, WAR, Docker image, etc.)
 */
export interface ReleaseArtifact {
  /** Artifact unique ID */
  id: string;

  /** Artifact type */
  type: 'jar' | 'war' | 'docker' | 'binary' | 'other';

  /** Artifact filename */
  filename: string;

  /** Download/access URL */
  url: string;

  /** File size in bytes */
  size: number;

  /** MD5/SHA256 checksum for integrity verification */
  checksum?: string;

  /** Upload timestamp */
  uploadedAt: string;
}

/**
 * Release Summary (for list views)
 * Lightweight version of Release for list/grid displays
 */
export interface ReleaseSummary {
  id: string;
  version: string;
  name: string;
  status: ReleaseStatus;
  environment: ReleaseEnvironment;
  applicationName: string;
  createdAt: string;
  deployedAt?: string;
}

/**
 * Release Creation Request
 * DTO for creating a new release
 */
export interface CreateReleaseRequest {
  version: string;
  name: string;
  description?: string;
  priority: ReleasePriority;
  environment: ReleaseEnvironment;
  applicationId: string;
  releaseNotes?: string;
  gitCommit?: string;
  buildNumber?: string;
  config?: Record<string, unknown>;
  tags?: string[];
}

/**
 * Release Update Request
 * DTO for updating an existing release
 */
export interface UpdateReleaseRequest {
  name?: string;
  description?: string;
  status?: ReleaseStatus;
  priority?: ReleasePriority;
  releaseNotes?: string;
  config?: Record<string, unknown>;
  tags?: string[];
}

/**
 * Release Query Filters
 * For filtering release lists
 */
export interface ReleaseFilters {
  status?: ReleaseStatus[];
  environment?: ReleaseEnvironment[];
  applicationId?: string;
  createdBy?: string;
  priority?: ReleasePriority[];
  tags?: string[];
  fromDate?: string; // ISO 8601
  toDate?: string;   // ISO 8601
}

/**
 * Release List Response
 * Paginated response for release list API
 */
export interface ReleaseListResponse {
  releases: ReleaseSummary[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Release Deployment Result
 * Response from deployment operation
 */
export interface ReleaseDeploymentResult {
  releaseId: string;
  status: 'success' | 'failed' | 'in_progress';
  message: string;
  deployedAt?: string;
  logs?: string[];
  errorDetails?: {
    code: string;
    message: string;
    stack?: string;
  };
}
