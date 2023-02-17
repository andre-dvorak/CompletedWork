package com.amica.billing.db;

import com.amica.billing.Customer;
import com.amica.billing.Invoice;
import com.amica.billing.db.mongo.MongoPersistence;
import com.amica.billing.parse.ParserPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Migration {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private ParserPersistence source;
    @Autowired
    private MongoPersistence target;

    public void migrate(){
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();

        source.load();
        target.load();

        for( Customer customer : source.getCustomers().values() ){
            target.saveCustomer(customer);
        }

        for( Invoice invoice : source.getInvoices().values() ){
            target.saveInvoice(invoice);
        }

    }

}
