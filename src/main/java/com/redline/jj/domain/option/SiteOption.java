package com.redline.jj.domain.option;

import com.redline.jj.domain.common.BaseEntity;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.site.Site;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_site_option",
    uniqueConstraints = @UniqueConstraint(name = "uk_site_option", columnNames = {"model_idx", "site_idx", "option_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SiteOption extends BaseEntity {

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
    private String optionName;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private boolean inStock = false;

    @Column(nullable = false)
    private int price;

    // false→true 전환(재입고) 시에만 true 반환, 상태·가격은 항상 갱신
    public boolean updateSnapshot(boolean newInStock, int newPrice) {
        boolean isRestock = !this.inStock && newInStock;
        this.inStock = newInStock;
        this.price = newPrice;
        return isRestock;
    }
}
