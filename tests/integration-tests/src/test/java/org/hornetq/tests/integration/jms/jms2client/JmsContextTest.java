/**
 *
 */
package org.hornetq.tests.integration.jms.jms2client;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hornetq.tests.util.JMSTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JmsContextTest extends JMSTestBase
{

   private JMSContext context;
   private final Random random = new Random();
   private Queue queue1;

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      context = createContext();
      queue1 = createQueue(JmsContextTest.class.getSimpleName() + "Queue");
   }

   @Test
   public void testCreateContext()
   {
      Assert.assertNotNull(context);
   }

   @Test
   public void testRollbackTest()
   {
      JMSContext ctx =  addContext(cf.createContext(JMSContext.SESSION_TRANSACTED));

      JMSProducer producer = ctx.createProducer();
      JMSConsumer cons = ctx.createConsumer(queue1);

      producer.send(queue1, context.createTextMessage("hello"));

      ctx.rollback();

      assertNull(cons.receiveNoWait());

      producer.send(queue1, context.createTextMessage("hello"));

      ctx.commit();

      assertNotNull(cons.receiveNoWait());

      ctx.commit();

      ctx.rollback();

      assertNull(cons.receiveNoWait());

      cons.close();


   }

   @Test
   public void testDupsOK()
   {
      JMSContext ctx =  addContext(cf.createContext(JMSContext.DUPS_OK_ACKNOWLEDGE));
      assertEquals(JMSContext.DUPS_OK_ACKNOWLEDGE, ctx.getSessionMode());

      ctx.close();
      ctx =  addContext(cf.createContext(JMSContext.SESSION_TRANSACTED));
      assertEquals(JMSContext.SESSION_TRANSACTED, ctx.getSessionMode());

      ctx.close();
      ctx =  addContext(cf.createContext(JMSContext.CLIENT_ACKNOWLEDGE));
      assertEquals(JMSContext.CLIENT_ACKNOWLEDGE, ctx.getSessionMode());

      ctx.close();
      ctx =  addContext(cf.createContext(JMSContext.AUTO_ACKNOWLEDGE));
      assertEquals(JMSContext.AUTO_ACKNOWLEDGE, ctx.getSessionMode());

   }

   @Test
   public void testReceiveBytes() throws Exception
   {
      JMSProducer producer = context.createProducer();

      JMSConsumer consumer = context.createConsumer(queue1);

      BytesMessage bytesSend = context.createBytesMessage();
      bytesSend.writeByte((byte)1);
      bytesSend.writeLong(2l);
      producer.send(queue1, bytesSend);

      BytesMessage msgReceived = (BytesMessage)consumer.receiveNoWait();

      byte[] bytesArray = msgReceived.getBody(byte[].class);

      assertEquals((byte) 1, msgReceived.readByte());
      assertEquals(2l, msgReceived.readLong());

      DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytesArray));

      assertEquals((byte)1, dataInputStream.readByte());
      assertEquals(2l, dataInputStream.readLong());

   }

   @Test
   public void testReceiveText() throws Exception
   {
      JMSProducer producer = context.createProducer();

      JMSConsumer consumer = context.createConsumer(queue1);

      String randomStr = newXID().toString();

      System.out.println("RandomStr:" + randomStr);

      TextMessage sendMsg = context.createTextMessage(randomStr);
      producer.send(queue1, sendMsg);


      TextMessage receiveMsg = (TextMessage)consumer.receiveNoWait();

      assertEquals(randomStr, receiveMsg.getText());

   }

   @Test
   public void testDelay() throws Exception
   {
      JMSProducer producer = context.createProducer();

      JMSConsumer consumer = context.createConsumer(queue1);

      producer.setDeliveryDelay(500);

      long timeStart = System.currentTimeMillis();

      String strRandom = newXID().toString();

      producer.send(queue1, context.createTextMessage(strRandom));

      TextMessage msg = (TextMessage)consumer.receive(2500);

      assertNotNull(msg);

      long actualDelay = System.currentTimeMillis() - timeStart;
      assertTrue("delay is not working, actualDelay=" + actualDelay, actualDelay >= 500 && actualDelay < 2000);

      assertEquals(strRandom, msg.getText());
   }

   @Test
   public void testExpire() throws Exception
   {
      JMSProducer producer = context.createProducer();

      producer.setTimeToLive(500);

      String strRandom = newXID().toString();

      producer.send(queue1, context.createTextMessage(strRandom));

      Thread.sleep(700);

      // Create consumer after message is expired, making it to expire at the server's
      JMSConsumer consumer = context.createConsumer(queue1);

      TextMessage msg = (TextMessage)consumer.receiveNoWait();

      // Time to live kicked in, so it's supposed to return null
      assertNull(msg);

      strRandom = newXID().toString();

      producer.send(queue1, context.createTextMessage(strRandom));

      Thread.sleep(700);

      // Receive second message, expiring on client
      msg = (TextMessage)consumer.receiveNoWait();

      assertNull(msg);

      strRandom = newXID().toString();

      producer.send(queue1, context.createTextMessage(strRandom));

      // will receive a message that's not expired now
      msg = (TextMessage)consumer.receiveNoWait();

      assertNotNull(msg);

      assertEquals(strRandom, msg.getText());

   }

   @Test
   public void testDeliveryMode() throws Exception
   {
      JMSProducer producer = context.createProducer();

      JMSConsumer consumer = context.createConsumer(queue1);

      producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

      String strRandom = newXID().toString();

      producer.send(queue1, context.createTextMessage(strRandom));

      TextMessage msg = (TextMessage)consumer.receiveNoWait();

      assertNotNull(msg);

      assertEquals(DeliveryMode.NON_PERSISTENT, msg.getJMSDeliveryMode());

   }

   @Test
   public void testInvalidMessage()
   {
      JMSProducer producer = context.createProducer();
      try
      {
         producer.send(queue1, (Message)null);
         Assert.fail("null msg");
      }
      catch (MessageFormatRuntimeException expected)
      {
         // no-op
      }
   }

   @Test
   public void testInvalidDestination()
   {
      JMSProducer producer = context.createProducer();
      Message msg = context.createMessage();
      try
      {
         producer.send((Destination)null, msg);
         Assert.fail("null Destination");
      }
      catch (InvalidDestinationRuntimeException expected)
      {
         // no-op
      }
   }

   @Test
   public void testSendStreamMessage() throws JMSException, InterruptedException
   {
      JmsProducerCompletionListenerTest.CountingCompletionListener cl = new JmsProducerCompletionListenerTest.CountingCompletionListener(1);
      JMSProducer producer = context.createProducer();
      producer.setAsync(cl);
      StreamMessage msg = context.createStreamMessage();
      msg.setStringProperty("name", name.getMethodName());
      String bprop = "booleanProp";
      String iprop = "intProp";
      msg.setBooleanProperty(bprop, true);
      msg.setIntProperty(iprop, 42);
      msg.writeBoolean(true);
      msg.writeInt(67);
      producer.send(queue1, msg);
      JMSConsumer consumer = context.createConsumer(queue1);
      Message msg2 = consumer.receive(100);
      Assert.assertNotNull(msg2);
      Assert.assertTrue(cl.completionLatch.await(1, TimeUnit.SECONDS));
      StreamMessage sm = (StreamMessage)cl.lastMessage;
      Assert.assertEquals(true, sm.getBooleanProperty(bprop));
      Assert.assertEquals(42, sm.getIntProperty(iprop));
      Assert.assertEquals(true, sm.readBoolean());
      Assert.assertEquals(67, sm.readInt());
   }

   @Test
   public void testSetClientIdLate()
   {
      JMSProducer producer = context.createProducer();
      Message msg = context.createMessage();
      producer.send(queue1, msg);
      try
      {
         context.setClientID("id");
         Assert.fail("expected exception");
      }
      catch (IllegalStateRuntimeException e)
      {
         // no op
      }
   }

   @Test
   public void testCloseSecondContextConnectionRemainsOpen() throws JMSException
   {
      JMSContext localContext = context.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
      Assert.assertEquals("client_ack", JMSContext.CLIENT_ACKNOWLEDGE, localContext.getSessionMode());
      JMSProducer producer = localContext.createProducer();
      JMSConsumer consumer = localContext.createConsumer(queue1);

      final int pass = 1;
      for (int idx = 0; idx < 2; idx++)
      {
         Message m = localContext.createMessage();
         int intProperty = random.nextInt();
         m.setIntProperty("random", intProperty);
         Assert.assertNotNull(m);
         producer.send(queue1, m);
         m = null;
         Message msg = consumer.receive(100);
         Assert.assertNotNull("must have a msg", msg);
         Assert.assertEquals(intProperty, msg.getIntProperty("random"));
         /* In the second pass we close the connection before ack'ing */
         if (idx == pass)
         {
            localContext.close();
         }
         /**
          * From {@code JMSContext.close()}'s javadoc:<br/>
          * Invoking the {@code acknowledge} method of a received message from a closed connection's
          * session must throw an {@code IllegalStateRuntimeException}. Closing a closed connection
          * must NOT throw an exception.
          */
         try {
            msg.acknowledge();
            Assert.assertEquals("connection should be open on pass 0. It is " + pass, 0, idx);
         }
         // HORNETQ-1209 "JMS 2.0" XXX JMSContext javadoc says we must expect a
         // IllegalStateRuntimeException here. But Message.ack...() says it must throws the
         // non-runtime variant.
         catch (javax.jms.IllegalStateException expected)
         {
            Assert.assertEquals("we only close the connection on pass " + pass, pass, idx);
         }
      }
   }

   @Test(expected = JMSRuntimeException.class)
   public void testInvalidSessionModesValueMinusOne()
   {
      context.createContext(-1);
   }

   @Test(expected = JMSRuntimeException.class)
   public void testInvalidSessionModesValue4()
   {
      context.createContext(4);
   }

   @Test
   public void testGetAnotherContextFromIt()
   {
      JMSContext c2 = context.createContext(Session.DUPS_OK_ACKNOWLEDGE);
      Assert.assertNotNull(c2);
      Assert.assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c2.getSessionMode());
      Message m2 = c2.createMessage();
      Assert.assertNotNull(m2);
      c2.close(); // should close its session, but not its (shared) connection
      try
      {
         c2.createMessage();
         Assert.fail("session should be closed...");
      }
      catch (JMSRuntimeException expected)
      {
         // expected
      }
      Message m1 = context.createMessage();
      Assert.assertNotNull("connection must be open", m1);
   }

   @Test
   public void testSetGetClientIdNewContext()
   {
      final String id = "123";
      JMSContext c = context;// createContext();
      c.setClientID(id);
      JMSContext c2 = addContext(c.createContext(Session.CLIENT_ACKNOWLEDGE));
      Assert.assertEquals(id, c2.getClientID());
   }

   @Test
   public void testGetClientId()
   {
      JMSContext context2 = addContext(context.createContext(Session.AUTO_ACKNOWLEDGE));
      final String id = "ID: " + random.nextInt();
      context.setClientID(id);
      Assert.assertEquals("id's must match because the connection is shared", id, context2.getClientID());
   }

   @Test
   public void testCreateConsumerWithSelector() throws JMSException
   {
      final String filterName = "magicIndexMessage";
      final int total = 5;
      JMSProducer producer = context.createProducer();
      JMSConsumer consumerNoSelect = context.createConsumer(queue1);
      JMSConsumer consumer = context.createConsumer(queue1, filterName + "=TRUE");
      for (int i = 0; i < total; i++)
      {
         Message msg = context.createTextMessage("message " + i);
         msg.setBooleanProperty(filterName, i == 3);
         producer.send(queue1, msg);
      }
      Message msg0 = consumer.receive(500);
      Assert.assertNotNull(msg0);
      msg0.acknowledge();
      Assert.assertNull("no more messages", consumer.receiveNoWait());
      for (int i = 0; i < total - 1; i++)
      {
         Message msg = consumerNoSelect.receive(100);
         Assert.assertNotNull(msg);
         msg.acknowledge();
      }
      Assert.assertNull("no more messages", consumerNoSelect.receiveNoWait());
   }

   @Test
   public void testContextStopAndCloseFromMessageListeners() throws Exception
   {
      final JMSContext context1 = context.createContext(Session.AUTO_ACKNOWLEDGE);
      JMSConsumer consumer1 = context1.createConsumer(queue1);

      final CountDownLatch latch1 = new CountDownLatch(1);

      InvalidMessageListener listener1 = new InvalidMessageListener(context1, latch1, 1);

      consumer1.setMessageListener(listener1);

      JMSProducer producer = context1.createProducer();
      Message msg = context1.createTextMessage("first message");
      producer.send(queue1, msg);

      latch1.await();

      Throwable error1 = listener1.getError();

      assertNotNull(error1);

      assertTrue(error1 instanceof IllegalStateRuntimeException);

      context1.close();

      final JMSContext context2 = context.createContext(Session.AUTO_ACKNOWLEDGE);
      JMSConsumer consumer2 = context2.createConsumer(queue1);

      final CountDownLatch latch2 = new CountDownLatch(1);

      InvalidMessageListener listener2 = new InvalidMessageListener(context2, latch2, 2);

      consumer2.setMessageListener(listener2);

      JMSProducer producer2 = context2.createProducer();
      Message msg2 = context2.createTextMessage("second message");
      producer2.send(queue1, msg2);
      
      latch2.await();

      Throwable error2 = listener2.getError();
      
      assertNotNull(error2);
      
      assertTrue(error2 instanceof IllegalStateRuntimeException);
      
      context2.close();
   }

   private static class InvalidMessageListener implements MessageListener
   {
      private int id;
      private CountDownLatch latch;
      private JMSContext context;
      private volatile Throwable error;

      public InvalidMessageListener(JMSContext context, CountDownLatch latch, int id)
      {
         this.id = id;
         this.latch = latch;
         this.context = context;
      }

      public Throwable getError()
      {
         return error;
      }

      @Override
      public void onMessage(Message arg0)
      {
         switch (id)
         {
         case 1:
            stopContext();
            break;
         case 2:
            closeContext();
            break;
         default:
            break;
         }
         latch.countDown();
      }

      private void stopContext()
      {
         try
         {
            context.stop();
         }
         catch (Throwable t)
         {
            error = t;
         }
      }

      private void closeContext()
      {
         try
         {
            context.close();
         }
         catch (Throwable t)
         {
            error = t;
         }
      }
      
   }
}
