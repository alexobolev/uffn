FROM node:18-alpine AS node-assets

COPY . /var/uf

RUN rm -rf /var/uf/public/dist || true

WORKDIR /var/uf

RUN npm install && npm run build





FROM php:8.1-fpm-alpine AS php-server

RUN apk add --no-cache \
        bash \
        build-base \
        libpng-dev \
        libpq-dev \
        libxml2-dev \
        icu-dev && \
    docker-php-ext-install \
        gd \
        intl \
        pdo_pgsql \
        opcache && \
    apk del --purge \
        build-base

SHELL ["/bin/bash", "-c"]

COPY --from=composer /usr/bin/composer /usr/bin/composer

COPY --chown=www-data:www-data . /var/www/html

RUN rm /var/www/html/public/dist/* || true

COPY --chown=www-data:www-data --from=node-assets /var/uf/public/dist/* /var/www/html/public/dist/

USER www-data

WORKDIR /var/www/html

RUN composer install

CMD php-fpm

EXPOSE 9000
