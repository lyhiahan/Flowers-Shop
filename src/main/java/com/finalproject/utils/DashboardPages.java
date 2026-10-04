package com.finalproject.utils;

import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

/** Bound dashboard rankings to 1000 records and render 20 per page. */
public final class DashboardPages {
    private DashboardPages() {}
    public static void apply(Model model, String name) {
        if (!(model.getAttribute(name) instanceof List<?> list)) return;
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String parameter = name + "Page";
        String raw = request.getParameter(parameter);
        int page = raw == null ? 0 : Integer.parseInt(raw);
        if (page < 0) throw new IllegalArgumentException("Trang không hợp lệ.");
        int pages = Math.max(1, (Math.min(1000, list.size()) + 19) / 20);
        page = Math.min(page, pages - 1);
        int from = page * 20, to = Math.min(from + 20, Math.min(1000, list.size()));
        model.addAttribute(name, list.subList(from, to));
        model.addAttribute(name + "Pager", new Pager(page + 1, pages,
                url(parameter, Math.max(0, page - 1)), url(parameter, Math.min(pages - 1, page + 1))));
    }
    private static String url(String name, int page) {
        return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam(name, page).build().toUriString();
    }
    public record Pager(int page, int pages, String previous, String next) {}
}
