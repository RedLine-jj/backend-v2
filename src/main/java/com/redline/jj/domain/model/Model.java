package com.redline.jj.domain.model;

import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_model",
    uniqueConstraints = @UniqueConstraint(name = "uk_model", columnNames = {"brand_idx", "model_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Model extends BaseEntity {

    public enum ModelType { DENIM_PANTS, DENIM_JACKET }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_idx", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelType modelType;
}
