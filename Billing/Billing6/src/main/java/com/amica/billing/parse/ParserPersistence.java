package com.amica.billing.parse;

import com.amica.billing.db.CachingPersistence;
import com.amica.billing.parse.Parser;
import com.amica.billing.*;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.stream.Stream;

@Component
@Log
public class ParserPersistence extends CachingPersistence {

    @Setter
    @Value("${ParserPersistence.customersFile}")
    String customersFile;

    @Setter
    @Value("${ParserPersistence.invoicesFile}")
    String invoicesFile;

    Parser parser;

    @Override
    @PostConstruct
    public void load(){
        parser = ParserFactory.createParser(invoicesFile);
        super.load();
    }

    protected Stream<Customer> readCustomers(){
        Stream<Customer> returnStream = Stream.empty();
        try{
            returnStream = parser.parseCustomers( Files.lines(Paths.get(customersFile)) );
        }catch(IOException e){
            log.log(Level.WARNING, e, () -> "There was an issue in ParserPersistence.readCustomers()");
        }

        return returnStream;
    }

    protected Stream<Invoice> readInvoices(){
        Stream<Invoice> returnStream = Stream.empty();
        try{
            returnStream = parser.parseInvoices( Files.lines(Paths.get(invoicesFile)), customers );
        }catch(IOException e){
            log.log(Level.WARNING, e, () -> "There was an issue in ParserPersistence.readInvoices()");
        }

        return returnStream;
    }

    protected void writeCustomer(Customer customer){
        try( PrintWriter out = new PrintWriter(new FileWriter(Paths.get(customersFile).toFile())) ){
            parser.produceCustomers( customers.values().stream() ).forEach(out::println);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex, () -> "Couldn't open " + customersFile + " in write mode.");
        }
    }

    protected void writeInvoice(Invoice invoice){
        try( PrintWriter out = new PrintWriter(new FileWriter(Paths.get(invoicesFile).toFile())) ){
            parser.produceInvoices( invoices.values().stream() ).forEach(out::println);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex, () -> "Couldn't open " + invoicesFile + " in write mode.");
        }
    }

}
