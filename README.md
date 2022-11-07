
# 제품배송 - Capstone Project

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [제품배송](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

기능적 요구사항
1. 고객이 상품을 선택하여 주문한다
1. 고객이 결제한다
1. 주문이 되면 주문 내역이 상품팀에게 전달된다
1. 상품팀이 주문 내역을 확인하여 배송 출발한다
1. 고객이 주문을 취소할 수 있다
1. 주문이 취소되면 배송이 취소된다
1. 고객이 주문상태를 중간중간 조회한다
1. 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다
1. 배송이 완료되면 상품의 재고량이 감소한다.


비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다. (Sync 호출) 
1. 장애격리
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  (Circuit breaker, fallback)
1. 성능
    1. 고객이 자주 상점관리에서 확인할 수 있는 배달상태를 주문시스템(프론트엔드)에서 확인할 수 있어야 한다  (CQRS)
    1. 배달상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  (Event driven)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
  https://labs.msaez.io/#/storming/n1YnKFppadMPlnAVQffdPCgX2XG3/9dcb4885bd50a42876dfd04be9fe5a66

### 완성 모형
![image](https://user-images.githubusercontent.com/113887798/200229290-c58083ce-20a1-4787-938e-2fa65e567c37.png)

# 구현:

Event Storming 을 통해 도출된 아키텍처에 따라, 각 Bounded Context 별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 
구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd order
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd product
mvn spring-boot:run  

cd notice
mvn spring-boot:run  

cd customercenter
mvn spring-boot:run  
```

## 1. 비동기식 호출(Pub/Sub)
주문이 완료 되었을때 알림 서비스로 이를 알려주는 행위는 동기식이 아니라 비동기식으로 처리한다.
 
- 이를 위하여 주문이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
``` 
package capstoneproductmanage.domain;

@Entity
@Table(name="Order_table")
@Data
public class Order  {

 ...
   @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered(this);
        ordered.publishAfterCommit();

    }
...
}
```
- 알림(notice) 서비스에서는 주문 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package capstoneproductmanage.infra;

...

@Service
public class PolicyHandler{

    @StreamListener(value=KafkaProcessor.INPUT, condition="headers['type']=='DeliveryStarted'")
    public void wheneverDeliveryStarted_KakaoNotice(@Payload DeliveryStarted deliveryStarted){
        DeliveryStarted event = deliveryStarted;
        System.out.println("\n\n##### listener KakaoNotice : " + deliveryStarted + "\n\n");
    }
}

```


- 알림 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 알림 시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다.
```
# 알림 서비스(notice) 를 잠시 내려놓음 (ctrl+c)

#주문처리 (2건)
$ http POST localhost:8081/orders item=TV-Pub/Sub#1 orderQty=1 price=10 status=0 #Success
HTTP/1.1 201 
Connection: keep-alive
Content-Type: application/json
Date: Mon, 07 Nov 2022 04:44:36 GMT
Keep-Alive: timeout=60
Location: http://localhost:8081/orders/12
Transfer-Encoding: chunked
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/12"
        },
        "self": {
            "href": "http://localhost:8081/orders/12"
        }
    },
    "item": "TV-Pub/Sub#1",
    "orderQty": 1,
    "price": 10.0,
    "status": "0"
}

$ http POST localhost:8081/orders item=TV-Pub/Sub#2 orderQty=1 price=10 status=0 #Success
HTTP/1.1 201 
Connection: keep-alive
Content-Type: application/json
Date: Mon, 07 Nov 2022 04:44:41 GMT
Keep-Alive: timeout=60
Location: http://localhost:8081/orders/13
Transfer-Encoding: chunked
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/13"
        },
        "self": {
            "href": "http://localhost:8081/orders/13"
        }
    },
    "item": "TV-Pub/Sub#2",
    "orderQty": 1,
    "price": 10.0,
    "status": "0"
} 

#알림 서비스 기동
cd notice
mvn spring-boot:run

#알림 서비스 로그확인
##### listener KakaoNotice : OrderCanceled(id=12, item=TV-Pub/Sub#1, orderQty=1, status=0, price=null)
##### listener KakaoNotice : OrderCanceled(id=13, item=TV-Pub/Sub#2, orderQty=1, status=0, price=null)
```

## 2. CQRS
트랜잭션과 분리된 별도 ReadModel을 생성해서 시스템 성능과 안정성을 확보한다.
- MyPage 모델 생성 후 Ordered 이벤트 발생시 MyPage 모델에 자동반영되도록 한 후
  실제 Ordered 이벤트 발생 후 Mypages에 반영되는지 확인한다.
  
  gitpod /workspace/capstone-product-manage/customercenter (main) $ mvn spring-boot:run
  gitpod /workspace/capstone-product-manage (main) $ http POST localhost:8081/orders item="CQR111" quantity=111 status="st1" price=111
  {
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "item": "CQR111",
    "price": 111.0,
    "quantity": 111,
    "status": "st1"
}

   "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "item": "CQR111",
    "price": 111.0,
    "quantity": 111,
    "status": "st1"
}


