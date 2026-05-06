package com.redline.jj.domain.model;

import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_model",
    uniqueConstraints = @UniqueConstraint(name = "uk_brand_model", columnNames = {"brand_id", "model_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Model extends BaseEntity {

    public enum ModelType {
        DENIM_PANTS("데님 팬츠"),
        DENIM_JACKET("데님 재킷");

        private final String label;

        ModelType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private String modelName;

    @Column
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ModelType modelType;

    public void updateImageUrlIfAbsent(String imageUrl) {
        if (this.imageUrl == null && imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        }
    }

    public void updateModelTypeIfAbsent(ModelType modelType) {
        if (this.modelType == null && modelType != null) {
            this.modelType = modelType;
        }
    }
}
