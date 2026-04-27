package com.redline.jj.domain.option;

import com.redline.jj.domain.common.BaseEntity;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.site.Site;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_site_option",
    uniqueConstraints = @UniqueConstraint(name = "uk_site_model_option", columnNames = {"site_id", "model_id", "option_label"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SiteOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(name = "option_label", nullable = false)
    private String optionLabel;

    @Column(nullable = false)
    private String url;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private boolean inStock = false;

    @Column
    private Integer price;

    @Column(nullable = false)
    private LocalDateTime lastCapturedAt;

    // false→true 전환(재입고) 시에만 true 반환, 상태·가격은 항상 갱신 (newPrice null 허용 - 가격 미표시 사이트 대응)
    public boolean updateSnapshot(boolean newInStock, Integer newPrice) {
        boolean isRestock = !this.inStock && newInStock;
        this.inStock = newInStock;
        this.price = newPrice;
        return isRestock;
    }
}
