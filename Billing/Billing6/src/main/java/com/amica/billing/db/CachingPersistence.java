package com.amica.billing.db;

import lombok.Getter;
import com.amica.billing.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public abstract class CachingPersistence implements Persistence{

    protected Map<String, Customer> customers;
    protected Map<Integer, Invoice> invoices;

    //Loads stream of customers & invoices into map.  Separating
    //logic into two try blocks in case one of them fails
    public void load(){
        try( Stream<Customer> customerStream = readCustomers() ){
            customers = customerStream.collect(Collectors.toMap(Customer::getName, Function.identity()));
        }
        try( Stream<Invoice> invoiceStream = readInvoices() ){
            invoices = invoiceStream.collect(Collectors.toMap(Invoice::getNumber, Function.identity()));
        }
    }

    public void saveCustomer(Customer customer){
        customers.put(customer.getName(), customer);
        writeCustomer(customer);
    }

    public void saveInvoice(Invoice invoice){
        invoices.put(invoice.getNumber(), invoice);
        writeInvoice(invoice);
    }

    protected abstract Stream<Customer> readCustomers();
    protected abstract Stream<Invoice> readInvoices();
    protected abstract void writeCustomer(Customer customer);
    protected abstract void writeInvoice(Invoice invoice);
}
