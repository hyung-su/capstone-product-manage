package capstoneproductmanage.domain;

import capstoneproductmanage.domain.Ordered;
import capstoneproductmanage.domain.OrderCanceled;
import capstoneproductmanage.OrderApplication;
import javax.persistence.*;
import java.util.List;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name="Order_table")
@Data

public class Order  {

    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    
    
    
    
    
    private Long id;
    
    
    
    
    
    private String item;
    
    
    
    
    
    private Integer orderQty;
    
    
    
    
    
    private String status;
    
    
    
    
    
    private Long price;

    @PostPersist
    public void onPostPersist(){

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.


        capstoneproductmanage.external.Pay pay = new capstoneproductmanage.external.Pay();
        // mappings goes here
        OrderApplication.applicationContext.getBean(capstoneproductmanage.external.PayService.class)
            .approvePayment(pay);


        Ordered ordered = new Ordered(this);
        ordered.publishAfterCommit();



        OrderCanceled orderCanceled = new OrderCanceled(this);
        orderCanceled.publishAfterCommit();

    }
    @PreRemove
    public void onPreRemove(){
    }

    public static OrderRepository repository(){
        OrderRepository orderRepository = OrderApplication.applicationContext.getBean(OrderRepository.class);
        return orderRepository;
    }




    public static void orderStatusModify(DeliveryStarted deliveryStarted){

        /** Example 1:  new item 
        Order order = new Order();
        repository().save(order);

        */

        /** Example 2:  finding and process
        
        repository().findById(deliveryStarted.get???()).ifPresent(order->{
            
            order // do something
            repository().save(order);


         });
        */

        
    }


}
