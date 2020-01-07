package pers.xmr.bigdata.kafka;

import java.util.*;
import java.util.Map.Entry;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.network.BlockingChannel;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import pers.xmr.bigdata.basic.EmailSendInfo;
import pers.xmr.bigdata.basic.EmailSender;
import pers.xmr.bigdata.basic.Property;


/**
 * @author xmr
 * @date 2020/1/6 10:04
 * @description 获取Kafka指定消费者组堆积的偏移量, 并发送邮件预警
 */
public class KafkaOffsetTools {

    public static void main(String[] args) throws InterruptedException {

        String topic = Property.getProperty("topic");
        String broker = Property.getProperty("broker");
        int port = 9092;
        String groups = Property.getProperty("group");
        String clientId = Property.getProperty("clientId");
        int correlationId = 0;
        while (true) {
            List<String> seeds = new ArrayList<>();
            seeds.add(broker);
            KafkaOffsetTools kot = new KafkaOffsetTools();

            TreeMap<Integer, PartitionMetadata> metadatas = kot.findLeader(seeds, port, topic);

            List<TopicAndPartition> partitions = new ArrayList<>();
            for (Entry<Integer, PartitionMetadata> entry : metadatas.entrySet()) {
                int partition = entry.getKey();
                TopicAndPartition testPartition = new TopicAndPartition(topic, partition);
                partitions.add(testPartition);
            }
            String[] groupArgs = groups.split(",");
            StringBuilder sb = new StringBuilder();
            BlockingChannel channel = new BlockingChannel(broker, port, BlockingChannel.UseDefaultBufferSize(), BlockingChannel.UseDefaultBufferSize(), 5000);
            String servers = Property.getProperty("servers");
            for (String group : groupArgs) {
                long sum = 0L;
                long sumOffset = 0L;
                long lag = 0L;
                KafkaConsumer<String, String> kafkaConsumer = kot.getKafkaConsumer(group, topic, servers);
                for (Entry<Integer, PartitionMetadata> entry : metadatas.entrySet()) {
                    int partition = entry.getKey();
                    try {
                        channel.connect();
                        TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
                        OffsetFetchRequest fetchRequest = new OffsetFetchRequest(group, partitions, (short) 1, correlationId, clientId);
                        channel.send(fetchRequest.underlying());
                        OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().payload());

//                      OffsetMetadataAndError result = fetchResponse.offsets().get(topicAndPartition);
                        OffsetAndMetadata committed = kafkaConsumer.committed(new TopicPartition(topic, partition));
                        long partitionOffset = committed.offset();
//                       short offsetFetchErrorCode = result.error();
//                        if (offsetFetchErrorCode == ErrorMapping.NotCoordinatorForConsumerCode()) {
//                        } else {
//                            long retrievedOffset = result.offset();
//                            sumOffset += retrievedOffset;
//                        }
                        sumOffset += partitionOffset;
                        String leadBroker = entry.getValue().leader().host();
                        String clientName = "Client_" + topic + "_" + partition;
                        SimpleConsumer consumer = new SimpleConsumer(leadBroker, port, 100000,
                                64 * 1024, clientName);
                        long readOffset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.LatestTime(), clientName);
                        sum += readOffset;
                        System.out.println("group: " + group + " " + partition + ":" + readOffset);
                        consumer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        channel.disconnect();
                    }
                }

                System.out.println("logSize：" + sum);
                System.out.println("offset：" + sumOffset);

                lag = sum - sumOffset;
                System.out.println("lag:" + lag);
                sb.append("消费者组 " + group + " 积压的偏移量为: " + lag);
                sb.append("\n");
            }
            String title = topic + " 消费者消费情况";
            kot.sendMail(title, sb.toString());
            Thread.sleep(60000 * 30);
        }


    }

    /**
     * 获取Kafka消费者实例
     *
     * @param group  消费者组
     * @param topic  主题名
     * @param servers 服务器列表
     * @return KafkaConsumer<String, String>
     */
    private KafkaConsumer<String, String> getKafkaConsumer(String group, String topic, String servers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", group);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("max.poll.records", 100);
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer(props);
        consumer.subscribe(Arrays.asList(topic));
        return consumer;

    }


    private KafkaOffsetTools() {
    }

    /**
     * 获取该消费者组每个分区最后提交的偏移量
     *
     * @param consumer   消费者组对象
     * @param topic      主题
     * @param partition  分区
     * @param whichTime  最晚时间
     * @param clientName 客户端名称
     * @return 偏移量
     */
    private static long getLastOffset(SimpleConsumer consumer, String topic, int partition, long whichTime, String clientName) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
        OffsetResponse response = consumer.getOffsetsBefore(request);
        if (response.hasError()) {
            System.out.println("Error fetching data Offset Data the Broker. Reason: " + response.errorCode(topic, partition));
            return 0;
        }
        long[] offsets = response.offsets(topic, partition);
        return offsets[0];
    }

    /**
     * 获取每个partation的元数据信息
     *
     * @param seedBrokers 服务器列表
     * @param port        端口号
     * @param topic       主题名
     * @return
     */
    private TreeMap<Integer, PartitionMetadata> findLeader(List<String> seedBrokers, int port, String topic) {
        TreeMap<Integer, PartitionMetadata> map = new TreeMap<>();
        loop:
        for (String seed : seedBrokers) {
            SimpleConsumer consumer = null;
            try {
                consumer = new SimpleConsumer(seed, port, 100000, 64 * 1024, "leaderLookup" + new Date().getTime());
                List<String> topics = Collections.singletonList(topic);
                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

                List<TopicMetadata> metaData = resp.topicsMetadata();
                for (TopicMetadata item : metaData) {
                    for (PartitionMetadata part : item.partitionsMetadata()) {
                        map.put(part.partitionId(), part);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error communicating with Broker [" + seed + "] to find Leader for [" + topic + ", ] Reason: " + e);
            } finally {
                if (consumer != null)
                    consumer.close();
            }
        }
        return map;
    }

    /**
     * 给定邮件主题和内容发送邮件
     *
     * @param title   邮件主题
     * @param content 邮件内容
     */
    private void sendMail(String title, String content) {
        EmailSendInfo mailInfo = new EmailSendInfo();
        String host = Property.getProperty("host");
        String username = Property.getProperty("username");
        String password = Property.getProperty("password");
        String sendEmailUsers = Property.getProperty("sendEmailUsers");
        String ccEmialUsers = Property.getProperty("ccEmailUsers");
        mailInfo.setMailServerHost(host);
        mailInfo.setValidate(true);
        mailInfo.setUserName(username);
        mailInfo.setPassword(password);
        mailInfo.setFromAddress(username);
        mailInfo.setToAddress(sendEmailUsers);
        mailInfo.setCcAddress(ccEmialUsers);
        mailInfo.setSubject(title);
        mailInfo.setContent(content);
        //发送文体格式邮件
        EmailSender.sendTextMail(mailInfo);
    }
}

