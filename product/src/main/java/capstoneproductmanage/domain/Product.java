package capstoneproductmanage.domain;

import capstoneproductmanage.domain.DeliveryStarted;
import capstoneproductmanage.ProductApplication;
import javax.persistence.*;
import java.util.List;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name="Product_table")
@Data

public class Product  {

    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    
    
    
    
    
    private Long id;
    
    
    
    
    
    private Long orderId;
    
    
    
    
    
    private Long address;
    
    
    
    
    
    private Long totalQuantity;

    @PostPersist
    public void onPostPersist(){


        DeliveryStarted deliveryStarted = new DeliveryStarted(this);
        deliveryStarted.publishAfterCommit();

    }

    public static ProductRepository repository(){
        ProductRepository productRepository = ProductApplication.applicationContext.getBean(ProductRepository.class);
        return productRepository;
    }




    public static void orderInfoReceived(PayApproved payApproved){

        /** Example 1:  new item 
        Product product = new Product();
        repository().save(product);

        */

        /** Example 2:  finding and process
        
        repository().findById(payApproved.get???()).ifPresent(product->{
            
            product // do something
            repository().save(product);


         });
        */

        
    }
    public static void orderCancelProcess(PayCanceled payCanceled){

        /** Example 1:  new item 
        Product product = new Product();
        repository().save(product);

        */

        /** Example 2:  finding and process
        
        repository().findById(payCanceled.get???()).ifPresent(product->{
            
            product // do something
            repository().save(product);


         });
        */

        
    }


}
