package interview.ecommerce.controller;


import interview.ecommerce.dto.CreateCustomerRequest;
import interview.ecommerce.dto.CustomerDto;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.exceptions.GlobalExceptionHandler;
import interview.ecommerce.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final String TAXCODE = "RSSMRA85M01H501Z";

    private static final String VALID_BODY = """
            {
              "first_name": "Mario",
              "last_name": "Rossi",
              "birth_date": "1985-08-01",
              "taxcode": "RSSMRA85M01H501Z",
              "email": "mario.rossi@mail.com"
            }
            """;

    @Test
    public void createCustomer201Test() throws Exception {
        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(customerDto());

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.first_name").value("Mario"))
                .andExpect(jsonPath("$.last_name").value("Rossi"))
                .andExpect(jsonPath("$.birth_date").value("1985-08-01"))
                .andExpect(jsonPath("$.taxcode").value(TAXCODE));
    }

    @Test
    public void createCustomerDeserializesRequestTest() throws Exception {
        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(customerDto());

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateCustomerRequest> requestCaptor = ArgumentCaptor.forClass(CreateCustomerRequest.class);
        verify(customerService).createCustomer(requestCaptor.capture());
        CreateCustomerRequest received = requestCaptor.getValue();

        assertThat(received.firstName()).isEqualTo("Mario");
        assertThat(received.lastName()).isEqualTo("Rossi");
        assertThat(received.birthDate()).isEqualTo(LocalDate.of(1985, 8, 1));
        assertThat(received.taxcode()).isEqualTo(TAXCODE);
        assertThat(received.email()).isEqualTo("mario.rossi@mail.com");
    }

    @Test
    public void createCustomerMissingTaxcode400Test() throws Exception {
        String body = """
                { "first_name": "Mario", "last_name": "Rossi", "birth_date": "1985-08-01" }
                """;

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    public void createCustomerMalformedTaxcode400Test() throws Exception {
        String body = """
                { "first_name": "Mario", "last_name": "Rossi", "taxcode": "rssmra85m01h501z" }
                """;

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    public void createCustomerTaxcodeWrongLength400Test() throws Exception {
        String body = """
                { "first_name": "Mario", "last_name": "Rossi", "taxcode": "RSSMRA85M01H501" }
                """;

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    public void createCustomerInvalidDateFormat400Test() throws Exception {
        String body = """
                {
                  "first_name": "Mario",
                  "last_name": "Rossi",
                  "birth_date": "01/08/1985",
                  "taxcode": "RSSMRA85M01H501Z"
                }
                """;

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    public void findAll200Test() throws Exception {
        when(customerService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(customerDto()), 0, 20, 1, 1, true));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].first_name").value("Mario"))
                .andExpect(jsonPath("$.content[0].last_name").value("Rossi"))
                .andExpect(jsonPath("$.content[0].taxcode").value("RSSMRA85M01H501Z"));
    }

    @Test
    public void findAllEmpty200Test() throws Exception {
        when(customerService.findAll(any(Pageable.class))).thenReturn(new PageResponse<>(List.of(), 0, 20, 1, 1, true));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private CustomerDto customerDto() {
        CustomerDto dto = new CustomerDto();
        dto.setId(1L);
        dto.setFirstName("Mario");
        dto.setLastName("Rossi");
        dto.setBirthDate(LocalDate.of(1985, 8, 1));
        dto.setTaxcode(TAXCODE);
        dto.setEmail("mario.rossi@mail.com");
        return dto;
    }
}

