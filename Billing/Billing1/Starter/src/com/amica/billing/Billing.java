package com.amica.billing;

import com.amica.billing.parse.Parser;
import com.amica.billing.parse.ParserFactory;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Billing {

    private String customerFileName;
    private String invoiceFileName;
    private Parser parser;
    private List<Invoice> invoices;
    private Map<String, Customer> customers;
    private List<Consumer<Invoice>> invoiceListeners;

    public Billing(String customerFileName, String invoiceFileName){
        this.customerFileName = customerFileName;
        this.invoiceFileName = invoiceFileName;
        parser = ParserFactory.createParser(customerFileName);
        invoiceListeners = new ArrayList<Consumer<Invoice>>();
        try {
            customers = parser.parseCustomers(new BufferedReader(new FileReader(customerFileName)).lines()).collect(Collectors.toMap(Customer::getName, Function.identity()));
            invoices = parser.parseInvoices(new BufferedReader(new FileReader(invoiceFileName)).lines(), customers).collect(Collectors.toList());
        }catch(Exception e){
            throw new RuntimeException("ERROR: File not found:\n" + e.getStackTrace());
        }
    }

    public List<Invoice> getInvoices(){
        return Collections.unmodifiableList(invoices);
    }

    public Map<String, Customer> getCustomers(){
        return Collections.unmodifiableMap(customers);
    }

    public Stream<Invoice> getInvoicesOrderedByNumber(){
        return invoices.stream().sorted((i1, i2) -> Integer.compare(i1.getNumber(), i2.getNumber()));
    }

    public Stream<List<Invoice>> getInvoicesGroupedByCustomer(){
        return invoices.stream().collect(Collectors.groupingBy(i -> i.getCustomer())).values().stream();
    }

    public Stream<Invoice> getOverdueInvoices(){
        return invoices.stream().filter( i -> (i.getPaidDate().isEmpty() && i.getInvoiceDate().plusDays(i.getCustomer().getTerms().getDays()).isBefore(LocalDate.now())) || i.getInvoiceDate().plusDays(i.getCustomer().getTerms().getDays()).isBefore(i.getPaidDate().get()) );
    }

    /*public Stream<Map<Customer, Double>> getCustomersAndVolume(){
       return getInvoicesGroupedByCustomer().map(i -> new HashMap<Customer, Double>().put(i.get(0).getCustomer(), i.stream().mapToDouble(v -> v.getAmount()).sum()));
    }*/

    @SneakyThrows
    public void saveCustomers(){
        try(PrintWriter printWriter = new PrintWriter(customerFileName);){
            parser.produceCustomers(customers.values().stream()).forEach(l -> printWriter.println(l));
        }
    }

    @SneakyThrows
    public void saveInvoices(){
        try(PrintWriter printWriter = new PrintWriter(invoiceFileName);) {
            parser.produceInvoices(invoices.stream()).forEach(l -> printWriter.println(l));
        }
    }

    public void payInvoice(int invoiceNumber){
        Invoice invoice = invoices.stream().filter(i -> i.getNumber() == invoiceNumber).findFirst().orElse(null);

        if( invoice != null ){
            invoice.setPaidDate(Optional.of(LocalDate.now()));
            saveInvoices();
            invoiceListeners.stream().forEach(l -> l.accept(invoice));
        }
    }

    public void addInvoiceListener(Consumer<Invoice> listener){
        invoiceListeners.add(listener);
    }

    public void removeInvoiceListener(Consumer<Invoice> listener){
        invoiceListeners.remove(listener);
    }

    public static void main(String[] args){
        List<Invoice> invList = new ArrayList<Invoice>();
        invList.add(new Invoice(0, 15.0, LocalDate.now(), Optional.of(LocalDate.now()), null));
        invList.add(new Invoice(0, 10.5, LocalDate.now(), Optional.of(LocalDate.now()), null));
        double sum = invList.stream().mapToDouble(v -> v.getAmount()).sum();
        Billing billingSystem = new Billing("Starter/data/customers.csv", "Starter/data/invoices.csv");
    }

}
