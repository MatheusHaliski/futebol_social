package com.footballai.social.repository.cassandra;

import com.footballai.social.model.cassandra.FeedEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

/** Leitura por partição (user_id, feed_window) com paging state do driver (cursor). */
public interface FeedEntryRepository extends CassandraRepository<FeedEntry, FeedEntry.Key> {
    Slice<FeedEntry> findByKeyUserIdAndKeyFeedWindow(UUID userId, String feedWindow, Pageable pageable);
}
