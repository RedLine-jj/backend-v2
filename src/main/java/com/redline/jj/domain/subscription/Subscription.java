package com.redline.jj.domain.subscription;

import com.redline.jj.domain.common.BaseEntity;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_subscription",
    uniqueConstraints = @UniqueConstraint(name = "uk_subscription", columnNames = {"user_id", "model_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;
}
