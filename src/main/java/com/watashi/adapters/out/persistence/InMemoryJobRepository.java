package com.watashi.adapters.out.persistence;

import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.ports.out.JobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJobRepository implements JobRepository {

    private final Map<String, JobOpportunity> store = new ConcurrentHashMap<>();

    @Override
    public void save(JobOpportunity job) {
        if (job != null && job.id() != null) {
            store.put(job.id(), job);
        }
    }

    @Override
    public List<JobOpportunity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<JobOpportunity> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }
}
