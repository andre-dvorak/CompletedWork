package com.amica.billing;

import com.amica.escm.configuration.properties.PropertiesConfiguration;

import java.util.Properties;

import static com.amica.billing.TestUtility.*;
import static com.amica.billing.TestUtility.TEMP_FOLDER;

public class BillingPropertyTest extends BillingTest {

    public static final String CUSTOMERS_FILENAME = "customers_configured.csv";
    public static final String INVOICES_FILENAME = "invoices_configured.csv";

    /**
     * Helper method to get the customers filename.
     */
    @Override
    protected String getCustomersFilename() {
        return CUSTOMERS_FILENAME;
    }

    /**
     * Helper method to get the invoices filename.
     */
    @Override
    protected String getInvoicesFilename() {
        return INVOICES_FILENAME;
    }

    /**
     * Helper method to create the Billing object.
     */
    @Override
    protected Billing createTestTarget() {
        Properties properties = new Properties();
        properties.put( "Billing.customersFile", TEMP_FOLDER + "/" + getCustomersFilename() );
        properties.put( "Billing.invoicesFile", TEMP_FOLDER + "/" + getInvoicesFilename() );
        return new Billing( new PropertiesConfiguration(properties) );
    }

}
