package com.vwholesale.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(RabbitMqConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue wechatNotifyQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WECHAT_NOTIFY_QUEUE)
                .withArguments(deadLetterArgs())
                .build();
    }

    @Bean
    public Queue statsRefreshQueue() {
        return QueueBuilder.durable(RabbitMqConstants.STATS_REFRESH_QUEUE)
                .withArguments(deadLetterArgs())
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.DLQ).build();
    }

    @Bean
    public Binding wechatNotifyBinding(TopicExchange orderTopicExchange, Queue wechatNotifyQueue) {
        return BindingBuilder.bind(wechatNotifyQueue)
                .to(orderTopicExchange)
                .with("order.notify.boss");
    }

    @Bean
    public Binding statsRefreshCreatedBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("order.created");
    }

    @Bean
    public Binding statsRefreshConfirmedBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("order.confirmed");
    }

    @Bean
    public Binding statsRefreshPricedBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("order.priced");
    }

    @Bean
    public Binding statsRefreshStatementBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("order.statement.sent");
    }

    @Bean
    public Binding statsRefreshOrderPaymentBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("order.payment.received");
    }

    @Bean
    public Binding statsRefreshPaymentBinding(TopicExchange orderTopicExchange, Queue statsRefreshQueue) {
        return BindingBuilder.bind(statsRefreshQueue)
                .to(orderTopicExchange)
                .with("payment.received");
    }

    @Bean
    public Binding deadLetterBinding(DirectExchange deadLetterExchange, Queue deadLetterQueue) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dlq");
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    private Map<String, Object> deadLetterArgs() {
        return Map.of(
                "x-dead-letter-exchange", RabbitMqConstants.DLX_EXCHANGE,
                "x-dead-letter-routing-key", "dlq"
        );
    }
}
