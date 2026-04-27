package com.redline.jj.domain.site;

import com.redline.jj.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_site",
    uniqueConstraints = @UniqueConstraint(name = "uk_site_name", columnNames = "site_name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Site extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String siteName;

    @Column
    private String siteLink;
}
