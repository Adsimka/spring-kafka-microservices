package producer.event;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductCreatedEvent {

    private String productId;

    private String title;

    private BigDecimal price;

    private Integer quantity;
}
