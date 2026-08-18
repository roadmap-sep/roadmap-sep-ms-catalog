package sh.roadmap.sep.productcatalog.infrastructure.output.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.productcatalog.domain.model.Category;
import sh.roadmap.sep.productcatalog.domain.util.Page;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.entity.CategoryEntity;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.repository.CategoryJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryJpaAdapterTest {

    @Mock
    private CategoryJpaRepository categoryJpaRepository;

    @Mock
    private CategoryJpaMapper categoryJpaMapper;

    @InjectMocks
    private CategoryJpaAdapter categoryJpaAdapter;

    private final Category category = new Category(1L, "Electronics",
            "electronics-slug", null, true);
    private final CategoryEntity categoryEntity = new CategoryEntity(1L,
            "Electronics", "electronics-slug", null, true);
    private final Page.Request pageRequest = new Page.Request(0, 10);

    @Nested
    @DisplayName("Tests for getAll()")
    class GetAllTests {

        @Test
        @DisplayName("should return a mapped category page")
        void shouldReturnMappedCategoryPage() {
            var pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize());
            var entityPage = new PageImpl<>(List.of(categoryEntity), pageable, 1);

            given(categoryJpaRepository.findAll(pageable)).willReturn(entityPage);
            given(categoryJpaMapper.toModel(categoryEntity)).willReturn(category);

            var result = categoryJpaAdapter.getAll(pageRequest);

            assertThat(result).isNotNull();
            assertThat(result.data()).containsExactly(category);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.pageNumber()).isZero();
            assertThat(result.pageSize()).isEqualTo(10);

            then(categoryJpaRepository).should().findAll(pageable);
            then(categoryJpaMapper).should().toModel(categoryEntity);
        }
    }

    @Nested
    @DisplayName("Tests for getByName()")
    class GetByNameTests {

        @Test
        @DisplayName("should return a mapped page filtered by name")
        void shouldReturnMappedCategoryPageByName() {
            String nameToSearch = "elec";
            var pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize());
            var entityPage = new PageImpl<>(List.of(categoryEntity), pageable, 1);

            given(categoryJpaRepository.findByNameContainingIgnoreCase(nameToSearch, pageable))
                    .willReturn(entityPage);
            given(categoryJpaMapper.toModel(categoryEntity)).willReturn(category);

            var result = categoryJpaAdapter.getByName(nameToSearch, pageRequest);

            assertThat(result.data()).containsExactly(category);
            then(categoryJpaRepository).should().findByNameContainingIgnoreCase(nameToSearch, pageable);
        }
    }

    @Nested
    @DisplayName("Tests for getById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return category when exists")
        void shouldReturnCategoryWhenExists() {
            long categoryId = 1L;
            given(categoryJpaRepository.findById(categoryId)).willReturn(Optional.of(categoryEntity));
            given(categoryJpaMapper.toModel(categoryEntity)).willReturn(category);

            var result = categoryJpaAdapter.getById(categoryId);

            assertThat(result).isEqualTo(category);
            then(categoryJpaRepository).should().findById(categoryId);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException if the category does not exist.")
        void shouldThrowExceptionWhenCategoryDoesNotExist() {
            long categoryId = 99L;
            given(categoryJpaRepository.findById(categoryId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryJpaAdapter.getById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(categoryJpaMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("should save Category successfully")
        void shouldSaveCategorySuccessfully() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            categoryJpaAdapter.create(category);

            then(categoryJpaRepository).should().save(categoryEntity);
        }

        @Test
        @DisplayName("should throw CategoryAlreadyExistsException if there is an ID collision")
        void shouldThrowExceptionWhenIdViolatesIntegrity() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            var cause = new RuntimeException("duplicate key value violates unique constraint on id");
            var exception = new DataIntegrityViolationException("DB Error", cause);

            given(categoryJpaRepository.save(categoryEntity)).willThrow(exception);

            assertThatThrownBy(() -> categoryJpaAdapter.create(category))
                    .isInstanceOf(CategoryAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw CategoryAlreadyExistsException if there is a slug collision")
        void shouldThrowExceptionWhenSlugViolatesIntegrity() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            var cause = new RuntimeException("duplicate key value violates unique constraint on slug");
            var exception = new DataIntegrityViolationException("DB Error", cause);

            given(categoryJpaRepository.save(categoryEntity)).willThrow(exception);

            assertThatThrownBy(() -> categoryJpaAdapter.create(category))
                    .isInstanceOf(CategoryAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should propagate DataIntegrityViolationException if the cause is other than id or slug")
        void shouldPropagateExceptionWhenUnknownViolation() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            var cause = new RuntimeException("foreign key constraint fails on another_field");
            var exception = new DataIntegrityViolationException("DB Error", cause);

            given(categoryJpaRepository.save(categoryEntity)).willThrow(exception);

            assertThatThrownBy(() -> categoryJpaAdapter.create(category))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Tests for update()")
    class UpdateTests {

        @Test
        @DisplayName("You must update if the category exists")
        void shouldUpdateCategoryWhenExists() {
            given(categoryJpaRepository.existsById(category.id())).willReturn(true);
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            categoryJpaAdapter.update(category);

            then(categoryJpaRepository).should().save(categoryEntity);
        }

        @Test
        @DisplayName("It should throw CategoryNotFoundException if an update "
                + "attempt is made and the CategoryNotFoundException does not exist.")
        void shouldThrowExceptionWhenUpdatingNonExistentCategory() {
            given(categoryJpaRepository.existsById(category.id())).willReturn(false);

            assertThatThrownBy(() -> categoryJpaAdapter.update(category))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(categoryJpaRepository).should(never()).save(any());
            then(categoryJpaMapper).shouldHaveNoInteractions();
        }
    }
}