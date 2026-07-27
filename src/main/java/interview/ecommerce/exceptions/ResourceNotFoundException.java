package interview.ecommerce.exceptions;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super("%s with identifier '%s' doesn't exists".formatted(resourceType, identifier));
    }

    public static ResourceNotFoundException product(String code) {
        return new ResourceNotFoundException("Product", code);
    }

    public static ResourceNotFoundException order(Long orderId) {
        return new ResourceNotFoundException("Order", orderId);
    }

    public static ResourceNotFoundException customer(Long customerId) {
        return new ResourceNotFoundException("Customer", customerId);
    }

}
