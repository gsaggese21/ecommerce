package interview.ecommerce.entity;

import interview.ecommerce.exceptions.InsufficientStockException;
import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_code", unique = true)
    private String code;

    @Column
    private String name;

    @Column
    private int stock;


    public Product(String code, String name, int stock) {
        this.code = code;
        this.name = name;
        this.stock = stock;
    }

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }


    public void decreaseStock(int quantity) {
        checkQuantity(quantity);
        if (quantity > this.stock) throw new InsufficientStockException(code, stock, quantity);
        this.stock -= quantity;
    }

    public void increaseStock(int quantity){
        checkQuantity(quantity);
        this.stock += quantity;
    }

    private void checkQuantity(int quantity){
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }
}
