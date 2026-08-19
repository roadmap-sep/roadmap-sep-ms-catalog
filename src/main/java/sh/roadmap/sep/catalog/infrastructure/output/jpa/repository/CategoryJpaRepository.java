package sh.roadmap.sep.catalog.infrastructure.output.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.CategoryEntity;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long>,
        JpaSpecificationExecutor<CategoryEntity> {
    Page<CategoryEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
