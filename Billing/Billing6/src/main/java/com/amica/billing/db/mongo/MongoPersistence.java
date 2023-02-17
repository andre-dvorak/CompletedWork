package com.amica.billing.db.mongo;

import com.amica.billing.Customer;
import com.amica.billing.Invoice;
import com.amica.billing.db.CachingPersistence;
import com.amica.billing.db.CustomerRepository;
import com.amica.billing.db.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.stream.Stream;

@Component
@Primary
public class MongoPersistence extends CachingPersistence {

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    @Override
    public void load(){
        super.load();
    }

    protected Stream<Customer> readCustomers(){
        CustomerRepository customerRepo = context.getBean(CustomerRepository.class);
        return customerRepo.streamAllBy();
    }

    protected Stream<Invoice> readInvoices(){
        InvoiceRepository invoiceRepo = context.getBean(InvoiceRepository.class);
        return invoiceRepo.streamAllBy();
    }

    protected void writeCustomer(Customer customer){
        CustomerRepository customerRepo = context.getBean(CustomerRepository.class);
        customerRepo.save(customer);
    }

    protected void writeInvoice(Invoice invoice){
        InvoiceRepository invoiceRepo = context.getBean(InvoiceRepository.class);
        invoiceRepo.save(invoice);
    }

}
