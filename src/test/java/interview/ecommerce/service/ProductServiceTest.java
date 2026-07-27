package interview.ecommerce.service;

import interview.ecommerce.dto.CreateProductRequest;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.dto.ProductDto;
import interview.ecommerce.entity.Product;
import interview.ecommerce.exceptions.DuplicateResourceException;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.ProductMapper;
import interview.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private static final ProductMapper productMapper = new ProductMapper();

    private ProductService productService;

    private Product product;

    @BeforeEach
    void setup() {
        product = new Product("PR1", "product name", 10);
        productService = new ProductService(productRepository, productMapper);
    }



    @Test
    public void findAllTest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        List<Product> products = List.of(product, new Product("PR2", "product name 2", 20));

        when(productRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(products, pageable, 2));

        PageResponse<ProductDto> response = productService.findAll(pageable);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();

        assertThat(response.content()).hasSize(2);

        assertThat(response.content().get(0).getCode()).isEqualTo("PR1");
        assertThat(response.content().get(0).getName()).isEqualTo("product name");
        assertThat(response.content().get(0).getStock()).isEqualTo(10);

        assertThat(response.content().get(1).getCode()).isEqualTo("PR2");
        assertThat(response.content().get(1).getName()).isEqualTo("product name 2");
        assertThat(response.content().get(1).getStock()).isEqualTo(20);

        verify(productRepository).findAll(pageable);
    }

    @Test
    public void createOkTest() {
        when(productRepository.existsByCode("PR3")).thenReturn(false);

        AtomicReference<Long> idBeforeSave = new AtomicReference<>();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product productReceived = invocation.getArgument(0);
            idBeforeSave.set(productReceived.getId());
            productReceived.setId(1L);
            return productReceived;
        });

        CreateProductRequest createProductRequest = new CreateProductRequest("PR3", "Product 3", 30);

        ProductDto productDto = productService.create(createProductRequest);

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productArgumentCaptor.capture());
        Product productSaved = productArgumentCaptor.getValue();

        assertThat(idBeforeSave.get()).isNull();
        assertThat(productSaved.getCode()).isEqualTo("PR3");
        assertThat(productSaved.getName()).isEqualTo("Product 3");
        assertThat(productSaved.getStock()).isEqualTo(30);

        assertThat(productDto.getId()).isEqualTo(1L);
        assertThat(productDto.getCode()).isEqualTo("PR3");
        assertThat(productDto.getName()).isEqualTo("Product 3");
        assertThat(productDto.getStock()).isEqualTo(30);

        verify(productRepository).existsByCode("PR3");
    }

    @Test
    public void createDuplicateResource409Test() {
        when(productRepository.existsByCode("PR1")).thenReturn(true);

        CreateProductRequest createProductRequest = new CreateProductRequest("PR1", "Product 1", 10);

        assertThatThrownBy(() -> productService.create(createProductRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("PR1");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    public void findByCodeForUpdateOkTest(){
        product.setId(1L);
        when(productRepository.findByCodeForUpdate("PR1")).thenReturn(Optional.ofNullable(product));

        Product productEntity = productService.findProductByCodeForUpdate("PR1");

        assertThat(productEntity.getId()).isEqualTo(1L);
    }

    @Test
    public void findByCodeForUpdateEntityNotFoundTest(){
        when(productRepository.findByCodeForUpdate("PR2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProductByCodeForUpdate("PR2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PR2");

    }
}