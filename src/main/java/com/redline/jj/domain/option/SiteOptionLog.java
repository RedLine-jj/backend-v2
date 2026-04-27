package com.redline.jj.domain.option;

import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "option_label", nullable = false)
    private String optionLabel;

    @Column
    private Integer price;

    @Column(name = "status", nullable = false)
    private boolean inStock;
}
