package com.finalproject.controller.common;
 
import com.finalproject.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalCartAdvice {

    private final CartService cartService;

    @ModelAttribute("cartItemCount")
    public int populateCartItemCount(Principal principal) {
        if (principal != null) {
            try {
                return cartService.getCartItemCount(principal.getName());
            } catch (Exception ex) {
                return 0;
            }
        }
        return 0;
    }
}
