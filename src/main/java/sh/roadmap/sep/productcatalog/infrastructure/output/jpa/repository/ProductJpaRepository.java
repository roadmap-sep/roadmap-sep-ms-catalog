package sh.roadmap.sep.productcatalog.infrastructure.output.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID>,
        JpaSpecificationExecutor<ProductEntity> {
    Optional<ProductEntity> findBySku(String productSku);
}
