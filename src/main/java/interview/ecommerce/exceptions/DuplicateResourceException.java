package interview.ecommerce.exceptions;

public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String resourceType, Object identifier) {
        super("%s with identifier '%s' already exists".formatted(resourceType, identifier));
    }

    public static DuplicateResourceException product(String code) {
        return new DuplicateResourceException("Product", code);
    }

}
