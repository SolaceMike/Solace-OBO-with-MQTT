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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * A Mqtt topic publisher 
 *
 */
public class TopicPublisher {
    
    public void run(String... args) {
        System.out.println("TopicPublisher initializing...");

        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient("tcp://" + args[0], "HelloWorldPub");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            
            // Connect the client
            System.out.println("Connecting to Solace broker: tcp://" + args[0]);
            mqttClient.connect(connOpts);
            System.out.println("Connected");

            for (int index =1; index < 11; index++) {
                // Create a Mqtt message
                String content = "Hello world from MQTT " + index;
                MqttMessage message = new MqttMessage(content.getBytes());
                // Set the QoS on the Messages - 
                // Here we are using QoS of 0 (equivalent to Direct Messaging in Solace)
                message.setQos(0);
                
                System.out.println("Publishing message: " + content);
                
                // Publish the message
                mqttClient.publish("T/GettingStarted/pubsub", message);
                
                try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            // Disconnect the client
            mqttClient.disconnect();
            
            System.out.println("Message published. Exiting");

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

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length < 1) {
            System.out.println("Usage: TopicPublisher <msg_backbone_ip:port>");
            System.exit(-1);
        }

		TopicPublisher app = new TopicPublisher();
		app.run(args);
    }
}
