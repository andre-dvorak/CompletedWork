package com.amica.billing;

import com.amica.esa.componentconfiguration.manager.ComponentConfigurationManager;
import com.amica.escm.configuration.properties.PropertiesConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Properties;

import static com.amica.billing.TestUtility.TEMP_FOLDER;

public class BillingConfiguredTest extends BillingTest {

    public static final String CUSTOMERS_FILENAME = "customers_configured.csv";
    public static final String INVOICES_FILENAME = "invoices_configured.csv";

    @BeforeAll
    public static void initialize(){
        System.setProperty("env.name", "Configured");
        ComponentConfigurationManager.getInstance().initialize();
    }

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
        return new Billing();
    }

}
