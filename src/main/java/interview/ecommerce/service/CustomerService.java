package interview.ecommerce.service;

import interview.ecommerce.dto.CreateCustomerRequest;
import interview.ecommerce.dto.CustomerDto;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.entity.Customer;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.CustomerMapper;
import interview.ecommerce.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper){
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request){
        log.debug("Creating new customer");

        Customer customer = customerRepository.save(customerMapper.toEntityFromCreateRequest(request));

        log.info("Customer {} created", customer.getId());
        return customerMapper.toDto(customer);
    }

    public PageResponse<CustomerDto> findAll(Pageable pageable){
        log.debug("Finding customers: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<CustomerDto> response = PageResponse.of(customerRepository.findAll(pageable), customerMapper::toDto);

        log.debug("Retrieved {} customer(s) out of {}", response.content().size(), response.totalElements());
        return response;
    }

    public Customer findCustomerEntityById(Long id) {
        log.trace("Finding customer by id {}", id);

        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.customer(id));
    }
}