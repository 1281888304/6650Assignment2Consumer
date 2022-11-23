package assignment2Consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import model.Comment;
import model.LiftRide;
import model.Skier;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class RedisConsumerTask implements Runnable {

  private final String QUEUE_NAME;

  private Connection connection;

  private JedisPool pool;

  public RedisConsumerTask(String QUEUE_NAME, Connection connection,
      JedisPool pool) {
    this.QUEUE_NAME = QUEUE_NAME;
    this.connection = connection;
    this.pool = pool;
  }

  @Override
  public void run() {
    Channel channel;

    try {

      channel = connection.createChannel();
      String exchangeName = "fanout_exchange";
      channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT);
      String name = QUEUE_NAME + "2";
      channel.queueDeclare(name, false, false, false, null);
      channel.queueBind(name, exchangeName, "");
      channel.basicQos(1);
      System.out.println("waiting message");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        Gson gson = new Gson();
        Skier skier = gson.fromJson(message, Skier.class);
        System.out.println("get skier object");
        //System.out.println("get object from rabbit mq " +skier);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        //System.out.println("success consume");
        //put it to the list
        addSkierToRedisList(pool, message, skier);

        //handle first problem
        handleSessionDays(pool, message, skier);
        //System.out.println("hadnle the first problem");

        //handle second problem
        handleTotalVerticalDays(pool,message,skier);
        //System.out.println("handle second ");


        //handle 3th problem
        handleLiftRode(pool,message,skier);
        System.out.println("handle 3th");

        //4th problem
        handleUniqueSkier(pool,message,skier);
        //System.out.println("handle the 4th problem");

        //successful handle the data, send the ack back to provider

      };
      channel.basicConsume(QUEUE_NAME+"2", false, deliverCallback, consumerTag -> {
      });

    } catch (IOException e) {
      throw new RuntimeException("Error on create connection during consumer init");
    }

  }

  /**
   * add all the skier object to the redis
   *
   * @param pool    jedis pool
   * @param message the json of skier object
   * @param skier   skier object
   */
  public void addSkierToRedisList(JedisPool pool, String message, Skier skier) {
    //borrow jedis from pool
    Jedis jedis = pool.getResource();
    try {
      //use skier id as the key, put the json object to the list
      jedis.lpush(skier.getSkierID() + "", message);
    } catch (JedisException e) {
      if (null != jedis) {
        pool.returnBrokenResource(jedis);
        jedis = null;
      }
    } finally {
      //return jedis to the pool
      if (jedis != null) {
        pool.returnResource(jedis);
      }

    }
  }


  /**
   * handle the problem "For skier N, how many days have they skied this season?" use skier+session
   * id as the key,if contains, then increase it
   *
   * @param pool    jedis pool
   * @param message the json of skier object
   * @param skier   skier object
   */
  public void handleSessionDays(JedisPool pool, String message, Skier skier) {
    String key = Comment.SESSION_DAYS +"_"+ skier.getSkierID() +"_"+ skier.getSeasonID();

    //borrow jedis from pool
    Jedis jedis = pool.getResource();
    try {
      //use skier id as the key, put the json object to the list
      if (jedis.exists(key)) {
        jedis.incr(key);
      } else {
        jedis.set(key, 1 + "");
      }
    } catch (JedisException e) {
      if (null != jedis) {
        pool.returnBrokenResource(jedis);
        jedis = null;
      }
    } finally {
      //return jedis to the pool
      if (jedis != null) {
        pool.returnResource(jedis);
      }

    }
  }

  /**
   * For skier N, what are the vertical totals for each ski day?” (calculate vertical as liftID*10)
   *
   * @param pool    jedis pool
   * @param message skier object
   * @param skier   skier
   */
  public void handleTotalVerticalDays(JedisPool pool, String message, Skier skier) {
    String key = Comment.VERTICAL_TOTAL + skier.getSkierID() + skier.getDayID();
    Jedis jedis = pool.getResource();
    try {
      //use skier id as the key, put the json object to the list
      if (jedis.exists(key)) {

        jedis.incrBy(key, skier.getLiftRide().getLiftID() * 10);
      } else {
        jedis.set(key, skier.getLiftRide().getLiftID() * 10 + "");
      }
    } catch (JedisException e) {
      if (null != jedis) {
        pool.returnBrokenResource(jedis);
        jedis = null;
      }
    } finally {
      //return jedis to the pool
      if (jedis != null) {
        pool.returnResource(jedis);
      }

    }
  }

  /**
   * For skier N, show me the lifts they rode on each ski day
   *
   * @param pool    jedis pool
   * @param message skier object
   * @param skier   skier
   */
  public void handleLiftRode(JedisPool pool, String message, Skier skier) {
    String key = Comment.LIFT_RODE +"_"+ skier.getSkierID()+"_";
    Jedis jedis = pool.getResource();
    try {
      //use skier id as the key, put the json object to the list
      if (jedis.exists(key)) {

        Map<String, String> map = jedis.hgetAll(key);
        map.put(skier.getDayID(),skier.getLiftRide().getLiftID()+"_"+skier.getLiftRide().getTime());
        jedis.hmset(key,map);

      } else {
        //only one message per consume, so it is thread safe
        Map<String,String> map=new HashMap<>();
        map.put(skier.getDayID(),skier.getLiftRide().getLiftID()+"_"+skier.getLiftRide().getTime());
        jedis.hmset(key,map);
      }
    } catch (JedisException e) {
      if (null != jedis) {
        pool.returnBrokenResource(jedis);
        jedis = null;
      }
    } finally {
      //return jedis to the pool
      if (jedis != null) {
        pool.returnResource(jedis);
      }

    }
  }


  /**
   * For skier N, what are the vertical totals for each ski day?” (calculate vertical as liftID*10)
   *
   * @param pool    jedis pool
   * @param message skier object
   * @param skier   skier
   */
  public void handleUniqueSkier(JedisPool pool, String message, Skier skier) {
    String key = Comment.UNIQUE_SKIERS +"_day_"+ skier.getDayID();
    Jedis jedis = pool.getResource();
    try {
      //use skier id as the key, put the json object to the list
      if (jedis.exists(key)) {

        jedis.incrBy(key, skier.getLiftRide().getLiftID() * 10);
      } else {
        jedis.set(key, skier.getLiftRide().getLiftID() * 10 + "");
      }
    } catch (JedisException e) {
      if (null != jedis) {
        pool.returnBrokenResource(jedis);
        jedis = null;
      }
    } finally {
      //return jedis to the pool
      if (jedis != null) {
        pool.returnResource(jedis);
      }

    }
  }


}
