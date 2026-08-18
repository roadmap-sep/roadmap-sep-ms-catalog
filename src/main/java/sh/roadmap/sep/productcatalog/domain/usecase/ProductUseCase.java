package sh.roadmap.sep.productcatalog.domain.usecase;

import sh.roadmap.sep.productcatalog.domain.model.Product;
import sh.roadmap.sep.productcatalog.domain.model.ProductFilter;
import sh.roadmap.sep.productcatalog.domain.port.in.ProductPortIn;
import sh.roadmap.sep.productcatalog.domain.port.out.ProductPortOut;
import sh.roadmap.sep.productcatalog.domain.util.Page;

import java.util.List;
import java.util.UUID;

public class ProductUseCase implements ProductPortIn {
    private final ProductPortOut productPortOut;

    public ProductUseCase(ProductPortOut productPortOut) {
        this.productPortOut = productPortOut;
    }

    @Override
    public Page<Product> searchProducts(ProductFilter productFilter, Page.Request pageRequest) {
        return productPortOut.searchProducts(productFilter, pageRequest);
    }

    @Override
    public Product getById(UUID productId) {
        return productPortOut.getById(productId);
    }

    @Override
    public Product getBySku(String sku) {
        return productPortOut.getBySku(sku);
    }

    @Override
    public void save(Product product) {
        productPortOut.save(product);
    }

    @Override
    public void saveBatch(List<Product> products) {
        productPortOut.saveBatch(products);
    }

    @Override
    public void update(Product product) {
        productPortOut.update(product);
    }
}
