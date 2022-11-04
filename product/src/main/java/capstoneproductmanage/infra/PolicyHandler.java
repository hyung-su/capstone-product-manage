package capstoneproductmanage.infra;

import javax.naming.NameParser;

import javax.naming.NameParser;
import javax.transaction.Transactional;

import capstoneproductmanage.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import capstoneproductmanage.domain.*;


@Service
@Transactional
public class PolicyHandler{
    @Autowired ProductRepository productRepository;
    
    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}

    @StreamListener(value=KafkaProcessor.INPUT, condition="headers['type']=='PayApproved'")
    public void wheneverPayApproved_OrderInfoReceived(@Payload PayApproved payApproved){

        PayApproved event = payApproved;
        System.out.println("\n\n##### listener OrderInfoReceived : " + payApproved + "\n\n");


        

        // Sample Logic //
        Product.orderInfoReceived(event);
        

        

    }

    @StreamListener(value=KafkaProcessor.INPUT, condition="headers['type']=='PayCanceled'")
    public void wheneverPayCanceled_OrderCancelProcess(@Payload PayCanceled payCanceled){

        PayCanceled event = payCanceled;
        System.out.println("\n\n##### listener OrderCancelProcess : " + payCanceled + "\n\n");


        

        // Sample Logic //
        Product.orderCancelProcess(event);
        

        

    }

}


