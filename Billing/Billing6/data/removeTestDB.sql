CONNECT 'jdbc:derby://localhost/AmicaTraining';

SET SCHEMA BillingTest;

DROP TABLE invoice;
DROP TABLE customer;

DROP SCHEMA BillingTest RESTRICT;

