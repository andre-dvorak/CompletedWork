package com.amica.billing;

import com.amica.billing.parse.CSVParser;
import com.amica.billing.parse.FlatParser;
import com.amica.billing.parse.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ParserFactoryTest {
    Parser mockParser;
    Supplier<Parser> mockParserFactory;

    @BeforeEach
    public void setUp(){
        mockParser = Mockito.mock(Parser.class);
        mockParserFactory = () -> mockParser;
    }

    @Test
    public void testCreateParser(){
        assertThat( ParserFactory.createParser("testFile1.csv"), instanceOf(CSVParser.class) );
        assertThat( ParserFactory.createParser("testFile2.flat"), instanceOf(FlatParser.class) );
        assertThat( ParserFactory.createParser("testFile3.CsV"), instanceOf(CSVParser.class) );
        assertThat( ParserFactory.createParser("testFile4.flAt"), instanceOf(FlatParser.class) );
        assertThat( ParserFactory.createParser(null), instanceOf(CSVParser.class) );
    }

    @Test
    public void testAddParser(){
        ParserFactory.addParser("tst", mockParserFactory);
        Assertions.assertThrows( IllegalArgumentException.class, () -> ParserFactory.addParser("tst", mockParserFactory) );
        assertThat( ParserFactory.createParser("testFile1.tst"), equalTo(mockParser) );
    }

    @Test
    public void testReplaceParser(){
        ParserFactory.replaceParser("flat", mockParserFactory);
        ParserFactory.replaceParser(null, mockParserFactory);
        Assertions.assertThrows( IllegalArgumentException.class, () -> ParserFactory.replaceParser("tst", mockParserFactory) );
        assertThat( ParserFactory.createParser("testFile1.flat"), equalTo(mockParser) );
        assertThat( ParserFactory.createParser(null), equalTo(mockParser) );
    }

}
