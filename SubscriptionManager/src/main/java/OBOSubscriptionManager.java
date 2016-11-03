/**
 *  Copyright 2016 Solace Systems, Inc. All rights reserved.
 * 
 *  http://www.solacesystems.com
 * 
 *  This source is distributed under the terms and conditions of
 *  any contract or license agreement between Solace Systems, Inc.
 *  ("Solace") and you or your company. If there are no licenses or
 *  contracts in place use of this source is not authorized. This 
 *  source is provided as is and is not supported by Solace unless
 *  such support is provided for under an agreement signed between 
 *  you and Solace.
 */
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.ClientName;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * OBOSubscriptionManager program to illustrate how an OBO subscription manager can be implemented
 */
public class OBOSubscriptionManager 
{
	// use a latch to synchnoronize threads
	final CountDownLatch latch = new CountDownLatch(1);
	private JSONParser parser = new JSONParser();
	/**
	 * Dump the expected command line usage (help) to standard out.
	 */
	private static void showUsage() {
		System.out.println("--------------------------------------------------------");
    	System.out.println("Usage: OBOSubscriptionManager <router>");
    	System.out.println("");
    	System.out.println("Where <router> is the ip:port of your Solace VMR or router.");
		System.out.println("--------------------------------------------------------");
	}
	/**
     * The core of this program.
     * 
     * @param args - the command line arguments passed into the program
     * @throws JCSMPException
     * @throws IOException
     * @throws InterruptedException
     */
	public void run(String... args) throws JCSMPException 
	{ 
        System.out.println("OBOSubscriptionManager initializing...");
        if (args.length != 1) {
        	showUsage();
        	throw new IllegalArgumentException("You must pass in the IP:port of your Solace VMR or router as " + 
        			"the only command line argument.");
        }
        final String solaceRouter = args[0];
        final String vpn = "default";
        final String user = "oboManager";
        final String pw = "default";
        final String oBOSubscriptionManagersWellKnownTopic = "T/obo/request";

    	// Create a JCSMP Session
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solaceRouter); // msg-backbone ip:port
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);  
        properties.setProperty(JCSMPProperties.USERNAME, user); 
        properties.setProperty(JCSMPProperties.PASSWORD, pw); 
        final JCSMPSession session =  JCSMPFactory.onlyInstance().createSession(properties);
        
        // connect before querying capabilities.
        session.connect();
        
        // lets check the capabilities of this session and ensure that the client username
        // which was used to connect has the 'subscription manager' property enabled.
		if (!session.isCapable(CapabilityType.SUBSCRIPTION_MANAGER)) {
			System.out.println("This agent's client username '" + user + "' must have 'subscription manager' enabled.");
			System.exit(0);
		}
        
        /** Anonymous inner-class for handling publishing events */
        final XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);
            }
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n",
                        messageID,timestamp,e);
            }
        });
        /** Anonymous inner-class for handling incoming messages */
        final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {
            @SuppressWarnings("unchecked")
			public void onReceive(BytesXMLMessage msg) {
                try {
                	BytesMessage bytes = (BytesMessage) msg;
                	byte[] bytesArr = bytes.getData();
                	
                	String Body = new String(bytesArr, "UTF-8");
                	System.out.println("Body=" + Body);
                	Object payloadObj = parser.parse(new String(Body));
                    JSONObject jsonPayload = (JSONObject) payloadObj;

                    String correlationId = (String) jsonPayload.get("correlationId");
                    String replyTo = (String) jsonPayload.get("replyTo");
                    String clientName = (String) jsonPayload.get("clientName");
                    String topicRequested = (String) jsonPayload.get("topicRequested");
                    
                    // here we are simulating a topic abstraction service
                    if (topicRequested.equals("The pub sub demo service")) {
                    	String physicalTopic = "T/GettingStarted/pubsub";
                    	System.out.println("Redirecting service abstraction '" + topicRequested + "' to physical topic '" + physicalTopic + "'.");
                    	topicRequested = physicalTopic;

        				// this is where we would check with an external data source to confirm if the client is entitled 
        				// to the requested topic
                        System.out.printf("Request Message received: from client '%s for topic %s\n",clientName, topicRequested);
                        System.out.println("This request will be allowed; making subscription on behalf of the client.");
                        
        				// make the subscription on behalf of the client. 
                        JCSMPFactory fact = JCSMPFactory.onlyInstance();
        				ClientName clientNameObject = fact.createClientName(clientName);
        		        Topic requestedTopic = fact.createTopic(topicRequested);
        		        String replyText = "ok";
        		        try {
        		        	// wait for confirm to be sure that the subscription was successfully applied. 
        		        	session.addSubscription(clientNameObject, requestedTopic, JCSMPSession.WAIT_FOR_CONFIRM);
        		        	// success, leave the replyText as 'ok'
        	                System.out.println("The subscription has been successfully made on the router.");
        		        } catch (JCSMPException e) {
        		        	replyText = "ERROR: " + e.getMessage();
        		        }
                        // send the reply to the client
        		        sendReply(correlationId, replyText, replyTo, prod);
                    }
                    else {
                    	sendReply(correlationId, "unknown service", replyTo, prod);
                    }
    				
   					// trigger the main thread
   					latch.countDown();                    
   				}
                catch (Exception e) {
					e.printStackTrace();
				}
            }
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n",e);
            }
        });
        cons.start();
        System.out.println("Consumer and producer created...");
        
        // subscribe to 'obo', the topic which this agent will accept requests on.
        JCSMPFactory fact = JCSMPFactory.onlyInstance();
        final Topic agentServiceTopic = fact.createTopic(oBOSubscriptionManagersWellKnownTopic);
        session.addSubscription(agentServiceTopic, true);

        // lets just wait until we've processed a message. 
        try {
            latch.await(); // block here until message received, and latch will flip
        } catch (InterruptedException e) {
            System.out.println("I was awoken while waiting");
        }        
        // and then exit
        session.closeSession();
        System.out.println("Exiting.");
	}
	@SuppressWarnings("unchecked")
	private void sendReply(String correlationId, String result, String replyToTopic, XMLMessageProducer prod) throws JCSMPException {
		JSONObject obj = new JSONObject();
        obj.put("correlationId", correlationId);
        obj.put("result", result);
        String respPayload = obj.toJSONString();
        byte[] replyBytes = respPayload.getBytes();
        
        JCSMPFactory fact = JCSMPFactory.onlyInstance();
        
        BytesMessage reply = fact.createMessage(BytesMessage.class);
        reply.setData(replyBytes);
        
        final Topic replyDest = fact.createTopic(replyToTopic);
        prod.send(reply, replyDest);
	}
	/**
	 * OBOSubscriptionManager program to illustrate how a Subscription Manager 
	 * can be implemented.
	 * @param args
	 * @throws JCSMPException
	 */
    public static void main(String... args) throws JCSMPException 
    {
    	OBOSubscriptionManager me = new OBOSubscriptionManager();
    	me.run(args);
    }
}
