package assignment2Consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import model.LiftRide;
import model.Skier;


public class ConsumerTask implements Runnable {

  private ConcurrentHashMap<Integer, List<LiftRide>> map;

  private final String QUEUE_NAME;
  private CountDownLatch countDownLatch;
  private AtomicInteger consumerMessageNum;
  private Connection connection;

  public ConsumerTask(
      ConcurrentHashMap<Integer, List<LiftRide>> map, String QUEUE_NAME,
      CountDownLatch countDownLatch, AtomicInteger consumerMessageNum,
      Connection connection) {
    this.map = map;
    this.QUEUE_NAME = QUEUE_NAME;
    this.countDownLatch = countDownLatch;
    this.consumerMessageNum = consumerMessageNum;
    this.connection = connection;
  }

  @Override
  public void run() {
    Channel channel;

    try {

      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);
      channel.basicQos(1);
      System.out.println("waiting message");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        Gson gson = new Gson();
        Skier skier = gson.fromJson(message, Skier.class);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

//        map.putIfAbsent(skier.getSkierID(), new CopyOnWriteArrayList<>());
//        map.get(skier.getSkierID()).add(skier.getLiftRide());
//        consumerMessageNum.addAndGet(1);
        //System.out.println(skier.toString() + " No." + consumerMessageNum.toString() + " messages");
        if (map.containsKey(skier.getSkierID())) {
          List<LiftRide> list = map.get(skier.getSkierID());
          //spin lock for wait init list
          while (list == null) {
            list = map.get(skier.getSkierID());
          }
          list.add(skier.getLiftRide());
          map.put(skier.getSkierID(), list);

        } else {
          //here might be concurrent problem,use spin lock spin 10 times
          AtomicInteger spinTimes=new AtomicInteger(0);
          List<LiftRide> list=null;
          while(spinTimes.get()<=10){
            spinTimes.addAndGet(1);
            list=map.get(skier.getSkierID());
            if(list!=null){
              break;
            }
          }
          if(list==null){
            ArrayList<LiftRide> largeList = new ArrayList<>(1000);
            list = new CopyOnWriteArrayList<>(largeList);
          }
          list.add(skier.getLiftRide());
          map.put(skier.getSkierID(), list);
        }
        //consumerMessageNum.addAndGet(1);
      };
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
      });

    } catch (IOException e) {
      throw new RuntimeException("Error on create connection during consumer init");
    }

  }
}
