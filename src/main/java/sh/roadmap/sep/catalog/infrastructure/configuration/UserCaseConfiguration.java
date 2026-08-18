package sh.roadmap.sep.catalog.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.catalog.domain.port.in.ProductPortIn;
import sh.roadmap.sep.catalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.catalog.domain.port.out.ProductPortOut;
import sh.roadmap.sep.catalog.domain.usecase.CategoryUseCase;
import sh.roadmap.sep.catalog.domain.usecase.ProductUseCase;

@Configuration
public class UserCaseConfiguration {
    @Bean
    public ProductPortIn productPortIn(ProductPortOut productPortOut) {
        return new ProductUseCase(productPortOut);
    }

    @Bean
    public CategoryPortIn categoryPortIn(CategoryPortOut categoryPortOut) {
        return new CategoryUseCase(categoryPortOut);
    }
}
