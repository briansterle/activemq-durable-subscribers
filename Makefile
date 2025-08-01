start_activemq:
	podman run -d --name activemq -p 61616:61616 -p 8161:8161 rmohr/activemq

restart_activemq:
	podman restart activemq

publisher: 
	mvn compile exec:java -Dexec.mainClass=com.sterle.Publisher

non_durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.NonDurableSubscriber

durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.DurableSubscriber

