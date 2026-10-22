# Bản đồ cấu trúc và route

Ngày quét mã nguồn: 21/10/2026. Bản kê tĩnh để đối chiếu phạm vi kiểm thử; route annotation có thể có alias và còn chịu kiểm tra quyền tại SecurityConfig/service. Đây không phải kết quả chạy HTTP.

## 1. Thống kê mã nguồn

| Nhóm | Số file |
|---|---:|
| controller | 48 |
| service | 59 |
| repository | 41 |
| entity | 41 |
| dto | 9 |
| config | 22 |
| security | 1 |
| enums | 9 |
| exception | 2 |
| utils | 9 |
| scheduler | 1 |
| db | 3 |
| src/main/resources/templates | 77 |
| src/main/resources/static/js | 19 |
| src/test/java | 78 |
| src/test/js | 15 |
| src/main/resources/db/migration/sqlserver | 11 |

## 2. Controller và route annotation

Mỗi controller ghi route cấp lớp (nếu có), sau đó mapping cấp phương thức theo đúng mã nguồn. Ghép prefix cấp lớp với mapping phương thức, thử từng alias khi cần. Dòng trống ở annotation nghĩa là dùng prefix. MessageMapping là đích STOMP, không phải HTTP URL.

### AdminCategoryController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminCategoryController.java](../src/main/java/com/finalproject/controller/admin/AdminCategoryController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/categories", "/category", "/loai-san-pham"})
@GetMapping("/categories/create")
@GetMapping("/categories/edit/{id}")
@PostMapping("/categories/add")
@PostMapping("/categories/update/{id}")
@PostMapping("/categories/delete/{id}")
@PostMapping("/categories/restore/{id}")
@PostMapping("/categories/reset-defaults")
```

### AdminChatController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminChatController.java](../src/main/java/com/finalproject/controller/admin/AdminChatController.java)

```java
@RequestMapping("/admin/chat")
@GetMapping
@GetMapping("/access-logs")
```

### AdminCommissionController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminCommissionController.java](../src/main/java/com/finalproject/controller/admin/AdminCommissionController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/commissions", "/commission", "/chinh-sach-hoa-hong"})
@GetMapping("/commissions/create")
@GetMapping("/commissions/edit/{id}")
@PostMapping("/commissions/add")
@PostMapping("/commissions/update/{id}")
@PostMapping("/commissions/delete/{id}")
@PostMapping("/commissions/restore/{id}")
@PostMapping("/commissions/reset-defaults")
```

### AdminDashboardController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminDashboardController.java](../src/main/java/com/finalproject/controller/admin/AdminDashboardController.java)

```java
@RequestMapping("/admin")
@GetMapping("/dashboard/stock")
@GetMapping({"", "/dashboard"})
```

### AdminDeliveryController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminDeliveryController.java](../src/main/java/com/finalproject/controller/admin/AdminDeliveryController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/deliveries", "/delivery", "/don-vi-van-chuyen", "/shipping-manage"})
@GetMapping("/deliveries/create")
@GetMapping("/deliveries/edit/{id}")
@PostMapping("/deliveries/add")
@PostMapping("/deliveries/update/{id}")
@PostMapping("/deliveries/toggle/{id}")
@PostMapping("/deliveries/delete/{id}")
@PostMapping("/deliveries/restore/{id}")
@PostMapping("/deliveries/reset-defaults")
@PostMapping("/deliveries/tariffs/add")
@PostMapping("/deliveries/tariffs/update/{id}")
@PostMapping("/deliveries/tariffs/toggle/{id}")
@PostMapping("/deliveries/tariffs/delete/{id}")
```

### AdminOrderManagementController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminOrderManagementController.java](../src/main/java/com/finalproject/controller/admin/AdminOrderManagementController.java)

```java
@RequestMapping({"/admin/orders", "/admin/orders-manage"})
@GetMapping
@GetMapping({"/{id}", "/detail/{id}"})
@PostMapping("/update-status/{id}")
@PostMapping("/confirm-payment/{id}")
```

### AdminProductCatalogController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminProductCatalogController.java](../src/main/java/com/finalproject/controller/admin/AdminProductCatalogController.java)

```java
@RequestMapping({"/admin/products", "/admin/products-manage"})
@GetMapping
```

### AdminProductController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminProductController.java](../src/main/java/com/finalproject/controller/admin/AdminProductController.java)

```java
@RequestMapping({"/admin/products", "/admin/products-manage"})
@PostMapping("/approve/{id}")
@PostMapping("/ban/{id}")
@PostMapping("/toggle-active/{id}")
@PostMapping("/toggle-selling/{id}")
@GetMapping("/create")
@PostMapping("/create")
@GetMapping("/edit/{id}")
@PostMapping("/edit/{id}")
@PostMapping("/delete/{id}")
@PostMapping({"/seed-defaults", "/reset-defaults"})
```

### AdminStoreController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminStoreController.java](../src/main/java/com/finalproject/controller/admin/AdminStoreController.java)

```java
@RequestMapping("/admin/stores")
@GetMapping
@PostMapping("/update-level/{id}")
@PostMapping("/approve/{id}")
@PostMapping("/ban/{id}")
@PostMapping("/toggle-status/{id}")
```

### AdminStoreLevelController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminStoreLevelController.java](../src/main/java/com/finalproject/controller/admin/AdminStoreLevelController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/store-levels", "/store-level", "/cap-do-cua-hang", "/hang-chi-nhanh"})
@GetMapping("/store-levels/create")
@GetMapping("/store-levels/edit/{id}")
@PostMapping("/store-levels/add")
@PostMapping("/store-levels/update/{id}")
@PostMapping("/store-levels/delete/{id}")
@PostMapping("/store-levels/restore/{id}")
@PostMapping("/store-levels/reset-defaults")
```

