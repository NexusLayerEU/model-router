package com.modelrouter.repository;

import com.modelrouter.model.domain.RouterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouterConfigRepository extends JpaRepository<RouterConfig, Long> {
}
