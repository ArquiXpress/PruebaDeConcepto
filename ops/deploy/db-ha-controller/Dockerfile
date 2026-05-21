FROM alpine:3.20

RUN apk add --no-cache bash haproxy postgresql16-client openssh-client

COPY controller.sh /controller.sh
RUN chmod +x /controller.sh

EXPOSE 15432
ENTRYPOINT ["/controller.sh"]
