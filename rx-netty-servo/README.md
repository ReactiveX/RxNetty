# RxNetty Servo Plugin

This plugin provides metrics support for RxNetty using [servo](https://github.com/Netflix/servo)
 
### Usage
 
 
 ```java
 
 import io.reactivex.netty.RxNetty;
 import io.reactivex.netty.servo.ServoEventsListenerFactory;
 
 public class MyApplication {
 
     public static void main(String[] args) {
         RxNetty.useMetricListenersFactory(new ServoEventsListenerFactory());
     }
 }
 
 ```
 
 The above code will configure RxNetty to send metric related events to the servo implementation provided in this 
 module.