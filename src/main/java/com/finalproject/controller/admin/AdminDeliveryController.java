package com.finalproject.controller.admin;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDeliveryController {
    private final DeliveryRepository deliveryRepository;
    private final StoreRepository storeRepository;
    private final ShippingTariffRepository shippingTariffRepository;

    // --- 3. Quản lý Giao hàng & Vận chuyển (Deliveries & Shipping Tariffs) ---
    @GetMapping({"/deliveries", "/delivery", "/don-vi-van-chuyen", "/shipping-manage"})
    public String listDeliveries(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tab", defaultValue = "units") String activeTab,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "service", required = false) String service,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Delivery> allDeliveries = deliveryRepository.findAllByOrderByIdAsc();
            java.util.List<Delivery> filtered = allDeliveries;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(d ->
                        (d.getName() != null && d.getName().toLowerCase().contains(kw))
                        || String.valueOf(d.getId()).contains(kw)
                        || (d.getDescription() != null && d.getDescription().toLowerCase().contains(kw))
                        || (d.getPhone() != null && d.getPhone().toLowerCase().contains(kw))
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Delivery> pagedDeliveries = filtered.subList(fromIndex, toIndex);

            model.addAttribute("deliveries", pagedDeliveries);
            model.addAttribute("allDeliveries", allDeliveries);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);

            // Tab navigation & Tariff data
            model.addAttribute("activeTab", activeTab);
            model.addAttribute("tariffs", shippingTariffRepository.searchTariffs(storeId, service));
            model.addAttribute("stores", storeRepository.findAll());
            model.addAttribute("currentStoreId", storeId);
            model.addAttribute("currentService", service != null ? service : "ALL");
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu quản lý giao hàng. Vui lòng thử lại!");
            model.addAttribute("deliveries", java.util.Collections.emptyList());
            model.addAttribute("allDeliveries", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
            model.addAttribute("activeTab", activeTab);
            model.addAttribute("tariffs", java.util.Collections.emptyList());
            model.addAttribute("stores", java.util.Collections.emptyList());
        }
        return "admin/deliveries";
    }


    /**
     * Bước 5 & 6: Chọn nút New delivery unit để thêm mới -> Hiển thị form thêm mới
     */
    @GetMapping("/deliveries/create")
    public String createDeliveryForm(Model model) {
        model.addAttribute("delivery", Delivery.builder().phone("").price(BigDecimal.ZERO).isActive(true).isDeleted(false).build());
        model.addAttribute("isEdit", false);
        return "admin/delivery-form";
    }

    /**
     * Alternative 5a & 6a: Chọn nút Edit để chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @GetMapping("/deliveries/edit/{id}")
    public String editDeliveryForm(@PathVariable("id") Long id, Model model) {
        Delivery d = deliveryRepository.findById(id).orElse(null);
        if (d == null) {
            return "redirect:/admin/deliveries";
        }
        model.addAttribute("delivery", d);
        model.addAttribute("isEdit", true);
        return "admin/delivery-form";
    }

    /**
     * Bước 7 - 11: Nhập thông tin, Submit, Confirm -> Thêm mới thành công
     */
    @PostMapping("/deliveries/add")
    public String addDelivery(@RequestParam("name") String name,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "price", defaultValue = "0") BigDecimal price,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes ra) {
        try {
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập tên đơn vị vận chuyển!");
                return "redirect:/admin/deliveries";
            }
            Delivery d = Delivery.builder()
                    .name(cleanName)
                    .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                    .price(price != null ? price : BigDecimal.ZERO)
                    .description(description)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Thêm đơn vị vận chuyển [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5a: Cập nhật đơn vị vận chuyển
     */
    @PostMapping("/deliveries/update/{id}")
    public String updateDelivery(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "phone", required = false) String phone,
                                 @RequestParam(value = "price", defaultValue = "0") BigDecimal price,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "isActive", required = false) Boolean isActive,
                                 RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElse(null);
            if (d == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy đơn vị vận chuyển #" + id);
                return "redirect:/admin/deliveries";
            }
            String cleanName = name != null ? name.trim() : d.getName();
            d.setName(cleanName);
            if (phone != null) {
                d.setPhone(phone.trim());
            }
            if (price != null) {
                d.setPrice(price);
            }
            d.setDescription(description);
            if (isActive != null) {
                d.setIsActive(isActive);
            }
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Cập nhật đơn vị vận chuyển [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/toggle/{id}")
    public String toggleDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(d.getIsActive());
        d.setIsActive(newActive);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Đã " + (newActive ? "bật áp dụng" : "tạm dừng") + " đơn vị vận chuyển [" + d.getName() + "].");
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5-8b: Xóa đơn vị vận chuyển (soft delete)
     */
    @PostMapping("/deliveries/delete/{id}")
    public String deleteDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElseThrow();
            d.setIsDeleted(true);
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Xóa đơn vị vận chuyển [" + d.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5-8c: Khôi phục đơn vị vận chuyển
     */
    @PostMapping("/deliveries/restore/{id}")
    public String restoreDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElseThrow();
            d.setIsDeleted(false);
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Khôi phục đơn vị vận chuyển [" + d.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi khôi phục đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/reset-defaults")
    public String resetDefaultDeliveries(RedirectAttributes ra) {
        record DefDeliv(String name, String phone, BigDecimal price, String desc) {}
        java.util.List<DefDeliv> defaults = java.util.List.of(
            new DefDeliv("Giao hàng nội bộ", "028 7108 8810", BigDecimal.ZERO, "Đội ngũ giao hoa nội bộ tiệm, trang bị thùng giữ ẩm chuyên dụng và che chắn hoa cẩn thận."),
            new DefDeliv("GrabExpress", "1900 633 677", BigDecimal.ZERO, "Đối tác GrabExpress chuyên giao hàng hỏa tốc xe máy có lồng bảo hộ cánh hoa."),
            new DefDeliv("Ahamove", "1900 545 411", BigDecimal.ZERO, "Đối tác Ahamove giao siêu tốc, chuyên đơn hàng hoa tươi và bánh kem nội thành.")
        );
        for (DefDeliv d : defaults) {
            java.util.Optional<Delivery> existing = deliveryRepository.findAll().stream()
                .filter(x -> x.getName().equalsIgnoreCase(d.name()) 
                          || (d.name().contains("nội bộ") && x.getName().toLowerCase().contains("nội bộ"))
                          || (d.name().contains("Grab") && x.getName().toLowerCase().contains("grab"))
                          || (d.name().contains("Ahamove") && x.getName().toLowerCase().contains("ahamove")))
                .findFirst();
            if (existing.isPresent()) {
                Delivery del = existing.get();
                del.setName(d.name());
                del.setPhone(d.phone());
                del.setPrice(d.price());
                del.setDescription(d.desc());
                del.setIsActive(true);
                del.setIsDeleted(false);
                deliveryRepository.save(del);
            } else {
                deliveryRepository.save(Delivery.builder()
                        .name(d.name())
                        .phone(d.phone())
                        .price(d.price())
                        .description(d.desc())
                        .isActive(true)
                        .isDeleted(false)
                        .build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục danh sách 3 đơn vị vận chuyển chuẩn thành công!");
        return "redirect:/admin/deliveries";
    }

    private void validateTariff(String area, String service, BigDecimal fee, BigDecimal surcharge, Long storeId, Long deliveryId) {
        if (area == null || area.isBlank() || area.trim().length() > 150 || service == null || service.isBlank() || service.trim().length() > 100)
            throw new IllegalArgumentException("Vui lòng nhập khu vực và dịch vụ hợp lệ.");
        if (fee == null || fee.signum() < 0 || surcharge == null || surcharge.signum() < 0
                || fee.compareTo(new BigDecimal("1000000000")) > 0 || surcharge.compareTo(new BigDecimal("1000000000")) > 0
                || fee.stripTrailingZeros().scale() > 2 || surcharge.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Phí và phụ phí phải từ 0 đến 1.000.000.000 đồng.");
        if (storeId != null && (storeId <= 0 || !storeRepository.existsById(storeId)))
            throw new IllegalArgumentException("Chi nhánh không tồn tại.");
        if (deliveryId != null && (deliveryId <= 0 || !deliveryRepository.existsById(deliveryId)))
            throw new IllegalArgumentException("Đơn vị giao hàng không tồn tại.");
    }

    // --- Biểu phí giao hàng (Shipping Tariffs) ---
    @PostMapping("/deliveries/tariffs/add")
    public String addTariff(@RequestParam(value = "storeId", required = false) Long storeId,
                            @RequestParam("destinationArea") String destinationArea,
                            @RequestParam("serviceName") String serviceName,
                            @RequestParam(value = "deliveryId", required = false) Long deliveryId,
                            @RequestParam("baseFee") BigDecimal baseFee,
                            @RequestParam(value = "bulkySurcharge", defaultValue = "0") BigDecimal bulkySurcharge,
                            @RequestParam(value = "estimatedTime", required = false) String estimatedTime,
                            @RequestParam(value = "note", required = false) String note,
                            RedirectAttributes ra) {
        try {
            validateTariff(destinationArea, serviceName, baseFee, bulkySurcharge, storeId, deliveryId);
            Store store = (storeId != null && storeId > 0) ? storeRepository.findById(storeId).orElse(null) : null;
            Delivery delivery = (deliveryId != null && deliveryId > 0) ? deliveryRepository.findById(deliveryId).orElse(null) : null;
            ShippingTariff tariff = ShippingTariff.builder()
                    .store(store)
                    .destinationArea(destinationArea.trim())
                    .serviceName(serviceName.trim())
                    .delivery(delivery)
                    .baseFee(baseFee != null ? baseFee : BigDecimal.ZERO)
                    .bulkySurcharge(bulkySurcharge != null ? bulkySurcharge : BigDecimal.ZERO)
                    .estimatedTime(estimatedTime)
                    .note(note)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            shippingTariffRepository.save(tariff);
            ra.addFlashAttribute("successMessage", "Thêm biểu phí giao hàng [" + serviceName + " - " + destinationArea + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm biểu phí: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries?tab=tariffs";
    }

    @PostMapping("/deliveries/tariffs/update/{id}")
    public String updateTariff(@PathVariable("id") Long id,
                               @RequestParam(value = "storeId", required = false) Long storeId,
                               @RequestParam("destinationArea") String destinationArea,
                               @RequestParam("serviceName") String serviceName,
                               @RequestParam(value = "deliveryId", required = false) Long deliveryId,
                               @RequestParam("baseFee") BigDecimal baseFee,
                               @RequestParam(value = "bulkySurcharge", defaultValue = "0") BigDecimal bulkySurcharge,
                               @RequestParam(value = "estimatedTime", required = false) String estimatedTime,
                               @RequestParam(value = "note", required = false) String note,
                               RedirectAttributes ra) {
        try {
            validateTariff(destinationArea, serviceName, baseFee, bulkySurcharge, storeId, deliveryId);
            ShippingTariff tariff = shippingTariffRepository.findById(id).orElseThrow();
            Store store = (storeId != null && storeId > 0) ? storeRepository.findById(storeId).orElse(null) : null;
            Delivery delivery = (deliveryId != null && deliveryId > 0) ? deliveryRepository.findById(deliveryId).orElse(null) : null;
            tariff.setStore(store);
            tariff.setDestinationArea(destinationArea.trim());
            tariff.setServiceName(serviceName.trim());
            tariff.setDelivery(delivery);
            tariff.setBaseFee(baseFee != null ? baseFee : BigDecimal.ZERO);
            tariff.setBulkySurcharge(bulkySurcharge != null ? bulkySurcharge : BigDecimal.ZERO);
            tariff.setEstimatedTime(estimatedTime);
            tariff.setNote(note);
            shippingTariffRepository.save(tariff);
            ra.addFlashAttribute("successMessage", "Cập nhật biểu phí giao hàng #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật biểu phí: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries?tab=tariffs";
    }

    @PostMapping("/deliveries/tariffs/toggle/{id}")
    public String toggleTariff(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            ShippingTariff tariff = shippingTariffRepository.findById(id).orElseThrow();
            boolean newActive = !Boolean.TRUE.equals(tariff.getIsActive());
            tariff.setIsActive(newActive);
            shippingTariffRepository.save(tariff);
            ra.addFlashAttribute("successMessage", (newActive ? "Đã bật áp dụng" : "Đã tạm dừng") + " biểu phí #" + id + ".");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi đổi trạng thái: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries?tab=tariffs";
    }

    @PostMapping("/deliveries/tariffs/delete/{id}")
    public String deleteTariff(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            ShippingTariff tariff = shippingTariffRepository.findById(id).orElseThrow();
            tariff.setIsDeleted(true);
            shippingTariffRepository.save(tariff);
            ra.addFlashAttribute("successMessage", "Đã xóa biểu phí giao hàng #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa biểu phí: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries?tab=tariffs";
    }

}

