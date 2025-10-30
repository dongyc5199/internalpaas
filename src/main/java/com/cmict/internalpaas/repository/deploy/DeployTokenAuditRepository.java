package com.cmict.internalpaas.repository.deploy;

import com.cmict.internalpaas.model.deploy.DeployTokenAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeployTokenAuditRepository extends JpaRepository<DeployTokenAudit, Long> {
}
