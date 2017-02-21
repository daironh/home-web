package eu.daiad.web.service.message.resolvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.daiad.web.annotate.message.MessageGenerator;
import eu.daiad.web.model.EnumDayOfWeek;
import eu.daiad.web.model.EnumTimeAggregation;
import eu.daiad.web.model.EnumTimeUnit;
import eu.daiad.web.model.device.EnumDeviceType;
import eu.daiad.web.model.message.EnumRecommendationTemplate;
import eu.daiad.web.model.message.Message;
import eu.daiad.web.model.message.MessageResolutionStatus;
import eu.daiad.web.model.message.Recommendation.ParameterizedTemplate;
import eu.daiad.web.model.message.SimpleMessageResolutionStatus;
import eu.daiad.web.model.query.DataQuery;
import eu.daiad.web.model.query.DataQueryBuilder;
import eu.daiad.web.model.query.DataQueryResponse;
import eu.daiad.web.model.query.EnumDataField;
import eu.daiad.web.model.query.EnumMetric;
import eu.daiad.web.model.query.EnumMeasurementDataSource;
import eu.daiad.web.model.query.SeriesFacade;
import eu.daiad.web.service.ICurrencyRateService;
import eu.daiad.web.service.IDataService;
import eu.daiad.web.service.message.AbstractRecommendationResolver;

@MessageGenerator(period = "P1W", dayOfWeek = EnumDayOfWeek.MONDAY, maxPerWeek = 1, maxPerMonth = 2)
@Component
@Scope("prototype")
public class InsightB1 extends AbstractRecommendationResolver
{
    public static class Parameters extends Message.AbstractParameters
        implements ParameterizedTemplate
    {
        /** A minimum value for weekly volume consumption */
        private static final String MIN_VALUE = "1E+0"; 

        @NotNull
        @DecimalMin(MIN_VALUE)
        private Double currentValue;

        @NotNull
        @DecimalMin(MIN_VALUE)
        private Double averageValue;

        @NotNull
        private EnumTimeUnit timeUnit;

        public Parameters()
        {
            super();
        }

        public Parameters(
            DateTime refDate, EnumDeviceType deviceType,
            EnumTimeUnit timeUnit, double currentValue, double averageValue)
        {
            super(refDate, deviceType);
            this.averageValue = averageValue;
            this.currentValue = currentValue;
            this.timeUnit = timeUnit;
        }

        @JsonProperty("currentValue")
        public void setCurrentValue(double y)
        {
            this.currentValue = y;
        }

        @JsonProperty("currentValue")
        public Double getCurrentValue()
        {
            return currentValue;
        }

        @JsonProperty("averageValue")
        public void setAverageValue(double y)
        {
            this.averageValue = y;
        }

        @JsonProperty("averageValue")
        public Double getAverageValue()
        {
            return averageValue;
        }

        @JsonProperty("timeUnit")
        public EnumTimeUnit getTimeUnit()
        {
            return timeUnit;
        }

        @JsonProperty("timeUnit")
        public void setTimeUnit(EnumTimeUnit timeUnit)
        {
            this.timeUnit = timeUnit;
        }

        @JsonIgnore
        @AssertTrue(message = "This insight (B.1) is only relevant to weekly/monthly consumption")
        public boolean hasProperTimeUnit()
        {
            return (timeUnit == EnumTimeUnit.WEEK || timeUnit == EnumTimeUnit.MONTH);
        }

        @JsonIgnore
        @Override
        public EnumRecommendationTemplate getTemplate()
        {
            EnumRecommendationTemplate t = null;            
            boolean increase = (averageValue < currentValue);

            switch (timeUnit) {
            case WEEK:
                t = increase?
                    EnumRecommendationTemplate.INSIGHT_B1_WEEKLY_CONSUMPTION_INCR:
                    EnumRecommendationTemplate.INSIGHT_B1_WEEKLY_CONSUMPTION_DECR;
                break;
            case MONTH:
                t = increase?
                    EnumRecommendationTemplate.INSIGHT_B1_MONTHLY_CONSUMPTION_INCR:
                    EnumRecommendationTemplate.INSIGHT_B1_MONTHLY_CONSUMPTION_DECR;
                break;
            default:
                // no-op
            }
            return t;
        }

        @JsonIgnore
        @Override
        public Map<String, Object> getParameters()
        {
            Map<String, Object> parameters = super.getParameters();

            parameters.put("value", currentValue);
            parameters.put("consumption", currentValue);     

            parameters.put("average_value", averageValue);
            parameters.put("average_consumption", averageValue);

            Double percentChange = 100.0 * Math.abs(((currentValue - averageValue) / averageValue));
            parameters.put("percent_change", Integer.valueOf(percentChange.intValue()));

            parameters.put("time_unit", timeUnit.name());

            return parameters;
        }

        @Override
        public Parameters withLocale(Locale target, ICurrencyRateService currencyRate)
        {
            return this;
        }
    }
    
