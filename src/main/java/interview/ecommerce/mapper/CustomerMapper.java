package interview.ecommerce.mapper;

import interview.ecommerce.dto.CreateCustomerRequest;
import interview.ecommerce.dto.CustomerDto;
import interview.ecommerce.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerMapper {

    public Customer toEntityFromCreateRequest(CreateCustomerRequest request) {
        Customer c = new Customer();
        c.setFirstName(request.firstName());
        c.setLastName(request.lastName());
        c.setBirthDate(request.birthDate());
        c.setTaxcode(request.taxcode());
        c.setEmail(request.email());

        return c;
    }

    public CustomerDto toDto(Customer customer){
        CustomerDto customerDto = new CustomerDto();
        customerDto.setFirstName(customer.getFirstName());
        customerDto.setLastName(customer.getLastName());
        customerDto.setBirthDate(customer.getBirthDate());
        customerDto.setTaxcode(customer.getTaxcode());
        customerDto.setId(customer.getId());
        customerDto.setEmail(customer.getEmail());

        return customerDto;
    }


}
