/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.messaging.tests.integration.consumer;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.impl.ConfigurationImpl;
import org.jboss.messaging.core.server.MessagingService;
import org.jboss.messaging.core.server.impl.MessagingServiceImpl;
import org.jboss.messaging.jms.client.JBossTextMessage;
import org.jboss.messaging.tests.util.UnitTestCase;
import org.jboss.messaging.util.SimpleString;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class ConsumerTest extends UnitTestCase
{
   private MessagingService messagingService;

   private SimpleString QUEUE = new SimpleString("ConsumerTestQueue");

   protected void setUp() throws Exception
   {
      Configuration conf = new ConfigurationImpl();

      conf.setSecurityEnabled(false);

      conf.getAcceptorConfigurations().add(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMAcceptorFactory"));

      messagingService = MessagingServiceImpl.newNullStorageMessagingServer(conf);

      messagingService.start();
   }

   protected void tearDown() throws Exception
   {
      messagingService.stop();

      messagingService = null;
   }

   public void testSimpleConsumerBrowser() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }

      consumer.close();

      consumer = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }

      consumer.close();

      session.close();


   }

   public void testConsumerBrowserWithSelector() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         message.putIntProperty(new SimpleString("x"), i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, new SimpleString("x >= 50"), false, true);

      for (int i = 50; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }

      consumer.close();

      consumer = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }

      consumer.close();

      session.close();


   }

   public void testConsumerBrowserWithStringSelector() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         if(i % 2 == 0)
         {
            message.putStringProperty(new SimpleString("color"), new SimpleString("RED"));
         }
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, new SimpleString("color = 'RED'"), false, true);

      for (int i = 0; i < numMessages; i+=2)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }

      session.close();


   }

   public void testConsumerMultipleBrowser() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, null, false, true);
      ClientConsumer consumer2 = session.createConsumer(QUEUE, null, false, true);
      ClientConsumer consumer3 = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
         message2 = consumer2.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
         message2 = consumer3.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
      }

      session.close();


   }

   public void testConsumerMultipleBrowserWithSelector() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         message.putIntProperty(new SimpleString("x"), i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, new SimpleString("x < 50"), false, true);
      ClientConsumer consumer2 = session.createConsumer(QUEUE, new SimpleString("x >= 50"), false, true);
      ClientConsumer consumer3 = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < 50; i++)
      {
         ClientMessage message2 = consumer.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
      }
      for (int i = 50; i < numMessages; i++)
      {
         ClientMessage message2 = consumer2.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
      }
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer3.receive(1000);
         assertEquals("m" + i, message2.getBody().getString());
      }

      session.close();


   }

   public void testConsumerBrowserMessagesArentAcked() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         assertEquals("m" + i, message2.getBody().getString());
      }
      //assert that all the messages are there and none have been acked
      assertEquals(messagingService.getServer().getPostOffice().getBinding(QUEUE).getQueue().getDeliveringCount(), 0);
      assertEquals(messagingService.getServer().getPostOffice().getBinding(QUEUE).getQueue().getMessageCount(), 100);

      session.close();


   }


   public void testConsumerBrowserMessageAckDoesNothing() throws Exception
   {

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true, false);

      session.createQueue(QUEUE, QUEUE, null, false, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 100;

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createMessage(session, "m" + i);
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE, null, false, true);

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message2 = consumer.receive(1000);

         message2.acknowledge();

         assertEquals("m" + i, message2.getBody().getString());
      }
      //assert that all the messages are there and none have been acked
      assertEquals(messagingService.getServer().getPostOffice().getBinding(QUEUE).getQueue().getDeliveringCount(), 0);
      assertEquals(messagingService.getServer().getPostOffice().getBinding(QUEUE).getQueue().getMessageCount(), 100);

      session.close();


   }

   private ClientMessage createMessage(ClientSession session, String msg)
   {
      ClientMessage message = session.createClientMessage(JBossTextMessage.TYPE, false, 0,
            System.currentTimeMillis(), (byte) 1);
      message.getBody().putString(msg);
      message.getBody().flip();
      return message;
   }
}
