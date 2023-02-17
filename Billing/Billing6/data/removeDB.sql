CONNECT 'jdbc:derby://localhost/AmicaTraining';

SET SCHEMA Billing;

DROP TABLE invoice;
DROP TABLE customer;

DROP SCHEMA Billing RESTRICT;

