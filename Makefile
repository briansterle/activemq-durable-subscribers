start_activemq:
	podman run -d --name activemq \
	-p 61616:61616 \
	-p 8161:8161 \
	-v ./target/activemq-durable-subscribers-1.0.0.jar:/opt/apache-activemq/lib/activemq-durable-subscribers-1.0.0.jar \
	-v ./activemq.xml:/opt/apache-activemq/conf/activemq.xml \
	apache/activemq-classic:6.1.6

restart_activemq:
	podman restart activemq

publisher: 
	mvn compile exec:java -Dexec.mainClass=com.sterle.Publisher

non_durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.NonDurableSubscriber

durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.DurableSubscriber


kill:
	podman kill --all; yes | podman container prune