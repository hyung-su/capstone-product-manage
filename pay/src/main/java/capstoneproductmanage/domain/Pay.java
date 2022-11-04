package capstoneproductmanage.domain;

import capstoneproductmanage.domain.PayApproved;
import capstoneproductmanage.domain.PayCanceled;
import capstoneproductmanage.PayApplication;
import javax.persistence.*;
import java.util.List;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name="Pay_table")
@Data

public class Pay  {

    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    
    
    
    
    
    private Long id;
    
    
    
    
    
    private Long orderId;
    
    
    
    
    
    private Double price;
    
    
    
    
    
    private String status;

    @PostPersist
    public void onPostPersist(){


        PayApproved payApproved = new PayApproved(this);
        payApproved.publishAfterCommit();



        PayCanceled payCanceled = new PayCanceled(this);
        payCanceled.publishAfterCommit();

    }

    public static PayRepository repository(){
        PayRepository payRepository = PayApplication.applicationContext.getBean(PayRepository.class);
        return payRepository;
    }




    public static void cancelPayment(OrderCanceled orderCanceled){

        /** Example 1:  new item 
        Pay pay = new Pay();
        repository().save(pay);

        */

        /** Example 2:  finding and process
        
        repository().findById(orderCanceled.get???()).ifPresent(pay->{
            
            pay // do something
            repository().save(pay);


         });
        */

        
    }


}
