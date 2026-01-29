import java.time.Instant;

public class Sale {
    private Integer id;
    private Instant creationDatetime;
    private Order order;

    public Sale(Integer id, Instant creationDatetime, Order order) {
        this.id = id;
        this.creationDatetime = creationDatetime;
        this.order = order;
    }


    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }
    public void setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
    }

    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }


}
