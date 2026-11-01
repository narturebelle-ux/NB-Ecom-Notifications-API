package com.narturebelle.mails.repository;

import com.narturebelle.mails.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, String> {
}
