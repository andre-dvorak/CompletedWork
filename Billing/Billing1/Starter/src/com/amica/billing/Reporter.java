package com.amica.billing;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.stream.Stream;

public class Reporter {

    Billing billingSystem;
    String reportFolder;
    LocalDate asOf;

    private static final String INVOICES_NUMBER_OR_GROUP = "==================================================================\n\n       Customer                     Issued      Amount        Paid\n----  ------------------------  ----------  ----------  ----------";
    private static final String INVOICES_VOLUME = "==================================================================\n\nCustomer                        Volume\n------------------------  ------------";
    private static final String INVOICES_OVERDUE = "==============================================================================\n\n       Customer                     Issued      Amount        Paid         Due\n----  ------------------------  ----------  ----------  ----------  ----------";

    public Reporter(Billing billingSystem, String reportFolder, LocalDate asOf) {
        this.billingSystem = billingSystem;
        this.reportFolder = reportFolder;
        this.asOf = asOf;
        billingSystem.addInvoiceListener(i -> onInvoiceChanged(i));
    }

    @SneakyThrows
    public void reportInvoicesOrderedByNumber(){
        try(PrintWriter printWriter = new PrintWriter(reportFolder + "/invoices_by_number_output.txt");){
            printWriter.println("All invoices, ordered by invoice number");
            printWriter.println(INVOICES_NUMBER_OR_GROUP);
            billingSystem.getInvoicesOrderedByNumber().forEach(i -> {
                String paidDate = i.getPaidDate().isPresent() ? i.getPaidDate().get().toString() : "";
                printWriter.printf("%4d  %-24s  %10s  %,10.2f  %10s%n", i.getNumber(), i.getCustomer().getName(), i.getInvoiceDate().toString(), i.getAmount(), paidDate);
            });
        }
    }

    @SneakyThrows
    public void reportInvoicesGroupedByCustomer(){
        try(PrintWriter printWriter = new PrintWriter(reportFolder + "/invoices_by_customer_output.txt");){
            printWriter.println("All invoices, grouped by customer and ordered by invoice number");
            printWriter.println(INVOICES_NUMBER_OR_GROUP);
            billingSystem.getInvoicesGroupedByCustomer().forEach(l -> {
                printWriter.printf("%n%s%n", l.get(0).getCustomer().getName());
                l.forEach(i -> {
                    String paidDate = i.getPaidDate().isPresent() ? i.getPaidDate().get().toString() : "";
                    printWriter.printf("%4d  %-24s  %10s  %,10.2f  %10s%n", i.getNumber(), i.getCustomer().getName(), i.getInvoiceDate().toString(), i.getAmount(), paidDate);
                });
            });
        }
    }

    @SneakyThrows
    public void reportOverdueInvoices(){
        try(PrintWriter printWriter = new PrintWriter(reportFolder + "/overdue_invoices_output.txt");) {
            printWriter.println("Overdue invoices, ordered by issue date");
            printWriter.println(INVOICES_OVERDUE);
            billingSystem.getOverdueInvoices().forEach(i -> {
                String paidDate = i.getPaidDate().isPresent() ? i.getPaidDate().get().toString() : "";
                String dueDate = i.getInvoiceDate().plusDays(i.getCustomer().getTerms().getDays()).toString();
                printWriter.printf("%4d  %-24s  %10s  %,10.2f  %10s  %10s%n", i.getNumber(), i.getCustomer().getName(), i.getInvoiceDate().toString(), i.getAmount(), paidDate, dueDate);
            });
        }
    }

    /*@SneakyThrows
    public void reportCustomersAndVolume(){
        try(PrintWriter printWriter = new PrintWriter(reportFolder + "/customer_and_volume_output.txt");) {
            printWriter.println("All customers and total volume of business");
            printWriter.println(INVOICES_VOLUME);
            billingSystem.getCustomersAndVolume().forEach(i -> {
                printWriter.printf("%-24s  %,12.2f%n", i.getCustomer().getName(), i.getAmount());
            });
        }
    }*/

    public void onInvoiceChanged(Invoice invoice){
        reportInvoicesOrderedByNumber();
    }

}
