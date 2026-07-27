package interview.ecommerce.exceptions;

public class InsufficientStockException extends DomainException {



    public InsufficientStockException(String productCode, int stock, int requested) {
        super("Insufficient stock for product '%s': requested %d, available %d"
                .formatted(productCode, requested, stock));
    }

}
