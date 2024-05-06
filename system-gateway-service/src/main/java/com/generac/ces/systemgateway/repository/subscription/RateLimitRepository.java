package com.generac.ces.systemgateway.repository.subscription;

import com.generac.ces.systemgateway.entity.subscription.RateLimit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {

    List<RateLimit> findAll();
}
