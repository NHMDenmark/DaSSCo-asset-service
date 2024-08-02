FROM rabbitmq:3.13.6

COPY rabbitmq.config /etc/rabbitmq/rabbitmq.conf
COPY advanced.config /etc/rabbitmq/advanced.config
COPY configure.sh /usr/local/bin/setup-config-file.sh
RUN setup-config-file.sh >&2
RUN rabbitmq-plugins enable rabbitmq_auth_backend_oauth2 rabbitmq_management rabbitmq_jms_topic_exchange