    @Autowired
    IDataService dataService;
    
    @Override
    public List<MessageResolutionStatus<ParameterizedTemplate>> resolve(
        UUID accountKey, EnumDeviceType deviceType)
    {
        final double K = 1.40;  // a threshold (z-score) of significant change
        final double F = 0.60;  // a threshold ratio of non-nulls for collected values
        
        final Period P = Period.months(2); // the whole period under examination
        
        // Build a common part of a data-service query

        DataQuery query;
        DataQueryResponse queryResponse;
        SeriesFacade series;

        DataQueryBuilder queryBuilder = new DataQueryBuilder()
            .timezone(refDate.getZone())
            .user("user", accountKey)
            .source(EnumMeasurementDataSource.fromDeviceType(deviceType))
            .sum();
        
        // Examine in weekly/monthly basis
        
        List<MessageResolutionStatus<ParameterizedTemplate>> results = new ArrayList<>();
        
        for (EnumTimeUnit timeUnit: Arrays.asList(EnumTimeUnit.WEEK, EnumTimeUnit.MONTH)) {
            final Period period = timeUnit.toPeriod();
            final DateTime targetDate = // start at most recent period-sized past interval 
                timeUnit.startOf(refDate.minus(period)); 
            final int N = // number of unit-sized periods
                timeUnit.numParts(new Interval(targetDate.minus(P), targetDate));
            final double threshold = config.getVolumeThreshold(deviceType, timeUnit);
            
            // Compute for target period

            query = queryBuilder
                .sliding(targetDate, +1, timeUnit, EnumTimeAggregation.ALL)
                .build();
            queryResponse = dataService.execute(query);
            series = queryResponse.getFacade(deviceType);
            Double targetValue = (series != null)? 
                series.get(EnumDataField.VOLUME, EnumMetric.SUM) : null;
            if (targetValue == null || targetValue < threshold)
                continue; // skip; nothing to compare to
            
            // Compute for past N periods

            DateTime start = targetDate;
            SummaryStatistics summary = new SummaryStatistics();
            for (int i = 0; i < N; i++) {
                start = start.minus(period);
                query = queryBuilder
                    .sliding(start, +1, timeUnit, EnumTimeAggregation.ALL)
                    .build();
                queryResponse = dataService.execute(query);
                series = queryResponse.getFacade(deviceType);
                Double val = (series != null)? 
                    series.get(EnumDataField.VOLUME, EnumMetric.SUM) : null;
                if (val != null)
                    summary.addValue(val);
            }
            if (summary.getN() < N * F)
                continue; // skip; too few values
            
            // Seems we have sufficient data

            double averageValue = summary.getMean();
            if (averageValue < threshold)
                continue; // skip; not reliable, consumption is too low

            double sd = Math.sqrt(summary.getPopulationVariance());
            double normValue = (sd > 0)? ((targetValue - averageValue) / sd) : Double.POSITIVE_INFINITY;
            double score = (sd > 0)? (Math.abs(normValue) / (2 * K)) : Double.POSITIVE_INFINITY;

            debug(
                "%s/%s: Computed consumption for period %s to %s: " +
                    "%.2f μ=%.2f σ=%.2f x*=%.2f score=%.2f",
                 accountKey, deviceType, period.multipliedBy(N), targetDate.toString("dd/MM/YYYY"),
                 targetValue, averageValue, sd, normValue, score);
            
            ParameterizedTemplate parameterizedTemplate = 
                new Parameters(refDate, deviceType, timeUnit, targetValue, averageValue);
            results.add(
                new SimpleMessageResolutionStatus<>(score, parameterizedTemplate));
        }
       
        return results;
    }

}
