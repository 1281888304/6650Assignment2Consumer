package assignment2Consumer;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import model.LiftRide;


public class ConsumerMain {

  private static final ConcurrentHashMap<Integer, List<LiftRide>> map=new ConcurrentHashMap<>();
  private final static String QUEUE_NAME = "skier_queue";

  private final static int THREAD_NUM=30;

  private static final AtomicInteger consumerMessageNum=new AtomicInteger(0);

  private static final CountDownLatch countDownLatch=new CountDownLatch(1);


  public static void main(String[] args) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.24.63");
    factory.setPort(5672);
    Connection connection = null;
    try {
      connection = factory.newConnection();
    } catch (IOException e) {
      throw new RuntimeException("Error IO exception out on get connection");
    } catch (TimeoutException e) {
      throw new RuntimeException("Error time out on get connection");
    }

    for(int i=0;i<THREAD_NUM;i++){
      ConsumerTask consumerTask=new ConsumerTask(map,QUEUE_NAME,countDownLatch,consumerMessageNum,connection);
      Thread thread=new Thread(consumerTask);
      thread.start();
    }
    try {
      countDownLatch.await();
      int num=0;
      for(Map.Entry<Integer,List<LiftRide>> entry : map.entrySet()){
        num+=entry.getValue().size();
      }
      System.out.println("total message "+consumerMessageNum.get()+" have been consumer");
      System.out.println("Hash map total message store "+num);
      System.out.println("Map entry size "+map.entrySet().size());
      System.out.println("Map size "+map.size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

}
