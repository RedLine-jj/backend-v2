package com.redline.jj.domain.option;

import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_site_option_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SiteOptionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_option_id", nullable = false)
    private SiteOption siteOption;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    @Column(nullable = false)
    private int price;
}
