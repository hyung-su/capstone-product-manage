package capstoneproductmanage.domain;

import capstoneproductmanage.domain.*;
import capstoneproductmanage.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class PayCanceled extends AbstractEvent {

    private Long id;
    private Long orderId;
    private Double price;
    private String status;

    public PayCanceled(Pay aggregate){
        super(aggregate);
    }
    public PayCanceled(){
        super();
    }
}
