package com.redline.jj.domain.brand;

import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_brand_alias",
    uniqueConstraints = @UniqueConstraint(name = "uk_brand_alias", columnNames = {"brand_idx", "alias_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BrandAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_idx", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private String aliasName;
}
