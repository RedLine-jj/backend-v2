package com.redline.jj.domain.model;

import com.redline.jj.domain.model.Model.ModelType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ModelRepository extends JpaRepository<Model, Long> {

    // 필터 없음, cursor 없음
    List<Model> findAllByOrderByIdDesc(Pageable pageable);

    // 필터 없음, cursor 있음
    List<Model> findByIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    // 브랜드+타입 필터, cursor 없음
    List<Model> findByBrand_IdInAndModelTypeInOrderByIdDesc(
        Collection<Long> brandIdList,
        Collection<ModelType> types,
        Pageable pageable
    );

    // 브랜드+타입 필터, cursor 있음
    List<Model> findByIdLessThanAndBrand_IdInAndModelTypeInOrderByIdDesc(
        Long cursorId,
        Collection<Long> brandIdList,
        Collection<ModelType> types,
        Pageable pageable
    );
}
