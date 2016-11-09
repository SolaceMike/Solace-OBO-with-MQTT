/**
 *  Copyright 2016 Solace Systems, Inc. All rights reserved.
 * 
 *  http://www.solace.com
 * 
 *  This source is distributed under the terms and conditions of
 *  any contract or license agreement between Solace Systems, Inc.
 *  ("Solace") and you or your company. If there are no licenses or
 *  contracts in place use of this source is not authorized. This 
 *  source is provided as is and is not supported by Solace unless
 *  such support is provided for under an agreement signed between 
 *  you and Solace.
 */
 package com.solace.obowithmqttsample;
 

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.Semaphore;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * A Mqtt basic requestor
 *
 */
public class BasicRequestor {
    
    // A unique Reply-To Topic for the client is obtained from Solace
    private String replyToTopic = "";
    private String clientName = "";
    
    private JSONParser parser = new JSONParser();
    
    @SuppressWarnings("unchecked")
	public void run(String... args) throws IOException {
        System.out.println("BasicRequestor initializing...");

        try {
            // Create an Mqtt client
            final MqttClient mqttClient = new MqttClient("tcp://" + args[0], "HelloWorldBasicRequestor");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            
            // Connect the client
            System.out.println("Connecting to Solace broker: tcp://" + args[0]);
            mqttClient.connect(connOpts);
            System.out.println("Connected");

            // Semaphore used for synchronizing b/w threads
            final Semaphore semaphore = new Semaphore(0);
            
            // Topic the client will use to send request messages
            final String requestTopic = "T/obo/request";
            
            // Callback - Anonymous inner-class for receiving the Reply-To topic from the Solace broker
            mqttClient.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // If the topic is "$SYS/client/reply-to" then set our replyToTopic
                    // to with the contents of the message payload received
                    if (topic != null && topic.equals("$SYS/client/reply-to")) { 
                        replyToTopic = new String(message.getPayload());
                        System.out.println("\nReceived Reply-to topic from Solace for the MQTT client:" +
                            "\n\tReply-To: " + replyToTopic + "\n");
                        //
                    } else if (topic != null && topic.equals("$SYS/client/client-name")) {
                    	clientName = new String(message.getPayload());
                        System.out.println("\nReceived client name from Solace for the MQTT client:" +
                                "\n\tclient name: " + clientName + "\n");
                    } else if (topic != null && topic.equals(replyToTopic)) { 
                        // Received a response to our request
                        try {
                            // Parse the response payload and convert to a JSONObject
                            Object obj = parser.parse(new String(message.getPayload()));
                            JSONObject jsonPayload = (JSONObject) obj;
                        
                            System.out.println("\nReceived a response!" +
                                    "\n\tCorrelation Id: " + (String) jsonPayload.get("correlationId") + 
                                    "\n\tResult:    " + (String) jsonPayload.get("result") + "\n");
                        } catch (ParseException ex) {
                            System.out.println("Exception parsing response message!");
                            ex.printStackTrace();
                        }
                    } else {
                    	// content from the publisher
                    	String time = new Timestamp(System.currentTimeMillis()).toString();
                        System.out.println("\nReceived a Message!" +
                                "\n\tTime:    " + time + 
                                "\n\tTopic:   " + topic + 
                                "\n\tMessage: " + new String(message.getPayload()) + 
                                "\n\tQoS:     " + message.getQos() + "\n");
                    }
                    
                    
                    semaphore.release(); // unblock main thread
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to Solace broker lost!" + cause.getMessage());
                    semaphore.release();
                }
                
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            
            // Subscribe client to the special Solace topic for requesting a unique
            // Reply-to destination for the MQTT client
            System.out.println("Requesting Reply-To topic from Solace...");
            mqttClient.subscribe("$SYS/client/reply-to", 0);
            
            // Wait for till we have received the reply to Topic
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }
            
            
            // Check if we have a Reply-To topic
            if(replyToTopic == null || replyToTopic.isEmpty())
            {
                System.out.println("Unable to request Reply-To from Solace. Exiting");
                System.exit(0);
            }
            
            // Subscribe client to the Solace provide Reply-To topic with a QoS level of 0
            System.out.println("Subscribing client to Solace provide Reply-To topic");
            mqttClient.subscribe(replyToTopic, 0);
            
            System.out.println("Requesting Client name from Solace...");
            mqttClient.subscribe("$SYS/client/client-name", 0);
            // Wait for till we have received the reply to Topic
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }
            
            // Create the request payload in JSON format
            JSONObject obj = new JSONObject();
            obj.put("correlationId", UUID.randomUUID().toString());
            obj.put("replyTo", replyToTopic);
            obj.put("clientName", clientName);
            obj.put("topicRequested", "The pub sub demo service");
            
            String reqPayload = obj.toJSONString();
            
            // Create a request message and set the request payload
            MqttMessage reqMessage = new MqttMessage(reqPayload.getBytes());
            reqMessage.setQos(0);
    
            System.out.println("Sending request to: " + requestTopic);
    
            // Publish the request message
            mqttClient.publish(requestTopic, reqMessage);

            // Wait for till we have received a response
            try {
                semaphore.acquire(); // block here until message received
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }
            
            // Wait for till we have received the reply to Topic
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }
            
            System.out.println("Press <return> to exit.");
            System.in.read();
            
            // Disconnect the client
            mqttClient.disconnect();
            System.out.println("Exiting");

            System.exit(0);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Check command line arguments
        if (args.length < 1) {
            System.out.println("Usage: BasicRequestor <msg_backbone_ip:port>");
            System.exit(-1);
        }
        
        BasicRequestor app = new BasicRequestor();
		app.run(args);
    }
}
