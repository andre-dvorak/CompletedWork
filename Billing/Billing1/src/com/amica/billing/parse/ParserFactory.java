package com.amica.billing.parse;

public class ParserFactory {
    private static final String FLAT = "flat";
    private static final String CSV = "csv";

    public static Parser createParser(String fileName){
        Parser parser = null;

        String[] splitFileName = fileName.split("[.]");

        switch(splitFileName[splitFileName.length-1]){
            case FLAT:
                parser = new FlatParser();
                break;
            case CSV:
                parser = new CSVParser();
                break;
            default:
                throw new RuntimeException("ParserFactory.createParser() ERROR: Unknown file type");
        }

        return parser;
    }

}
