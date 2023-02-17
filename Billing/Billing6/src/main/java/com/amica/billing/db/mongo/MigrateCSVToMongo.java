package com.amica.billing.db.mongo;

import com.amica.billing.db.CustomerRepository;
import com.amica.billing.db.InvoiceRepository;
import com.amica.billing.db.Migration;
import com.amica.billing.parse.ParserPersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@ComponentScan(basePackageClasses={CustomerRepository.class, ParserPersistence.class})
@EnableAutoConfiguration
@EnableMongoRepositories(basePackageClasses=CustomerRepository.class)
@PropertySource(value= {"classpath:DB.properties","classpath:migration.properties"})
public class MigrateCSVToMongo {

    public static void main(String[] args){
        try( ConfigurableApplicationContext context = SpringApplication.run(MigrateCSVToMongo.class) ){
            Migration migration = context.getBean(Migration.class);
            CustomerRepository customerRepo = context.getBean(CustomerRepository.class);
            InvoiceRepository invoiceRepo = context.getBean(InvoiceRepository.class);

            System.out.println("Customer count before: " + customerRepo.count());
            System.out.println("Invoice count before: " + invoiceRepo.count());
            migration.migrate();
            System.out.println("Customer count after: " + customerRepo.count());
            System.out.println("Invoice count after: " + invoiceRepo.count());


            /*MongoPersistence mongoPersistence = context.getBean(MongoPersistence.class);
            System.out.println(mongoPersistence.getCustomers());
            System.out.println(mongoPersistence.getInvoices());
            System.out.println(customerRepo.count());
            customerRepo.save(new com.amica.billing.Customer("Andre", "Test", com.amica.billing.Terms.CREDIT_30));
            System.out.println(customerRepo.count());
            System.out.println(customerRepo.findAll());

            System.out.println(invoiceRepo.count());
            invoiceRepo.save(new com.amica.billing.Invoice(1, customerRepo.findByFirstNameAndLastName("Andre", "Dvorak"), 15.00, java.time.LocalDate.now()));
            System.out.println(invoiceRepo.count());
            System.out.println(invoiceRepo.findById(1).get());

            customerRepo.deleteAll();
            invoiceRepo.deleteAll();*/
        }
    }

}
