FROM php:latest

ADD router.php /
ADD action.php /

EXPOSE 8080

CMD [ "php", "-S", "0.0.0.0:8080", "/router.php" ]