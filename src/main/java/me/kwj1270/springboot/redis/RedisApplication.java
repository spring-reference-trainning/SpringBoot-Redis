package me.kwj1270.springboot.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@SpringBootApplication
public class RedisApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisApplication.class);

    /**
     * RedisConnectionFactory
     * SpringBoot 는 기본적으로 RedisConnectionFactory 인터페이스를 통해
     * Jedis Redis 라이브러리의 JedisConnectionFactory 인스턴스를 사용한다.
     * <p>
     * RedisConnectionFactory 는 container 와 template 에 사용된다.
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("chat"));

        return container;
    }

    /**
     * listenerAdapter()
     * listenerAdapter()는 Receiver 인스턴스를 `리스너 컨테이너`에 등록하여
     * 채팅 토픽에 대한 메시지를 수신할 수 있게 끔 만들어준다.
     * 단, 우리가 주의깊게 볼 것은 adapter 패턴으로 Receiver 를 MessageListenerAdapter로 감싼다.
     */
    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    /**
     * receiver()
     * 채팅 토픽에 대한 메시지를 수신할 수 있게 해주는 Receiver 빈등록
     * 단, 단일로 사용하기는 힘들고 MessageListenerAdapter 로 감싸야 된다.
     */
    @Bean
    Receiver receiver() {
        return new Receiver();
    }

    /**
     * 앞선, listenerAdapter 와 Receiver 는 메시지 수신을 위한 것 이라면
     * RedisTemplate 은 메시지를 보내기 위해 필요하다.
     * 여기서는 디폴트로 사용되는 StringRedisTemplate 을 기반으로 사용한다.
     */
    @Bean
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    public static void main(String[] args) throws InterruptedException {

        // 빈을 가지고 있는 ApplicationContext 르 가져온다.
        ApplicationContext ctx = SpringApplication.run(RedisApplication.class, args);
        // StringRedisTemplate 빈을 가져온다.
        StringRedisTemplate template = ctx.getBean(StringRedisTemplate.class);
        // Receiver 빈을 가져온다,
        Receiver receiver = ctx.getBean(Receiver.class);

        // 리시버의 숫자가 0이 될 때까지
        // 5초마다 로그를 남긴다.
        while (receiver.getCount() == 0) {

            LOGGER.info("Sending message...");
            template.convertAndSend("chat", "Hello from Redis!");
            Thread.sleep(500L);
        }

        System.exit(0);
    }
}
