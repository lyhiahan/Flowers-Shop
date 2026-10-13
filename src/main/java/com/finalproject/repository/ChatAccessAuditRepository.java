package com.finalproject.repository;

import com.finalproject.entity.ChatAccessAudit;
import org.springframework.data.jpa.repository.*;

public interface ChatAccessAuditRepository extends JpaRepository<ChatAccessAudit,Long>, JpaSpecificationExecutor<ChatAccessAudit> {
}
