package capstoneproductmanage.domain;

import capstoneproductmanage.domain.*;
import capstoneproductmanage.infra.AbstractEvent;
import lombok.*;
import java.util.*;
@Data
@ToString
public class PayApproved extends AbstractEvent {

    private Long id;
    private Long orderId;
    private Double price;
    private String status;
}


