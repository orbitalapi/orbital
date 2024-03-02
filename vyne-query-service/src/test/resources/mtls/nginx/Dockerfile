FROM nginx:1.18-alpine

EXPOSE 443

VOLUME [ "/etc/nginx/conf.d/certs" ]

ENV ALLOWED_CERTIFICATE_FINGERPRINT 'all'

COPY nginx.conf /nginx.template

CMD ["/bin/sh","-c", "envsubst '${ALLOWED_CERTIFICATE_FINGERPRINT}' < /nginx.template > /etc/nginx/conf.d/nginx.conf; nginx-debug -g 'daemon off;'"]
