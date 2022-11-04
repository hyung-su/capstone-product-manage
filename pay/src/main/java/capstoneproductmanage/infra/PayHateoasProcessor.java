package capstoneproductmanage.infra;
import capstoneproductmanage.domain.*;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import org.springframework.hateoas.EntityModel;

@Component
public class PayHateoasProcessor implements RepresentationModelProcessor<EntityModel<Pay>>  {

    @Override
    public EntityModel<Pay> process(EntityModel<Pay> model) {

        
        return model;
    }
    
}
