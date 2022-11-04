package capstoneproductmanage.domain;

import capstoneproductmanage.domain.*;
import capstoneproductmanage.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class OrderCanceled extends AbstractEvent {

    private Long id;
    private String item;
    private Integer orderQty;
    private String status;
    private Long price;

    public OrderCanceled(Order aggregate){
        super(aggregate);
    }
    public OrderCanceled(){
        super();
    }
}
