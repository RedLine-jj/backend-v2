package com.redline.jj.domain.model;

import com.redline.jj.domain.model.Model.ModelType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ModelRepository extends JpaRepository<Model, Long> {

    // 필터 없음, cursor 없음
    List<Model> findAllByOrderByIdxDesc(Pageable pageable);

    // 필터 없음, cursor 있음
    List<Model> findByIdxLessThanOrderByIdxDesc(Long cursorIdx, Pageable pageable);

    // 브랜드+타입 필터, cursor 없음
    List<Model> findByBrand_IdxInAndModelTypeInOrderByIdxDesc(
        Collection<Long> brandIdxList,
        Collection<ModelType> types,
        Pageable pageable
    );

    // 브랜드+타입 필터, cursor 있음
    List<Model> findByIdxLessThanAndBrand_IdxInAndModelTypeInOrderByIdxDesc(
        Long cursorIdx,
        Collection<Long> brandIdxList,
        Collection<ModelType> types,
        Pageable pageable
    );
}
