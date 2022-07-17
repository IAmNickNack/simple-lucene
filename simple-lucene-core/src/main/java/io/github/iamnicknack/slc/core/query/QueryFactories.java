package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.api.query.QueryFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.time.ZonedDateTime;
import java.util.function.Function;

public class QueryFactories {

    private QueryFactories() {}

    public static QueryFactory<String> keyword(String field) {
        return value -> new TermQuery(new Term(field, value));
    }

    public static QueryFactory<String> text(String field) {
        QueryParser parser = new QueryParser(field, new StandardAnalyzer());
        return value -> {
            try {
                return parser.parse(value.toLowerCase());
            } catch (ParseException e) {
                throw new QueryException("Failed to parse query", e);
            }
        };
    }

    /**
     * This factory may not be necessary as it does exactly the same as:
     * <br>{@code Function.<Query>identity()::apply}. It only serves to be more descriptive.
     */
    public static QueryFactory<Query> lucene() {
        return Function.<Query>identity()::apply;
    }

    /**
     * Construct a query to match documents where the stored date falls on or after a specified date
     * @param field the field name
     */
    public static QueryFactory<ZonedDateTime> after(String field) {
        return value -> LongPoint.newRangeQuery(field, value.toInstant().toEpochMilli(), Long.MAX_VALUE);
    }

    /**
     * Construct a query to match documents where the stored date falls before a specified date
     * @param field the field name
     */
    public static QueryFactory<ZonedDateTime> before(String field) {
        return value -> LongPoint.newRangeQuery(field, Long.MIN_VALUE, value.toInstant().toEpochMilli() - 1);
    }

    /**
     * Construct a query to match documents where the stored date falls between two specified dates
     * (lower-bound inclusive)
     * @param field the field name
     */
    public static QueryFactory<ZonedDateTime[]> between(String field) {
        return value -> {
            if(value.length != 2) {
                throw new QueryException("Did not get 2 date for range query");
            }

            return LongPoint.newRangeQuery(field, value[0].toInstant().toEpochMilli(), value[1].toInstant().toEpochMilli() - 1);
        };
    }
}
