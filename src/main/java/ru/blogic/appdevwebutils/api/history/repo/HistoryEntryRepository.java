package ru.blogic.appdevwebutils.api.history.repo;

import io.vavr.collection.List;
import org.hibernate.query.spi.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HistoryEntryRepository extends CrudRepository<HistoryEntry, UUID> {
    List<HistoryEntry> findByServerIdOrderByDate(int serverId);
    int countByServerId(int serverId);
}
