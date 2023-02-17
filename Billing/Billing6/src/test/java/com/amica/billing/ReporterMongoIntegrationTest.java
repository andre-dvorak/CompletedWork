package com.amica.billing;

import com.amica.billing.db.Migration;
import com.amica.billing.db.mongo.MigrateCSVToMongo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes={ReporterMongoIntegrationTest.Config.class})
public class ReporterMongoIntegrationTest extends ReporterIntegrationTestBase {

    @Autowired
    private Migration migration;

    @ComponentScan
    @EnableAutoConfiguration
    @EnableMongoRepositories
    @PropertySource(value={"classpath:test.properties", "classpath:migration.properties"})
    public static class Config{
    }

    @BeforeEach
    @Override
    public void setUp() throws IOException {
        super.setUp();
        migration.migrate();
    }

    @Test
    @Disabled
    public void testAndreCreatedThis(){
        System.out.println("hello");
    }

}
