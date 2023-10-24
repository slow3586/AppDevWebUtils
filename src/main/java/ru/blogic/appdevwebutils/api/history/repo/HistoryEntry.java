package ru.blogic.appdevwebutils.api.history.repo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.print.attribute.standard.Severity;
import java.util.Date;
import java.util.UUID;

/**
 * Запись истории операций на серверах приложений.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "HISTORY_ENTRIES")
public class HistoryEntry {
    /** ID записи. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "UUID", updatable = false)
    @Setter(value = AccessLevel.PROTECTED)
    UUID uuid;
    /** ID сервера приложения. */
    @Column(name = "SERVER_ID")
    int serverId;
    /** Текст записи. */
    @Column(name = "TEXT")
    String text;
    /** Важность записи. */
    @Column(name = "SEVERITY")
    Severity severity;
    /** Имя пользователя, связанного с операцией. */
    @Column(name = "USERNAME")
    String username;
    /** Время записи */
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE", updatable = false)
    @Setter(value = AccessLevel.PROTECTED)
    Date date;

    /** Важность операции */
    public enum Severity {
        CRIT,
        INFO,
        TRACE
    }
}
