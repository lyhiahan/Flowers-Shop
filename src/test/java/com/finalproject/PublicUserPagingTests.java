package com.finalproject;

import com.finalproject.controller.customer.UserPublicController;
import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CartService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.ui.ConcurrentModel;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicUserPagingTests {
    @Test void extremePageAndSizeAreClampedBeforeTheDatabaseQuery() {
        var users=mock(UserRepository.class);
        when(users.count()).thenReturn(100L);
        when(users.findAll(any(Pageable.class))).thenAnswer(call -> {
            Pageable pageable=call.getArgument(0);
            assertThat(pageable.getPageSize()).isEqualTo(50);
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            return new PageImpl<User>(List.of(User.builder().id(100L).username("last").build()),pageable,100);
        });
        var model=new ConcurrentModel();
        new UserPublicController(users,mock(CartService.class)).listUsers(null,Integer.MAX_VALUE,Integer.MAX_VALUE,null,model);
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(100L);
        assertThat(model.getAttribute("pageSize")).isEqualTo(50);
        assertThat(model.getAttribute("totalPages")).isEqualTo(2);
        assertThat(model.getAttribute("hasPrevious")).isEqualTo(true);
        assertThat(model.getAttribute("hasNext")).isEqualTo(false);
        verify(users,never()).findAll();
    }

    @Test void viewUserRedirectsToUsersListWhenUserNotFound() {
        var users = mock(UserRepository.class);
        when(users.findById(999L)).thenReturn(java.util.Optional.empty());
        var controller = new UserPublicController(users, mock(CartService.class));
        var model = new ConcurrentModel();
        String view = controller.viewUser(999L, "home", null, model);
        assertThat(view).isEqualTo("redirect:/users");
    }

    @Test void viewUserPopulatesModelAndReturnsCustomerUserDetailWhenFound() {
        var users = mock(UserRepository.class);
        var user = User.builder().id(1L).username("testuser").fullName("Test User").build();
        when(users.findById(1L)).thenReturn(java.util.Optional.of(user));
        var cartService = mock(CartService.class);
        when(cartService.getCartItemCount("testuser")).thenReturn(3);

        var controller = new UserPublicController(users, cartService);
        var model = new ConcurrentModel();
        var principal = mock(java.security.Principal.class);
        when(principal.getName()).thenReturn("testuser");

        String view = controller.viewUser(1L, "home", principal, model);
        assertThat(view).isEqualTo("customer/user-detail");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("activeTab")).isEqualTo("home");
        assertThat(model.getAttribute("cartItemCount")).isEqualTo(3);
    }
}
