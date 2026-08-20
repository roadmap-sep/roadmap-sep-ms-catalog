package sh.roadmap.sep.catalog.infrastructure.input.rest.controller;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.catalog.application.service.ProductService;
import sh.roadmap.sep.catalog.domain.exception.ProductAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.ProductNotFoundException;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.util.Page;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRestController.class)
class ProductRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private static final String BASE_URL = "/v1.0/products";
    private final UUID productId = UUID.randomUUID();

    private ProductRequest.ProductRequestBuilder validRequestBuilder() {
        return ProductRequest.builder()
                .sku("PROD-123")
                .name("Smartphone Model X")
                .description("Un smartphone de última generación")
                .categoryIds(Set.of(1L, 2L))
                .price(new BigDecimal("999.99"))
                .stock(50)
                .mainImageUrl("https://example.com/images/prod123.jpg")
                .weight(0.25);
    }

    private final ProductResponse productResponse = ProductResponse.builder()
            .id(productId)
            .sku("PROD-123")
            .name("Smartphone Model X")
            .categories(Set.of("Electronics", "Smartphones"))
            .description("Un smartphone de última generación")
            .price(new BigDecimal("999.99"))
            .mainImageUrl("https://example.com/images/prod123.jpg")
            .stock(50)
            .weight(0.25)
            .active(true)
            .build();

    @Test
    @DisplayName("Should return error when invalid Api version")
    void shouldReturnErrorWhenInvalidApiVersion() throws Exception {
        mockMvc.perform(get("/v999.0/products/batch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.CoreMatchers.containsString("is not available")));
    }

    @Nested
    @DisplayName("GET " + BASE_URL)
    class SearchProductsTests {

        @Test
        @DisplayName("Should return 200 OK and mapped parameters to ProductFilter")
        void shouldReturnPagedProductsWithFilters() throws Exception {
            String name = "Smartphone";
            Long categoryId = 1L;
            BigDecimal minPrice = new BigDecimal("500.00");
            BigDecimal maxPrice = new BigDecimal("1500.00");
            Boolean inStock = true;
            Boolean isActive = true;
            int page = 0;
            int size = 20;

            var filter = new ProductFilter(name, categoryId, minPrice, maxPrice, inStock, isActive);
            var pageRequest = new Page.Request(page, size);
            var productPage = Page.<ProductResponse>builder()
                    .data(List.of(productResponse))
                    .pageNumber(page)
                    .pageSize(size)
                    .totalElements(1)
                    .build();

            given(productService.searchProducts(filter, pageRequest)).willReturn(productPage);

            mockMvc.perform(get(BASE_URL)
                            .param("name", name)
                            .param("category_id", String.valueOf(categoryId))
                            .param("min_price", minPrice.toString())
                            .param("max_price", maxPrice.toString())
                            .param("in_stock", String.valueOf(inStock))
                            .param("is_active", String.valueOf(isActive))
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(productId.toString()))
                    .andExpect(jsonPath("$.data[0].sku").value("PROD-123"));

            then(productService).should().searchProducts(filter, pageRequest);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_URL + "/{product_id}")
    class GetByIdTests {

        @Test
        @DisplayName("Should return 200 OK and product details")
        void shouldReturnProductById() throws Exception {
            given(productService.getById(productId)).willReturn(productResponse);

            mockMvc.perform(get(BASE_URL + "/{product_id}", productId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productId.toString()))
                    .andExpect(jsonPath("$.sku").value("PROD-123"));
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when ID not found")
        void shouldReturnProductNotFoundExceptionWhenIdNotFound() throws Exception {
            given(productService.getById(productId))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(get(BASE_URL + "/{product_id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Nested
    @DisplayName("GET " + BASE_URL + "/sku/{sku}")
    class GetBySkuTests {

        @Test
        @DisplayName("Should return 200 OK and product details by SKU")
        void shouldReturnProductBySku() throws Exception {
            String sku = "PROD-123";
            given(productService.getBySku(sku)).willReturn(productResponse);

            mockMvc.perform(get(BASE_URL + "/sku/{sku}", sku)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value(sku));
        }
    }

    @Nested
    @DisplayName("POST " + BASE_URL + " (Validations)")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 Created for a valid payload")
        void shouldCreateProduct() throws Exception {
            ProductRequest validRequest = validRequestBuilder().build();
            willDoNothing().given(productService).save(validRequest);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            then(productService).should().save(validRequest);
        }

        @Test
        @DisplayName("Should throw ProductAlreadyExistsException")
        void shouldReturnProductAlreadyExistsException() throws Exception {
            ProductRequest validRequest = validRequestBuilder().build();

            doThrow(new ProductAlreadyExistsException(validRequest.sku()))
                    .when(productService).save(any(ProductRequest.class));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("Should return 400 when SKU fails regex pattern")
        void shouldReturn400WhenSkuIsInvalid() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().sku("INVALID SKU!").build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(productService).should(never()).save(any());
        }

        @Test
        @DisplayName("Should return 400 when Price is zero or negative")
        void shouldReturn400WhenPriceIsInvalid() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().price(new BigDecimal("0.00")).build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(productService).should(never()).save(any());
        }

        @Test
        @DisplayName("Should return 400 when Categories set is empty")
        void shouldReturn400WhenCategoriesEmpty() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().categoryIds(Set.of()).build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when Image is not a valid URL")
        void shouldReturn400WhenImageNotUrl() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().mainImageUrl("not_a_url").build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when stock or weight is negative")
        void shouldReturn400WhenMetricsAreNegative() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().stock(-5).weight(-0.5).build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST " + BASE_URL + "/batch")
    class BatchCreateTests {

        @Test
        @DisplayName("Should return 201 Created for a valid batch file")
        void shouldCreateBatchSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                    "text/csv", "sku,name\nPROD-1,Name1".getBytes());

            willDoNothing().given(productService).saveBatch(any(ProductImportRequest.class));

            mockMvc.perform(multipart(BASE_URL + "/batch").file(file))
                    .andExpect(status().isCreated());

            then(productService).should().saveBatch(any(ProductImportRequest.class));
        }

        @Test
        @DisplayName("Should throw ConstraintViolationException when file extension is missing")
        void shouldThrowExceptionWhenManualValidationFails() throws Exception {
            MockMultipartFile invalidFile = new MockMultipartFile("file", "products",
                    "text/csv", "sku,name".getBytes());

            mockMvc.perform(multipart(BASE_URL + "/batch").file(invalidFile))
                    .andExpect(result -> assertInstanceOf(ConstraintViolationException.class,
                            result.getResolvedException()));

            then(productService).should(never()).saveBatch(any());
        }

        @Test
        @DisplayName("Should throw ImportException when IoException occurs")
        void shouldThrowProductImportExceptionWhenIoExceptionOccurs() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    MediaType.TEXT_PLAIN_VALUE, "dummy content".getBytes()) {
                @Override
                public InputStream getInputStream() throws IOException {
                    throw new IOException("Error simulado de IO");
                }
            };
            mockMvc.perform(multipart(BASE_URL + "/batch")
                            .file(file))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return error when max upload size exceeded")
        void shouldReturnErrorWhenMaxUploadSizeExceeded() throws Exception {
            byte[] largeContent = new byte[1024 * 1024 * 3];

            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large-products.csv",
                    MediaType.TEXT_PLAIN_VALUE,
                    largeContent
            );

            doThrow(new MaxUploadSizeExceededException(2097152))
                    .when(productService).saveBatch(any());

            mockMvc.perform(multipart(BASE_URL + "/batch")
                            .file(largeFile))
                    .andExpect(status().is(413))
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_URL + "/{product_id}")
    class UpdateTests {

        @Test
        @DisplayName("Should return 204 No Content for a valid update payload")
        void shouldUpdateSuccessfully() throws Exception {
            ProductRequest validRequest = validRequestBuilder().build();
            willDoNothing().given(productService).update(validRequest, productId);

            mockMvc.perform(put(BASE_URL + "/{product_id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNoContent());

            then(productService).should().update(validRequest, productId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when update payload is invalid")
        void shouldReturn400WhenUpdateIsInvalid() throws Exception {
            ProductRequest invalidRequest = validRequestBuilder().name("").build(); // Blank name

            mockMvc.perform(put(BASE_URL + "/{product_id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(productService).should(never()).update(any(), eq(productId));
        }
    }

    @Nested
    @DisplayName("PATCH " + BASE_URL + "/{product_id}")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should return 204 No Content when toggling active status")
        void shouldToggleActiveStatusSuccessfully() throws Exception {
            willDoNothing().given(productService).toggleActiveStatus(productId);

            mockMvc.perform(patch(BASE_URL + "/{product_id}", productId))
                    .andExpect(status().isNoContent());

            then(productService).should().toggleActiveStatus(productId);
        }
    }
}
