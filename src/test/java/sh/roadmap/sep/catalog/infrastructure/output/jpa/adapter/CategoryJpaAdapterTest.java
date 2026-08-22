package sh.roadmap.sep.catalog.infrastructure.output.jpa.adapter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import sh.roadmap.sep.catalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.util.Page;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.CategoryEntity;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.repository.CategoryJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CategoryJpaAdapterTest {

    @Mock
    private CategoryJpaRepository categoryJpaRepository;

    @Mock
    private CategoryJpaMapper categoryJpaMapper;

    @Captor
    private ArgumentCaptor<CategoryEntity> categoryEntityCaptor;

    @Captor
    private ArgumentCaptor<Specification<CategoryEntity>> specificationCaptor;

    @InjectMocks
    private CategoryJpaAdapter categoryJpaAdapter;

    private final Category category = new Category(1L, "Electronics",
            "electronics-slug", null, true);
    private final CategoryEntity categoryEntity = new CategoryEntity(1L,
            "Electronics", "electronics-slug", null, true);
    private final Page.Request pageRequest = new Page.Request(0, 10);

    @Nested
    @DisplayName("Tests for searchCategories()")
    class SearchCategoriesTests {

        @Test
        @DisplayName("should return a mapped category page and execute dynamic filters (True branches)")
        @SuppressWarnings("unchecked")
        void shouldReturnMappedCategoryPageWhenSearching() {
            var filter = new CategoryFilter("Electronics", "electronics-slug", 1L, true);
            var pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize());
            var entityPage = new PageImpl<>(List.of(categoryEntity), pageable, 1);

            given(categoryJpaRepository.findAll(specificationCaptor.capture(), eq(pageable)))
                    .willReturn(entityPage);
            given(categoryJpaMapper.toModel(categoryEntity)).willReturn(category);
            var result = categoryJpaAdapter.searchCategories(filter, pageRequest);

            Specification<CategoryEntity> capturedSpec = specificationCaptor.getValue();

            Root<CategoryEntity> rootMock = mock(Root.class);
            CriteriaQuery<?> queryMock = mock(CriteriaQuery.class);
            CriteriaBuilder cbMock = mock(CriteriaBuilder.class);
            Path<?> pathMock = mock(Path.class);

            given(rootMock.get(anyString())).willReturn((Path) pathMock);

            lenient().when(cbMock.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cbMock.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cbMock.equal(any(), any())).thenReturn(mock(Predicate.class));
            lenient().when(cbMock.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

            capturedSpec.toPredicate(rootMock, queryMock, cbMock);

            assertThat(result).isNotNull();
            assertThat(result.data()).containsExactly(category);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should execute dynamic filters with null or blank values (False branches)")
        @SuppressWarnings("unchecked")
        void shouldExecuteSpecificationWithNullOrBlankFilters() {
            var filter = new CategoryFilter("   ", "", null, null);
            var pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize());
            var entityPage = new PageImpl<>(List.of(categoryEntity), pageable, 1);

            given(categoryJpaRepository.findAll(specificationCaptor.capture(), eq(pageable)))
                    .willReturn(entityPage);
            given(categoryJpaMapper.toModel(categoryEntity)).willReturn(category);

            categoryJpaAdapter.searchCategories(filter, pageRequest);

            Specification<CategoryEntity> capturedSpec = specificationCaptor.getValue();
            Root<CategoryEntity> rootMock = mock(Root.class);
            CriteriaQuery<?> queryMock = mock(CriteriaQuery.class);
            CriteriaBuilder cbMock = mock(CriteriaBuilder.class);

            given(cbMock.and(any(Predicate[].class))).willReturn(mock(Predicate.class));

            capturedSpec.toPredicate(rootMock, queryMock, cbMock);
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

            var cause = new RuntimeException("duplicate key value violates unique constraint on primary");
            var exception = new DataIntegrityViolationException("DB Error", cause);

            given(categoryJpaRepository.save(categoryEntity)).willThrow(exception);

            assertThatThrownBy(() -> categoryJpaAdapter.create(category))
                    .isInstanceOf(CategoryAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw CategoryAlreadyExistsException if there is a slug collision")
        void shouldThrowExceptionWhenSlugViolatesIntegrity() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            var cause = new RuntimeException("duplicate key value violates unique constraint on uk_category_slug");
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
        @DisplayName("Should mark entity as not new and save")
        void shouldUpdateProductSuccessfully() {
            given(categoryJpaMapper.toEntity(category)).willReturn(categoryEntity);

            categoryJpaAdapter.update(category);
            then(categoryJpaRepository).should().save(categoryEntityCaptor.capture());
            CategoryEntity capturedEntity = categoryEntityCaptor.getValue();
            assertThat(capturedEntity.isNew()).isFalse();
        }

    }
}