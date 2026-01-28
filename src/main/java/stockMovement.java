import java.time.Instant;

public class stockMovement {
    private Integer id;
    private Double quantity;
    private String unit;
    private Instant movementDate;

    public StockMovement(Integer id, Double quantity, String unit, Instant movementDate) {
        this.id = id;
        this.quantity = quantity;
        this.unit = unit;
        this.movementDate = movementDate;
    }

    public Integer getId() {
        return id;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Instant getMovementDate() {
        return movementDate;
    }
}
