package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import com.finalproject.service.DemoBranchSeeder;
import com.finalproject.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DemoIdentityTests {
    @Test void fixturesUseDistinctPersonalNamesAndReuseAccountsByStableEmailAfterRename() {
        UserRepository users=mock(UserRepository.class);
        StoreRepository stores=mock(StoreRepository.class);
        UserAddressRepository addresses=mock(UserAddressRepository.class);
        PasswordEncoder encoder=mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("unchanged-test-hash");
        Map<String,User> existing=new HashMap<>();
        when(users.findByEmail(anyString())).thenAnswer(call -> Optional.ofNullable(existing.get(call.getArgument(0))));
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user=call.getArgument(0); user.setId((long)existing.size()+1); existing.put(user.getEmail(),user); return user;
        });
        Map<String,Store> branches=new HashMap<>();
        when(stores.findFirstByNameOrderByIdAsc(anyString())).thenAnswer(call -> {
            String name=call.getArgument(0);
            return Optional.of(branches.computeIfAbsent(name, key -> Store.builder().id((long)branches.size()+1).name(key).build()));
        });
        when(addresses.findByUserIdOrderByIsDefaultDescCreatedAtDesc(anyLong())).thenReturn(List.of(new UserAddress()));
        DemoBranchSeeder seeder=new DemoBranchSeeder(users,stores,addresses,encoder);
        seeder.seed();
        assertThat(existing.values()).extracting(User::getUsername)
                .containsExactlyInAnyOrder("nguyenthanhminh","nguyenquanghuy","tranduclong","leminhhoa");
        assertThat(existing.get("admin@rentalshop.vn").getFullName()).isEqualTo("Nguyễn Thanh Minh");
        assertThat(existing.get("admin@rentalshop.vn").getRole()).isEqualTo(Role.ADMIN);
        User customer=existing.get("khachhang@gmail.com");
        assertThat(customer.getFullName()).isEqualTo("Lê Minh Hoa");
        customer.setUsername("minhhoa.personal");
        customer.setFullName("Tên khách tự cập nhật");
        clearInvocations(users);
        seeder.seed();
        verify(users,never()).save(any(User.class));
        assertThat(customer.getUsername()).isEqualTo("minhhoa.personal");
        assertThat(customer.getFullName()).isEqualTo("Tên khách tự cập nhật");
        assertThat(existing).hasSize(4);
    }
}
