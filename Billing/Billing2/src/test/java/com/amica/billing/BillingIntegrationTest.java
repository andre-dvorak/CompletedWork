package com.amica.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.amica.billing.BillingTest.CUSTOMER_FILE_LOC;
import static com.amica.billing.BillingTest.INVOICE_FILE_LOC;
import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class BillingIntegrationTest {

    public static final String SOURCE_FOLDER = "data";

    Billing billing;

    /**
     * Assure that the necessary folders are in place, and make a copy
     * of the source data files. Install mock objects as listeners for changes.
     */
    @BeforeEach
    public void setUp() throws IOException {
        Files.createDirectories(Paths.get(TEMP_FOLDER));
        Files.createDirectories(Paths.get(OUTPUT_FOLDER));
        Files.copy(Paths.get(SOURCE_FOLDER, CUSTOMERS_FILENAME),
                Paths.get(TEMP_FOLDER, CUSTOMERS_FILENAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(SOURCE_FOLDER, INVOICES_FILENAME),
                Paths.get(TEMP_FOLDER, INVOICES_FILENAME),
                StandardCopyOption.REPLACE_EXISTING);

        billing = new Billing(CUSTOMER_FILE_LOC, INVOICE_FILE_LOC);
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // Invoice Query Methods
    //
    @Test
    public void testGetInvoicesOrderedByNumber(){
        assertThat( billing.getInvoicesOrderedByNumber(), hasNumbers(101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 121, 122, 123, 124) );
    }

    @Test
    public void testGetInvoicesOrderedByDate(){
        assertThat( billing.getInvoicesOrderedByDate(), hasNumbers(101, 102, 103, 104, 110, 105, 106, 107, 109, 111, 112, 113, 108, 114, 115, 116, 117, 118, 119, 121, 123, 122, 124) );
    }

    @Test
    public void testGetInvoicesGroupedByCustomer(){
        Map<Customer, List<Invoice>> invoicesByCustomer = billing.getInvoicesGroupedByCustomer();
        assertThat( invoicesByCustomer.get(billing.getCustomers().get("Jerry Reed")).stream(), hasNumbers(109, 122) );
    }

    @Test
    public void testGetOverdueInvoices(){
        assertThat( billing.getOverdueInvoices(AS_OF_DATE), hasNumbers(102, 105, 106, 107, 113, 116, 118, 122, 124) );
    }

    @Test
    public void testGetCustomersAndVolume(){
        Map<Customer, Double> customersAndVolume = billing.getCustomersAndVolume();
        assertThat( customersAndVolume.get(billing.getCustomers().get("Jerry Reed")), closeTo(2640, .0001) );
        assertThat( customersAndVolume.get(billing.getCustomers().get("Janis Joplin")), closeTo(510, .0001) );
    }

    @Test
    public void testGetCustomersAndVolumeStream(){
        List<String> filteredNames = List.of("Jerry Reed", "Janis Joplin");
        Set<Billing.CustomerAndVolume> customersAndVolumeStream = billing.getCustomersAndVolumeStream().filter(e -> filteredNames.contains(e.getCustomer().getName())).collect(Collectors.toSet());
        double expectedVolume;

        for( Billing.CustomerAndVolume e : customersAndVolumeStream ){
            expectedVolume = e.getCustomer().getName().equals("Jerry Reed") ? 2640.00 : 510.00;
            assertThat( e.getVolume(), closeTo(expectedVolume, .0001) );
        }
    }

}