### AdminStyleController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminStyleController.java](../src/main/java/com/finalproject/controller/admin/AdminStyleController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/styles", "/style", "/thuoc-tinh-san-pham"})
@GetMapping("/styles/create")
@GetMapping("/styles/edit/{id}")
@PostMapping("/styles/add")
@PostMapping("/styles/update/{id}")
@PostMapping("/styles/delete/{id}")
@PostMapping("/styles/restore/{id}")
@GetMapping("/styles/{id}/values")
@PostMapping("/styles/{styleId}/values/add")
@PostMapping("/styles/{styleId}/values/update/{valueId}")
@PostMapping("/styles/{styleId}/values/delete/{valueId}")
@PostMapping("/styles/{styleId}/values/restore/{valueId}")
@PostMapping("/styles/{styleId}/values/destroy/{valueId}")
@PostMapping({"/styles/seed-defaults", "/styles/reset-defaults"})
```

### AdminTransactionController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminTransactionController.java](../src/main/java/com/finalproject/controller/admin/AdminTransactionController.java)

```java
@RequestMapping("/admin/transactions")
@org.springframework.web.bind.annotation.PostMapping("/{id}/resolve")
@GetMapping
```

### AdminUserController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminUserController.java](../src/main/java/com/finalproject/controller/admin/AdminUserController.java)

```java
@RequestMapping("/admin/users")
@GetMapping
@PostMapping("/toggle-active/{id}")
@PostMapping("/create")
@PostMapping("/delete/{id}")
```

### AdminUserLevelController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminUserLevelController.java](../src/main/java/com/finalproject/controller/admin/AdminUserLevelController.java)

```java
@RequestMapping("/admin")
@GetMapping({"/user-levels", "/user-level", "/cap-bac-khach-hang"})
@GetMapping("/user-levels/create")
@GetMapping("/user-levels/edit/{id}")
@PostMapping("/user-levels/add")
@PostMapping("/user-levels/update/{id}")
@PostMapping("/user-levels/delete/{id}")
@PostMapping("/user-levels/restore/{id}")
@PostMapping("/user-levels/reset-defaults")
```

### AdminVoucherController

Nguồn: [src/main/java/com/finalproject/controller/admin/AdminVoucherController.java](../src/main/java/com/finalproject/controller/admin/AdminVoucherController.java)

```java
@RequestMapping("/admin")
@GetMapping("/vouchers")
@PostMapping("/vouchers/add")
@PostMapping("/vouchers/update/{id}")
@PostMapping("/vouchers/toggle/{id}")
@PostMapping("/vouchers/delete/{id}")
```

### AuthController

Nguồn: [src/main/java/com/finalproject/controller/auth/AuthController.java](../src/main/java/com/finalproject/controller/auth/AuthController.java)

```java
@GetMapping("/login")
@GetMapping("/register")
@PostMapping("/register")
@GetMapping("/verify-otp")
@PostMapping("/verify-otp")
@PostMapping("/resend-otp")
@GetMapping("/forgot-password")
@PostMapping("/forgot-password")
@GetMapping("/reset-password")
@PostMapping("/reset-password")
```

### JwtAuthController

Nguồn: [src/main/java/com/finalproject/controller/auth/JwtAuthController.java](../src/main/java/com/finalproject/controller/auth/JwtAuthController.java)

```java
@RequestMapping("/api/auth")
@PostMapping("/login")
@PostMapping("/refresh")
@GetMapping("/me")
@PostMapping("/logout")
```

### ChatController

Nguồn: [src/main/java/com/finalproject/controller/common/ChatController.java](../src/main/java/com/finalproject/controller/common/ChatController.java)

```java
@RequestMapping("/api/chat")
@GetMapping("/context")
@GetMapping("/conversations")
@PostMapping("/conversations")
@PostMapping("/conversations/contact")
@GetMapping("/conversations/{id}/messages")
@GetMapping("/access-logs")
@PostMapping(value="/conversations/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
```

### ChatSocketController

Nguồn: [src/main/java/com/finalproject/controller/common/ChatSocketController.java](../src/main/java/com/finalproject/controller/common/ChatSocketController.java)

```java
@MessageMapping("/chat.send")
```

### NotificationController

Nguồn: [src/main/java/com/finalproject/controller/common/NotificationController.java](../src/main/java/com/finalproject/controller/common/NotificationController.java)

```java
@GetMapping({"/notifications", "/manager/notifications"})
@GetMapping("/api/notifications/unread-count")
@GetMapping("/api/notifications/latest")
@PostMapping("/api/notifications/read/{id}")
@PostMapping("/api/notifications/read-all")
```

### BankPaymentController

Nguồn: [src/main/java/com/finalproject/controller/customer/BankPaymentController.java](../src/main/java/com/finalproject/controller/customer/BankPaymentController.java)

```java
@RequestMapping("/checkout/bank")
@GetMapping("/{orderId}")
@PostMapping("/{orderId}/simulate")
@GetMapping("/{orderId}/status")
```

### CartController

Nguồn: [src/main/java/com/finalproject/controller/customer/CartController.java](../src/main/java/com/finalproject/controller/customer/CartController.java)

```java
@RequestMapping("/cart")
@GetMapping
@PostMapping(value = "/add", headers = "X-Requested-With=XMLHttpRequest")
@PostMapping("/add")
@PostMapping(value = "/update-quantity/{id}", headers = "X-Requested-With=XMLHttpRequest")
@GetMapping(value = "/state", headers = "X-Requested-With=XMLHttpRequest")
@PostMapping("/update-quantity/{id}")
@PostMapping("/remove/{id}")
@PostMapping("/checkout")
```

### CatalogStockApiController

Nguồn: [src/main/java/com/finalproject/controller/customer/CatalogStockApiController.java](../src/main/java/com/finalproject/controller/customer/CatalogStockApiController.java)

```java
@RequestMapping("/api/public/catalog-stock")
@GetMapping
```

### CheckoutController

Nguồn: [src/main/java/com/finalproject/controller/customer/CheckoutController.java](../src/main/java/com/finalproject/controller/customer/CheckoutController.java)

```java
@RequestMapping("/checkout")
@GetMapping
@PostMapping
@PostMapping("/quote")
```

### CustomerOrderController

Nguồn: [src/main/java/com/finalproject/controller/customer/CustomerOrderController.java](../src/main/java/com/finalproject/controller/customer/CustomerOrderController.java)

```java
@RequestMapping("/customer/orders")
@GetMapping
@GetMapping({"/detail/{id}", "/{id}"})
@PostMapping("/refund/{id}")
@PostMapping("/cancel/{id}")
```

### CustomerPaymentController

Nguồn: [src/main/java/com/finalproject/controller/customer/CustomerPaymentController.java](../src/main/java/com/finalproject/controller/customer/CustomerPaymentController.java)

```java
@RequestMapping("/customer/payment")
@GetMapping("/{orderId}")
@PostMapping("/{orderId}/process")
```

### DemoPaymentController

Nguồn: [src/main/java/com/finalproject/controller/customer/DemoPaymentController.java](../src/main/java/com/finalproject/controller/customer/DemoPaymentController.java)

```java
@GetMapping("/customer/demo-payment/{token}")
@GetMapping("/demo-payment/{token}")
@PostMapping("/demo-payment/{token}/confirm")
@GetMapping("/customer/demo-payment/{token}/success")
@GetMapping("/demo-payment/{token}/success")
@PostMapping("/customer/demo-payment/order/{id}")
@GetMapping("/customer/demo-payment/{token}/status")
@GetMapping("/customer/demo-payment/{token}/connection")
@GetMapping(value = "/customer/demo-payment/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
```

### HomeController

Nguồn: [src/main/java/com/finalproject/controller/customer/HomeController.java](../src/main/java/com/finalproject/controller/customer/HomeController.java)

```java
@GetMapping({"/", "/products", "/search"})
@GetMapping("/following")
```

### MomoPaymentController

Nguồn: [src/main/java/com/finalproject/controller/customer/MomoPaymentController.java](../src/main/java/com/finalproject/controller/customer/MomoPaymentController.java)

```java
@RequestMapping("/checkout/momo")
@GetMapping("/{orderId}")
@GetMapping("/callback")
@PostMapping("/ipn")
@PostMapping("/{orderId}/simulate")
@PostMapping("/retry/{orderId}")
```

### PickupCheckController

Nguồn: [src/main/java/com/finalproject/controller/customer/PickupCheckController.java](../src/main/java/com/finalproject/controller/customer/PickupCheckController.java)

```java
@RequestMapping("/api/customer/pickup-check")
@GetMapping
```

### ProductDetailController

Nguồn: [src/main/java/com/finalproject/controller/customer/ProductDetailController.java](../src/main/java/com/finalproject/controller/customer/ProductDetailController.java)

```java
@GetMapping({"/product/{id}", "/products/{id}"})
@PostMapping({"/product/{id}/review", "/products/{id}/review"})
```

### ProfileController

Nguồn: [src/main/java/com/finalproject/controller/customer/ProfileController.java](../src/main/java/com/finalproject/controller/customer/ProfileController.java)

```java
@RequestMapping({"/customer/profile", "/customer"})
@GetMapping({"", "/profile"})
@PostMapping({"/update", "/profile/update"})
@PostMapping({"/send-password-otp", "/profile/send-password-otp"})
@PostMapping({"/change-password", "/profile/change-password"})
@PostMapping({"/address/add", "/profile/address/add"})
@PostMapping({"/address/update/{id}", "/profile/address/update/{id}"})
@PostMapping({"/address/delete/{id}", "/profile/address/delete/{id}"})
@PostMapping({"/address/set-default/{id}", "/profile/address/set-default/{id}"})
@GetMapping({"/wishlist", "/profile/wishlist"})
@GetMapping({"/following", "/profile/following"})
@PostMapping({"/wishlist/toggle/{productId}", "/profile/wishlist/toggle/{productId}", "/following/toggle/{productId}", "/following/product/{productId}", "/products/{productId}/follow"})
@GetMapping({"/recently-viewed", "/profile/recently-viewed"})
@PostMapping({"/recently-viewed/clear", "/profile/recently-viewed/clear"})
@GetMapping({"/followed-stores", "/profile/followed-stores"})
@PostMapping({"/store/follow/{storeId}", "/profile/store/follow/{storeId}"})
```

### StorePublicController

Nguồn: [src/main/java/com/finalproject/controller/customer/StorePublicController.java](../src/main/java/com/finalproject/controller/customer/StorePublicController.java)

```java
@RequestMapping("/stores")
@GetMapping
@GetMapping("/{id}")
@PostMapping("/{id}/follow")
```

### UserPublicController

Nguồn: [src/main/java/com/finalproject/controller/customer/UserPublicController.java](../src/main/java/com/finalproject/controller/customer/UserPublicController.java)

```java
@RequestMapping("/users")
@GetMapping
@GetMapping("/{id}")
```

### InventoryApiController

Nguồn: [src/main/java/com/finalproject/controller/InventoryApiController.java](../src/main/java/com/finalproject/controller/InventoryApiController.java)

```java
@RequestMapping("/api/inventory")
@GetMapping("/check")
```

### ManagerAnalyticsController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerAnalyticsController.java](../src/main/java/com/finalproject/controller/manager/ManagerAnalyticsController.java)

```java
@RequestMapping({"/manager/analytics", "/vendor/analytics"})
@GetMapping
```

### ManagerDashboardController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerDashboardController.java](../src/main/java/com/finalproject/controller/manager/ManagerDashboardController.java)

```java
@RequestMapping({"/manager", "/vendor"})
@GetMapping("/dashboard/stock")
@GetMapping({"", "/dashboard"})
```

### ManagerOrderController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerOrderController.java](../src/main/java/com/finalproject/controller/manager/ManagerOrderController.java)

```java
@RequestMapping({"/manager/orders", "/vendor/orders"})
@GetMapping
@GetMapping({"/{id}", "/detail/{id}"})
@PostMapping("/update-status/{id}")
@PostMapping("/confirm-payment/{id}")
```

### ManagerProductController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerProductController.java](../src/main/java/com/finalproject/controller/manager/ManagerProductController.java)

```java
@RequestMapping({"/manager/products", "/vendor/products"})
@GetMapping("/create")
@GetMapping("/edit/{id}")
@GetMapping
@PostMapping("/add")
@PostMapping("/update/{id}")
@PostMapping("/toggle-selling/{id}")
@GetMapping("/branch/{id}")
@PostMapping("/branch/{id}")
@PostMapping("/delete/{id}")
@PostMapping("/restore/{id}")
@PostMapping("/stock/{id}")
```

### ManagerProfileController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerProfileController.java](../src/main/java/com/finalproject/controller/manager/ManagerProfileController.java)

```java
@RequestMapping({"/manager/profile", "/manager/stores/profile", "/vendor/profile"})
@GetMapping
@PostMapping("/avatar")
@PostMapping("/cover")
@PostMapping("/featured/add")
@PostMapping("/featured/edit")
@PostMapping("/featured/delete")
@PostMapping("/info")
```

### ManagerStaffController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerStaffController.java](../src/main/java/com/finalproject/controller/manager/ManagerStaffController.java)

```java
@RequestMapping({"/manager/staff", "/vendor/staff", "/manager/staffs", "/vendor/staffs"})
@GetMapping
@PostMapping("/add")
@PostMapping({"/remove/{id}", "/delete/{id}"})
@PostMapping("/out-shop")
```

### ManagerStoreController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerStoreController.java](../src/main/java/com/finalproject/controller/manager/ManagerStoreController.java)

```java
@RequestMapping({"/manager/stores", "/vendor/stores"})
@GetMapping
@PostMapping({"/switch/{id}", "/go-dashboard/{id}"})
@GetMapping({"/switch/{id}", "/go-dashboard/{id}"})
@GetMapping("/create")
@PostMapping("/create")
@GetMapping("/edit/{id}")
@PostMapping("/edit/{id}")
```

### ManagerVoucherController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerVoucherController.java](../src/main/java/com/finalproject/controller/manager/ManagerVoucherController.java)

```java
@RequestMapping({"/manager/vouchers", "/vendor/vouchers"})
@GetMapping
@PostMapping("/add")
@PostMapping("/update/{id}")
@PostMapping("/toggle/{id}")
@PostMapping("/delete/{id}")
```

### ManagerWalletController

Nguồn: [src/main/java/com/finalproject/controller/manager/ManagerWalletController.java](../src/main/java/com/finalproject/controller/manager/ManagerWalletController.java)

```java
@RequestMapping({"/manager/wallet", "/vendor/wallet", "/manager/gd-coins", "/vendor/gd-coins"})
@GetMapping
@PostMapping("/withdraw")
```

### StaffOrderController

Nguồn: [src/main/java/com/finalproject/controller/staff/StaffOrderController.java](../src/main/java/com/finalproject/controller/staff/StaffOrderController.java)

```java
@RequestMapping("/staff/orders")
@GetMapping
@PostMapping("/approve/{id}")
@PostMapping("/deliver/{id}")
@PostMapping("/prepare/{id}")
```

## 3. Tài liệu tự test

[Kịch bản tự test toàn dự án](KICH_BAN_TU_TEST_TOAN_DU_AN.md). Tài liệu kiểm thử cũ tại thư mục gốc còn nhắc role SHIPPER và một số hành vi đã thay đổi; ưu tiên đối chiếu code hiện tại và bộ kịch bản mới.