gitpod /workspace/capstone-product-manage (main) $ http :8085/myPages
{
    "_embedded": {
        "myPages": [
            {
                "_links": {
                    "myPage": {
                        "href": "http://localhost:8085/myPages/1"
                    },
                    "self": {
                        "href": "http://localhost:8085/myPages/1"
                    }
                },
                "item": "CQR111",
                "quantity": 111,
                "status": "st1"
            }
        ]
    },

          
  
![image](https://user-images.githubusercontent.com/112880199/200234797-f1af247d-7cea-484a-b468-23a30b2e8c8b.png)

## 4. 동기식 호출(Request/Response) 

분석단계에서의 조건 중 하나로 주문(app)->결제(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (order) PayService.java

package capstoneproductmanage.external;

@FeignClient(name = "pay", url = "${api.url.pay}")
public interface PayService {
    @RequestMapping(method= RequestMethod.POST, path="/pays")
    public void approvePayment(@RequestBody Pay pay);
}
```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){

        capstoneproductmanage.external.Pay pay = new capstoneproductmanage.external.Pay();
        // mappings goes here
        OrderApplication.applicationContext.getBean(capstoneproductmanage.external.PayService.class)
            .approvePayment(pay);

        Ordered ordered = new Ordered(this);
        ordered.publishAfterCommit();

        OrderCanceled orderCanceled = new OrderCanceled(this);
        orderCanceled.publishAfterCommit();

    }
```
- 동기식 호출 정상 확인
```
#order 서비스 호출 (주문 생성) 시 pay 신규 생성 확인
$ http POST  localhost:8081/orders item=tv orderQty=3 status=0 price=1000
HTTP/1.1 201 
Connection: keep-alive
Content-Type: application/json
Date: Mon, 07 Nov 2022 01:41:23 GMT
Keep-Alive: timeout=60
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "item": "tv",
    "orderQty": 3,
    "price": 1000.0,
    "status": "0"
}

$ http GET  localhost:8083/pays/1
HTTP/1.1 200 
Connection: keep-alive
Content-Type: application/hal+json
Date: Mon, 07 Nov 2022 01:41:33 GMT
Keep-Alive: timeout=60
Transfer-Encoding: chunked
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{
    "_links": {
        "pay": {
            "href": "http://localhost:8083/pays/1"
        },
        "self": {
            "href": "http://localhost:8083/pays/1"
        }
    },
    "orderId": 1,
    "price": 1000.0,
    "status": "0"
}

```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:

```
#pay 서비스를 잠시 내려놓음 (ctrl+c)

#주문처리
http POST localhost:8081/orders item=TV orderQty=2 price=100   #Fail

#pay 서비스 재기동
cd pay
mvn spring-boot:run

#주문처리
http POST localhost:8081/orders item=TV orderQty=2 price=100   #Success
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)

## 5. 장애격리(Circuit Breaker) 
      
- 동기식 호출에 대한 장애 전파 차단을 구현한다. 주문 생성 시 Pay 서비스가 정상적이지 않은 경우 fallback 처리를 구현한다. (현재는 메세지만 처리)

```
package capstoneproductmanage.external;

@FeignClient(name = "pay", url = "${api.url.pay}", fallback = PayServiceFallback.class)
```

- PayServiceFallback 구현체 구현
```
package capstoneproductmanage.external;

@Service
public class PayServiceFallback implements PayService{
    public void approvePayment(Pay pay){
        System.out.println("PayService is Not Available. orderId = "+ pay.getOrderId() );
    }
}
```
- 결과확인 (Pay Service Down 상태에서 Order 생성 호출 시)
```
2022-11-07 02:31:42.416 DEBUG [order,e9e2c37997aedb16,e33afac526ecee00,true] 67258 --- [  hystrix-pay-2] o.s.c.s.i.w.c.feign.TracingFeignClient   : Handled receive of RealSpan(e9e2c37997aedb16/c87bf1a51854d89e)
PayService is Not Available. orderId = 2
```


## 6. Gateway
      
- API Gateway를 통하여 마이크로 서비스들의 진입점을 통일한다.
- Spring Cloud Gateway 에 아래와 같이 설정 한 후 서비스를 기동한다.

```
#Spring Cloud Gateway Config (application.yml)
spring:
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**, 
        - id: product
          uri: http://localhost:8082
          predicates:
            - Path=/products/**, 
        - id: pay
          uri: http://localhost:8083
          predicates:
            - Path=/pays/**, 
        - id: notice
          uri: http://localhost:8084
          predicates:
            - Path=, 
        - id: customercenter
          uri: http://localhost:8085
          predicates:
            - Path=, /myPages/**
            
# 서비스기동
cd gateway
mvn spring-boot:run

# Gateway 가 8088 포트로 기동됨
2022-11-07 05:24:48.337  INFO 41334 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port(s): 8088

```

- 아래와 같이 서비스를 호출한다. (Service Port 호출, Gateway Port 호출)
```
# 1. Order 서비스 호출 (Service Port 8081 이용하여 호출)
$ http POST localhost:8081/orders item=TV-ServicePort orderQty=2 price=100 status=0 #Success
HTTP/1.1 201 
Connection: keep-alive
Content-Type: application/json
Date: Mon, 07 Nov 2022 05:30:29 GMT
Keep-Alive: timeout=60
Location: http://localhost:8081/orders/23
Transfer-Encoding: chunked
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/23"
        },
        "self": {
            "href": "http://localhost:8081/orders/23"
        }
    },
    "item": "TV-ServicePort",
    "orderQty": 2,
    "price": 100.0,
    "status": "0"
}

# 2. Order 서비스 호출 (Gateway Port 8088 이용하여 호출)
 $ http POST localhost:8088/orders item=TV-GatewayPort orderQty=2 price=100 status=0 #Success
HTTP/1.1 201 Created
Content-Type: application/json
Date: Mon, 07 Nov 2022 05:31:37 GMT
Location: http://localhost:8081/orders/24
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/24"
        },
        "self": {
            "href": "http://localhost:8081/orders/24"
        }
    },
    "item": "TV-GatewayPort",
    "orderQty": 2,
    "price": 100.0,
    "status": "0"
}
```

# 운영

## 7. Deploy / Pipeline


- 각 서비스 Image 는 Repository(Docker Hub)에 저장하였다.
![image](https://user-images.githubusercontent.com/113887798/200247609-b5d3694e-b526-4869-9891-bba709f055e8.png)


- deployment.yaml 파일을 아래와 같이 구성하여, K8S 에 배포하였다.
```
#deployment.yaml (customercenter)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customercenter
  labels:
    app: customercenter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: customercenter
  template:
    metadata:
      labels:
        app: customercenter
    spec:
      containers:
        - name: customercenter
          image: agnesjh/customercenter:t2
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

![image](https://user-images.githubusercontent.com/113887798/200248188-5365aa92-f8d9-4971-b654-ffc2f8947abe.png)

## 8.Autoscale (HPA)
사용자의 요청이 많이 들어올 경우 Auto Scale-Out 설정을 통하여 서비스를 동적으로 확장시킨다. 


- order 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 50%프로를 넘어서면 replica 를 3개까지 늘려준다.
```
kubectl autoscale deployment order --cpu-percent=50 --min=1 --max=3
```
- 테스트를 위해 seige 명령으로 부하를 준다.
```
siege -c20 -t40S -v http://order:8080/orders
```
- 서비스 호출 증가에 따라 CPU 값이 늘어나는 것을 확인할 수 있다.
![image](https://user-images.githubusercontent.com/113887798/200251974-7699fc66-fba9-444c-accb-1b25844eec19.png)

- 모니터링 결과 서비스 호출 증가에 따라서 Pod 가 증가했다가 (최대 3개) 시간이 흐름에 따라 다시 줄어드는 것을 확인할 수 있다.
![image](https://user-images.githubusercontent.com/113887798/200252069-fdab787e-6556-4d43-83ed-9d8a069d3df8.png)

- siege 로그 
```
Lifting the server siege...
Transactions:                  12509 hits
Availability:                 100.00 %
Elapsed time:                  39.65 secs
Data transferred:               3.59 MB
Response time:                  0.06 secs
Transaction rate:             315.49 trans/sec
Throughput:                     0.09 MB/sec
Concurrency:                   18.72
Successful transactions:       12515
Failed transactions:               0
Longest transaction:            0.79
Shortest transaction:           0.00
```


## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c100 -t120S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"item": "chicken"}'

** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
:

```

- 새버전으로의 배포 시작
```
kubectl set image ...
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```
Transactions:		        3078 hits
Availability:		       70.45 %
Elapsed time:		       120 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02

```
배포기간중 Availability 가 평소 100%에서 70% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:


kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:
```
Transactions:		        3078 hits
Availability:		       100 %
Elapsed time:		       120 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02

```

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


# 신규 개발 조직의 추가

  ![image](https://user-images.githubusercontent.com/487999/79684133-1d6c4300-826a-11ea-94a2-602e61814ebf.png)


## 마케팅팀의 추가
    - KPI: 신규 고객의 유입률 증대와 기존 고객의 충성도 향상
    - 구현계획 마이크로 서비스: 기존 customer 마이크로 서비스를 인수하며, 고객에 음식 및 맛집 추천 서비스 등을 제공할 예정

## 이벤트 스토밍 
    ![image](https://user-images.githubusercontent.com/487999/79685356-2b729180-8273-11ea-9361-a434065f2249.png)


## 헥사고날 아키텍처 변화 

![image](https://user-images.githubusercontent.com/487999/79685243-1d704100-8272-11ea-8ef6-f4869c509996.png)

## 구현  

기존의 마이크로 서비스에 수정을 발생시키지 않도록 Inbund 요청을 REST 가 아닌 Event 를 Subscribe 하는 방식으로 구현. 기존 마이크로 서비스에 대하여 아키텍처나 기존 마이크로 서비스들의 데이터베이스 구조와 관계없이 추가됨. 

## 운영과 Retirement

Request/Response 방식으로 구현하지 않았기 때문에 서비스가 더이상 불필요해져도 Deployment 에서 제거되면 기존 마이크로 서비스에 어떤 영향도 주지 않음.

* [비교] 결제 (pay) 마이크로서비스의 경우 API 변화나 Retire 시에 app(주문) 마이크로 서비스의 변경을 초래함:

예) API 변화시
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){

        fooddelivery.external.결제이력 pay = new fooddelivery.external.결제이력();
        pay.setOrderId(getOrderId());
        
        Application.applicationContext.getBean(fooddelivery.external.결제이력Service.class)
                .결제(pay);

                --> 

        Application.applicationContext.getBean(fooddelivery.external.결제이력Service.class)
                .결제2(pay);

    }
```

예) Retire 시
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){

        /**
        fooddelivery.external.결제이력 pay = new fooddelivery.external.결제이력();
        pay.setOrderId(getOrderId());
        
        Application.applicationContext.getBean(fooddelivery.external.결제이력Service.class)
                .결제(pay);

        **/
    }
```


