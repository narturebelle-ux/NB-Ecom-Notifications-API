package com.narturebelle.mails.repository;

import com.narturebelle.mails.model.Reminder;
import com.narturebelle.mails.model.ReminderSourceType;
import com.narturebelle.mails.model.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReminderStatus status);

    List<Reminder> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndSourceTypeAndSourceIdAndDedupeKey(Long userId, ReminderSourceType sourceType, Long sourceId, String dedupeKey);

    Optional<Reminder> findByUserIdAndSourceTypeAndSourceIdAndStatus(
            Long userId, ReminderSourceType sourceType, Long sourceId, ReminderStatus status);
}
