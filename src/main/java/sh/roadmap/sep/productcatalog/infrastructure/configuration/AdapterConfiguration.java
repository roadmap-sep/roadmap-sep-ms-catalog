package sh.roadmap.sep.productcatalog.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import sh.roadmap.sep.productcatalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.productcatalog.domain.port.out.ProductPortOut;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.adapter.CategoryJpaAdapter;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.adapter.ProductJpaAdapter;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.mapper.ProductJpaMapper;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.repository.CategoryJpaRepository;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.repository.ProductJpaRepository;

@Configuration
public class AdapterConfiguration {
    @Bean
    public ProductPortOut productPortOut(ProductJpaRepository productJpaRepository,
                                         ProductJpaMapper productJpaMapper,
                                         JdbcTemplate jdbcTemplate) {
        return new ProductJpaAdapter(productJpaRepository, productJpaMapper, jdbcTemplate);
    }

    @Bean
    public CategoryPortOut categoryPortOut(CategoryJpaRepository categoryJpaRepository,
                                           CategoryJpaMapper categoryJpaMapper) {
        return new CategoryJpaAdapter(categoryJpaRepository, categoryJpaMapper);
    }
}
