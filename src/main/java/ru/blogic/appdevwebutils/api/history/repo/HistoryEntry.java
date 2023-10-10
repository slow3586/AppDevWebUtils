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
import lombok.Data;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
@Table(name = "HISTORY_ENTRIES")
public class HistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "UUID", updatable = false)
    @Setter(value = AccessLevel.PROTECTED)
    UUID uuid;
    @Column(name = "SERVER_ID")
    int serverId;
    @Column(name = "TEXT")
    String text;
    @Column(name = "SEVERITY")
    Severity severity;
    @Column(name = "USERNAME")
    String username;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE", updatable = false)
    @Setter(value = AccessLevel.PROTECTED)
    Date date;

    public enum Severity {
        CRIT,
        INFO,
        TRACE
    }
}
