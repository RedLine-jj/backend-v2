package com.redline.jj.domain.brand;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BrandAliasRepository extends JpaRepository<BrandAlias, Long> {
    Optional<BrandAlias> findByAliasName(String aliasName);
}
