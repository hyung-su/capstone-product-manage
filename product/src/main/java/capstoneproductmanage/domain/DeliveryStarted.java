package capstoneproductmanage.domain;

import capstoneproductmanage.domain.*;
import capstoneproductmanage.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class DeliveryStarted extends AbstractEvent {

    private Long id;
    private Long orderId;
    private Long address;

    public DeliveryStarted(Product aggregate){
        super(aggregate);
    }
    public DeliveryStarted(){
        super();
    }
}
