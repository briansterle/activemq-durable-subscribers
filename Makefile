all: kill start_activemq start_redis log_activemq

# create_network:
# 	podman network create odb-net
log_activemq:
	podman logs -f activemq

start_activemq:
	podman run -d --name activemq \
		--network odb-net \
		-p 61616:61616 \
		-p 8161:8161 \
		-v ./target/activemq-durable-subscribers-1.0.0.jar:/opt/apache-activemq/lib/activemq-durable-subscribers-1.0.0.jar \
		-v ./lib/jedis-6.0.0.jar:/opt/apache-activemq/lib/jedis-6.0.0.jar \
		-v ./activemq.xml:/opt/apache-activemq/conf/activemq.xml \
		apache/activemq-classic:6.1.6

start_redis:
	podman run -d --name redis \
		--network odb-net \
		--replace \
		-p 6379:6379 redis 


restart_activemq:
	podman restart activemq

publisher: 
	mvn compile exec:java -Dexec.mainClass=com.sterle.Publisher

non_durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.NonDurableSubscriber

durable_subscriber:
	mvn compile exec:java -Dexec.mainClass=com.sterle.DurableSubscriber

dump_redis:
	mvn compile exec:java -Dexec.mainClass=com.sterle.DumpRedis


kill:
	podman kill --all; yes | podman container prune


redis-cli:
	podman exec -it redis redis-cli