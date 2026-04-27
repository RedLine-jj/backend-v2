package com.redline.jj.domain.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ModelAliasRepository extends JpaRepository<ModelAlias, Long> {
    Optional<ModelAlias> findBySite_IdxAndSiteModelName(Long siteIdx, String siteModelName);
}
