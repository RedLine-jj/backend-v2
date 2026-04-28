package com.redline.jj.domain.model;

import com.redline.jj.domain.site.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ModelAliasRepository extends JpaRepository<ModelAlias, Long> {
    Optional<ModelAlias> findBySiteAndAliasName(Site site, String aliasName);
}
