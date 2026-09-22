package com.finalproject.service;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.Product;
import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(ProductForm form) throws IOException;
    void deleteProduct(Long id);
}