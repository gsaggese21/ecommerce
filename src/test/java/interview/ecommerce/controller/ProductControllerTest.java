package interview.ecommerce.controller;


import interview.ecommerce.dto.CreateProductRequest;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.dto.ProductDto;
import interview.ecommerce.exceptions.DuplicateResourceException;
import interview.ecommerce.exceptions.GlobalExceptionHandler;
import interview.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final String VALID_BODY = """
            { "product_code": "PR1", "product_name": "Product 1", "stock": 10 }
            """;

    @Test
    public void createProduct201Test() throws Exception {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(productDto());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.product_id").value(1))
                .andExpect(jsonPath("$.product_code").value("PR1"))
                .andExpect(jsonPath("$.name").value("Product 1"))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    public void createProductDeserializesRequestTest() throws Exception {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(productDto());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateProductRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductRequest.class);
        verify(productService).create(requestCaptor.capture());
        CreateProductRequest received = requestCaptor.getValue();

        assertThat(received.productCode()).isEqualTo("PR1");
        assertThat(received.productName()).isEqualTo("Product 1");
        assertThat(received.stock()).isEqualTo(10);
    }

    @Test
    public void createProductDuplicateResource409Test() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(DuplicateResourceException.product("PR1"));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicated Resource"))
                .andExpect(jsonPath("$.detail").value(containsString("PR1")));
    }

    @Test
    public void createProductEmptyCode400Test() throws Exception {
        String body = """
                { "product_code": "", "product_name": "Product 1", "stock": 10 }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    public void createProductMissingCode400Test() throws Exception {
        String body = """
                { "product_name": "Product 1", "stock": 10 }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    public void createProductUnsupportedMediaType415Test() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(productService);
    }

    @Test
    public void getAllProducts200Test() throws Exception {
        when(productService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(productDto()), 0, 20, 1, 1, true));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].product_code").value("PR1"))
                .andExpect(jsonPath("$.content[0].product_id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.total_pages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    public void getAllProductsEmpty200Test() throws Exception {
        when(productService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.total_elements").value(0))
                .andExpect(jsonPath("$.total_pages").value(0));
    }

    private ProductDto productDto() {
        return new ProductDto(1L, "PR1", "Product 1", 10);
    }
}
