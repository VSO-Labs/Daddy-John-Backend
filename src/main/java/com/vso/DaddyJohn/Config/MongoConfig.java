package com.vso.DaddyJohn.Config.Mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new YearMonthToStringConverter());
        converters.add(new StringToYearMonthConverter());
        return new MongoCustomConversions(converters);
    }

    // Converts YearMonth to a String like "2024-01" for storing in MongoDB
    static class YearMonthToStringConverter implements Converter<YearMonth, String> {
        @Override
        public String convert(YearMonth source) {
            return source != null ? source.toString() : null;
        }
    }

    // Converts the String "2024-01" from MongoDB back to a YearMonth object
    static class StringToYearMonthConverter implements Converter<String, YearMonth> {
        @Override
        public YearMonth convert(String source) {
            return source != null ? YearMonth.parse(source) : null;
        }
    }
}