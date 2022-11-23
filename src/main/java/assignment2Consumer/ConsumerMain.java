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
import redis.clients.jedis.JedisPool;


public class ConsumerMain {

  private static final ConcurrentHashMap<Integer, List<LiftRide>> map=new ConcurrentHashMap<>();
  //private final static String QUEUE_NAME = "skier_queue";
  private final static String QUEUE_NAME = "queue";
  private final static int THREAD_NUM=150;

  private static final AtomicInteger consumerMessageNum=new AtomicInteger(0);

  private static final CountDownLatch countDownLatch=new CountDownLatch(1);


  public static void main(String[] args) {
    ConnectionFactory factory = new ConnectionFactory();
    //factory.setHost("172.31.24.63");
    factory.setHost("52.25.17.217");
    factory.setPort(5672);
    Connection connection = null;
    try {
      connection = factory.newConnection();
    } catch (IOException e) {
      throw new RuntimeException("Error IO exception out on get connection");
    } catch (TimeoutException e) {
      throw new RuntimeException("Error time out on get connection");
    }

    //get Redis pool
    //JedisPool pool = new JedisPool("172.31.7.157", 6379);
    JedisPool pool = new JedisPool("35.92.11.27", 6379);

    for(int i=0;i<THREAD_NUM;i++){
      //ConsumerTask task=new ConsumerTask(map,QUEUE_NAME,countDownLatch,consumerMessageNum,connection);
      RedisConsumerTask task=new RedisConsumerTask(QUEUE_NAME,connection,pool);
      Thread thread=new Thread(task);
      thread.start();
    }


  }

}
