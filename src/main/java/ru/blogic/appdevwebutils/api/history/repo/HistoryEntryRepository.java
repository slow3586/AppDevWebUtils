package ru.blogic.appdevwebutils.api.history.repo;

import io.vavr.collection.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий записей истории операций на серверах приложений.
 */
@Repository
public interface HistoryEntryRepository extends CrudRepository<HistoryEntry, UUID> {
    List<HistoryEntry> findByServerIdOrderByDate(int serverId);
    int countByServerId(int serverId);
}
