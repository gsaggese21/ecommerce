package interview.ecommerce.service;
import interview.ecommerce.dto.CreateProductRequest;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.dto.ProductDto;
import interview.ecommerce.entity.Product;
import interview.ecommerce.exceptions.DuplicateResourceException;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.ProductMapper;
import interview.ecommerce.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper){
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductDto create(CreateProductRequest productRequest){
        log.debug("Creating product {} with initial stock {}",
                productRequest.productCode(), productRequest.stock());

        if (productRepository.existsByCode(productRequest.productCode()))
            throw DuplicateResourceException.product(productRequest.productCode());

        Product product = productRepository.save(productMapper.createProductRequesttoEntity(productRequest));

        log.info("Product {} created with id {} and stock {}",
                product.getCode(), product.getId(), product.getStock());

        return productMapper.productEntityToDto(product);
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public Product findProductByCodeForUpdate(String productCode){
        log.debug("Finding product by code {} with pessimistic lock", productCode);

        Product product = productRepository.findByCodeForUpdate(productCode)
                .orElseThrow(() -> ResourceNotFoundException.product(productCode));

        log.debug("Lock acquired on product {}, current stock {}", productCode, product.getStock());
        return product;
    }

    public PageResponse<ProductDto> findAll(Pageable pageable) {
        log.debug("Finding products: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<ProductDto> response =
                PageResponse.of(productRepository.findAll(pageable), productMapper::productEntityToDto);

        log.debug("Retrieved {} product(s) out of {}", response.content().size(), response.totalElements());
        return response;
    }

}