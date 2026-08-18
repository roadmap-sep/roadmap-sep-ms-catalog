package sh.roadmap.sep.catalog.domain.port.in;

import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.util.List;
import java.util.UUID;

public interface ProductPortIn {

    Page<Product> searchProducts(ProductFilter productFilter, Page.Request pageRequest);

    Product getById(UUID productId);

    Product getBySku(String sku);

    void save(Product product);

    void saveBatch(List<Product> products);

    void update(Product product);
}
