package capstoneproductmanage.domain;

import javax.persistence.*;
import java.util.List;
import java.util.Date;
import lombok.Data;

@Entity
@Table(name="MyPage_table")
@Data
public class MyPage {

        @Id
        //@GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Integer orderId;
        private Integer item;
        private Integer status;
        private Integer quantity;


}