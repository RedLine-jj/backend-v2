package com.redline.jj.domain.brand;

import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_brand",
    uniqueConstraints = @UniqueConstraint(name = "uk_brand_name", columnNames = "brand_name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brandName;

    @Column
    private String brandNameKo;
}
