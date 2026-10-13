package com.finalproject.controller.admin;

import com.finalproject.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller @RequestMapping("/admin/chat") @RequiredArgsConstructor
public class AdminChatController {
    private final ChatService chat;
    @GetMapping
    public String workbench(Principal principal,Model model) {
        if(!"ADMIN".equals(chat.context(principal.getName()).role())) throw new org.springframework.security.access.AccessDeniedException("Chỉ admin được mở trung tâm hội thoại.");
        model.addAttribute("chatWorkbench",true);
        return "admin/chat";
    }
    @GetMapping("/access-logs")
    public String logs(Principal principal,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("logs",chat.accessLogs(principal.getName(),page));
        return "admin/chat-access-logs";
    }
}
