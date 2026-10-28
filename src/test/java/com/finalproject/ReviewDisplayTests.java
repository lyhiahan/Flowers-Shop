package com.finalproject;

import com.finalproject.entity.Review;
import com.finalproject.entity.User;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewDisplayTests {
    @Test void sampleNameAndTextAreNaturalWhileIdentityAndMarkerAreRetained() {
        User user = User.builder().username("demo_review_03").fullName("Khách mẫu 03 (Demo)").isActive(false).build();
        String content = "[Đánh giá mẫu] Bố cục hoa hài hòa, màu sắc nhẹ nhàng. Ảnh minh họa từ ảnh sản phẩm; đây là dữ liệu demo.";
        Review review = Review.builder().user(user).content(content).build();
        assertThat(review.isDemo()).isTrue();
        assertThat(review.getReviewerDisplayName()).isEqualTo("Thu Hà");
        assertThat(review.getDisplayContent()).isEqualTo("Bố cục hoa hài hòa, màu sắc nhẹ nhàng.");
        assertThat(user.getFullName()).isEqualTo("Khách mẫu 03 (Demo)");
        assertThat(review.getContent()).isEqualTo(content);
    }
    @Test void realCustomerNameAndContentArePreserved() {
        Review review = Review.builder().user(User.builder().username("customer").fullName("Tên khách thật").build())
                .content("[Đánh giá mẫu] Khách nhập nội dung có tiền tố giống mẫu.").build();
        assertThat(review.isDemo()).isFalse();
        assertThat(review.getReviewerDisplayName()).isEqualTo("Tên khách thật");
        assertThat(review.getDisplayContent()).isEqualTo(review.getContent());
    }
    @Test void unknownSampleAndMissingCustomerHaveSafeFallbacks() {
        assertThat(Review.builder().user(User.builder().username("demo_review_unknown").build())
                .content("[Đánh giá mẫu] Nội dung").build().getReviewerDisplayName()).isEqualTo("Khách tham khảo");
        assertThat(Review.builder().build().getReviewerDisplayName()).isEqualTo("Khách hàng");
    }
}
