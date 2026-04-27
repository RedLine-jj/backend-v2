package com.redline.jj.domain.model;

import com.redline.jj.domain.common.BaseEntity;
import com.redline.jj.domain.site.Site;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_model_alias",
    uniqueConstraints = @UniqueConstraint(name = "uk_model_alias", columnNames = {"site_idx", "site_model_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ModelAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_idx", nullable = false)
    private Model model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_idx", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String siteModelName;

    @Column(nullable = false)
    private int confidence;
}
