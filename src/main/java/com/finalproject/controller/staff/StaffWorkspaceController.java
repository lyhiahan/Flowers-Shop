package com.finalproject.controller.staff;

import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffWorkspaceController {
    private final StoreAccessService access;

    @GetMapping({"", "/"})
    public String home() { return "redirect:/staff/orders"; }

    @GetMapping("/chat")
    public String chat(Model model) {
        var store = access.selectedStore();
        if (store != null) access.requireStore(store);
        model.addAttribute("staffActive", "chat");
        model.addAttribute("chatWorkbench", true);
        return "staff/chat";
    }
}
