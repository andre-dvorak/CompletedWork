package com.amica.billing.db;

import com.amica.billing.Invoice;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.stream.Stream;

public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, Integer>{

    public Stream<Invoice> streamAllBy();

}
