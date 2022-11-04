package capstoneproductmanage.domain;

import capstoneproductmanage.domain.*;
import capstoneproductmanage.infra.AbstractEvent;
import java.util.*;
import lombok.*;

@Data
@ToString
public class Ordered extends AbstractEvent {

    private Long id;
    private String item;
    private Integer quantity;
    private String status;
    private Long price;

    public Ordered(Order aggregate){
        super(aggregate);
    }
    public Ordered(){
        super();
    }
}
