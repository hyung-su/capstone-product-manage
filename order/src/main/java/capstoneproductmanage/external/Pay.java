package capstoneproductmanage.external;

import lombok.Data;
import java.util.Date;
@Data
public class Pay {

    private Long id;
    private Long orderId;
    private Double price;
    private String status;
}


