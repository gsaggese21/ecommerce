package interview.ecommerce.service;


import interview.ecommerce.dto.CreateCustomerRequest;
import interview.ecommerce.dto.CustomerDto;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.entity.Customer;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.CustomerMapper;
import interview.ecommerce.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private final CustomerMapper customerMapper = new CustomerMapper();

    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customer(1L, "Mario", "Rossi", "MRARSS79D26T215V","mario@email.com");
        customerService = new CustomerService(customerRepository, customerMapper);
    }


    @Test
    public void findAllCustomersTest(){
        Customer customer2 = customer(2L, "Luigi", "Rossi", "LGURSS56D26T215V","luigi@email.com");
        Pageable pageable = PageRequest.of(0,20, Sort.by("id"));
        List<Customer> customers = List.of(customer,customer2);

        when(customerRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(customers, pageable, 2));

        PageResponse<CustomerDto> pageResponse = customerService.findAll(pageable);

        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.size()).isEqualTo(20);
        assertThat(pageResponse.totalElements()).isEqualTo(2);
        assertThat(pageResponse.content().get(0).getTaxcode()).isEqualTo("MRARSS79D26T215V");

    }

    @Test
    public void createCustomerTest(){
        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest("Andrea", "Rossi", LocalDate.of(1979,04,21),"NDRRSS79D26T215V", "email");

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation ->{
            Customer customerReceived = invocation.getArgument(0);
            assertThat(customerReceived.getId()).isNull();
            customerReceived.setId(1L);
            return customerReceived;
        });

        CustomerDto customerDto = customerService.createCustomer(createCustomerRequest);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());

        assertThat(customerDto.getTaxcode()).isEqualTo(createCustomerRequest.taxcode());
    }


    @Test
    public void findCustomerByIdOkTest(){
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer result = customerService.findCustomerEntityById(1L);

        assertThat(result).isSameAs(customer);
    }

    @Test
    void findCustomerEntityNotFoundTest() {
        when(customerRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findCustomerEntityById(3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("3");
    }

    private Customer customer(Long id, String first, String last, String taxcode,String email) {
        Customer c = new Customer(first,last,LocalDate.of(1979,04,21),taxcode,email);
        c.setId(id);
        return c;
    }

}
