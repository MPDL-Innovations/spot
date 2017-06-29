package de.mpg.imeji.logic.search.elasticsearch.factory.util;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filters.Filters;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticAggregationFactory;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;

/**
 * Parse elasticSearchAggregation created by {@link ElasticAggregationFactory}
 * 
 * @author saquet
 *
 */
public class AggregationsParser {

  private AggregationsParser() {

  }

  /**
   * Parse a the Aggregation part of a SearchResponse and return a list of {@link FacetResult}.
   * Note: the aggregation must have been created by {@link ElasticAggregationFactory}
   * 
   * @param resp
   * @return
   */
  public static List<FacetResult> parse(SearchResponse resp) {
    List<FacetResult> facetResults = new ArrayList<>();
    if (resp != null && resp.getAggregations() != null) {
      Nested metadata = resp.getAggregations().get("metadata");
      if (metadata != null) {
        for (Aggregation mdAgg : metadata.getAggregations()) {
          FacetResult facetResult = new FacetResult(getFacetName(mdAgg.getName()), mdAgg.getName());
          if (mdAgg instanceof Filter) {
            Aggregation terms = ((Filter) mdAgg).getAggregations().asList().get(0);
            if (terms instanceof StringTerms) {
              for (Terms.Bucket bucket : ((StringTerms) terms).getBuckets()) {
                facetResult.getValues()
                    .add(new FacetResultValue(bucket.getKeyAsString(), bucket.getDocCount()));
              }
            } else if (terms instanceof Histogram) {
              int valueCount = 5;
              int intervalSize = ((Histogram) terms).getBuckets().size() / valueCount;
              int intervalDocCount = 0;
              int counter = 0;
              String yearStart = "Before ";
              String yearEnd = "";
              for (Histogram.Bucket b : ((Histogram) terms).getBuckets()) {
                if (counter < intervalSize) {
                  intervalDocCount += b.getDocCount();
                  counter++;
                } else {
                  yearEnd = b.getKeyAsString();
                  facetResult.getValues()
                      .add(new FacetResultValue(yearStart + yearEnd, intervalDocCount));
                  intervalDocCount = (int) b.getDocCount();
                  yearStart = b.getKeyAsString() + " to ";
                  counter = 0;
                }
              }
              facetResult.getValues()
                  .add(new FacetResultValue("After " + yearEnd, intervalDocCount));
            } else {
              System.out.println("NOT PARSED  METADATA AGGREGATION: " + terms);
            }
          }
          facetResults.add(facetResult);
        }
        Filters system = resp.getAggregations().get("system");
        if (system != null) {
          for (Aggregation agg : system.getBucketByKey("all").getAggregations()) {
            FacetResult facetResult = new FacetResult(getFacetName(agg.getName()), agg.getName());
            if (agg instanceof Filters) {
              for (Filters.Bucket bucket : ((Filters) agg).getBuckets()) {
                facetResult.getValues()
                    .add(new FacetResultValue(bucket.getKeyAsString(), bucket.getDocCount()));
              }
            } else if (agg instanceof StringTerms) {
              for (Terms.Bucket bucket : ((StringTerms) agg).getBuckets()) {
                facetResult.getValues()
                    .add(new FacetResultValue(bucket.getKeyAsString(), bucket.getDocCount()));
              }

            }
            facetResults.add(facetResult);
          }
        }
      }
    }
    return facetResults;
  }

  private static String getFacetName(String index) {
    final FacetService service = new FacetService();
    Facet facet = service.retrieveByIndexFromCache(index);
    return facet != null ? facet.getName() : index;
  }

}
