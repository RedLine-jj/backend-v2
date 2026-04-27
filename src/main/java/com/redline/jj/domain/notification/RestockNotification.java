package com.redline.jj.domain.notification;

import com.redline.jj.domain.common.BaseEntity;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_restock_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RestockNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_idx", nullable = false)
    private Model model;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    public void markAsRead() {
        this.read = true;
    }
}
