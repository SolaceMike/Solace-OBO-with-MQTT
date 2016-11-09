# Solace-OBO-with-MQTT
Sample project using Solace 'On Behalf Of' subscription management with an MQTT client. 

See the accompanying article at the Solace dev portal under the following URL:
https://solace.com/blog/devops/obo-subscription-managers-mqtt

## MQ Telemetry Transport (MQTT)

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to information provided on the Solace [developer portal](http://dev.solacesystems.com/tech/mqtt/), you may also look at some external sources for more details about MQTT. The following are good places to start

- http://mqtt.org/
- https://www.eclipse.org/paho/

In this sample, I illustrate whow to use MQTT clients with a Solace On-Behalf-Of agent. When architecting an enterprise messaging system, there are options surrounding how application subscriptions are added to the Solace message routers. One of these options is the use of a subscription manager. 

A subscription manager is a custom coded program running in the back end of your enterprise which manages subscriptions for other clients. Solace enables a centralized subscription management architecture by providing a special feature which allows certain clients to subscribe and unsubscribe to topics on behalf of other clients.

## Contents

This repository contains code and matching tutorial walk through for the scenario described in my blog on the Solace dev portal [blog page](https://solace.com/blog/devops/obo-subscription-managers-mqtt).

## Prerequisites

This tutorial requires the Solace Java API library. Download the Java API library to your computer from [here](http://dev.solace.com/downloads/).

## Build the Samples

Copy the contents of the `sol-jcsmp-VERSION/lib` directory from the Java API library to a `libs` sub-directory in your `solace-samples-java` project.

In the following command line replace `VERSION` with the Solace Java API version you downloaded. For example:

  1. clone this GitHub repository
  1. `cd solace-samples-java`
  1. `mkdir libs`
  1. `cp  ../sol-jcsmp-VERSION/lib/* libs`
  1. `./gradlew assemble`


## Running the Samples

To launch the samples, build the project from source and then run samples like the following:

    ./build/staged/bin/oBOSubscriptionManager  <HOST>
    ./build/staged/bin/basicRequestor <HOST>
    ./build/staged/bin/topicPublisher   <HOST>

See the individual tutorials linked from the [blog page](https://solace.com/blog/devops/obo-subscription-managers-mqtt) for full details which can walk you through the samples, what they do, and how to correctly run them.

## Exploring the Sample

### Setting up your preferred IDE

Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow the steps below for your preferred IDE.

#### Using Eclipse

To generate Eclipse metadata (.classpath and .project files), do the following:

    ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 *File -> Import -> Existing projects into workspace*

Browse to the *'solace-samples-java'* root directory. All projects should import
free of errors.

#### Using IntelliJ IDEA

To generate IDEA metadata (.iml and .ipr files), do the following:

    ./gradlew idea

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Author

This sample was created by Mike O'Brien, a Senior Professional Services Consultant with Solace.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Get a better understanding of [Solace technology](http://dev.solace.com/tech/).
- Check out the [Solace blog](http://dev.solace.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solace.com/community/)
