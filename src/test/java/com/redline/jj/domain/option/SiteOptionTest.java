package com.redline.jj.domain.option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SiteOptionTest {

    @Test
    void inStock이_false에서_true로_바뀔때만_true를_반환한다() {
        SiteOption option = SiteOption.builder()
            .optionName("S")
            .price(99000)
            .build();

        boolean result = option.updateSnapshot(true, 99000);

        assertThat(result).isTrue();
        assertThat(option.isInStock()).isTrue();
        assertThat(option.getPrice()).isEqualTo(99000);
    }

    @Test
    void 이미_inStock인_상태에서_true_유지시_false를_반환한다() {
        SiteOption option = SiteOption.builder()
            .optionName("M")
            .inStock(true)
            .price(99000)
            .build();

        boolean result = option.updateSnapshot(true, 99000);

        assertThat(result).isFalse();
        assertThat(option.isInStock()).isTrue();
    }

    @Test
    void 가격만_변경될때_false를_반환하고_가격은_갱신된다() {
        SiteOption option = SiteOption.builder()
            .optionName("M")
            .inStock(true)
            .price(99000)
            .build();

        boolean result = option.updateSnapshot(true, 89000);

        assertThat(result).isFalse();
        assertThat(option.getPrice()).isEqualTo(89000);
    }

    @Test
    void true에서_false로_전환시_false를_반환하고_상태는_갱신된다() {
        SiteOption option = SiteOption.builder()
            .optionName("L")
            .inStock(true)
            .price(99000)
            .build();

        boolean result = option.updateSnapshot(false, 99000);

        assertThat(result).isFalse();
        assertThat(option.isInStock()).isFalse();
    }

    @Test
    void false에서_false로_유지시_false를_반환한다() {
        SiteOption option = SiteOption.builder()
            .optionName("XL")
            .price(99000)
            .build();

        boolean result = option.updateSnapshot(false, 99000);

        assertThat(result).isFalse();
        assertThat(option.isInStock()).isFalse();
    }
}
