package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) throws IOException {
        // parse the JSON
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("test.json");

        ObjectMapper objectMapper = new ObjectMapper();
        SummaryResponse summaryResponse = objectMapper.readValue(inputStream, SummaryResponse.class);



        // define the periods
        List<Period> periods = Arrays.asList(
                new Period("2023-01-01", "2023-01-01"),
                new Period("2023-01-02", "2023-01-02")
//                new Period("2023-01-04", "2023-01-05")
        );

        // call the method
        List<Summary> summaries = getTotalAmountAndCountByDateRangeAndType(summaryResponse, periods);

        // print the results
        for (Summary summary : summaries) {
            System.out.println(summary.toString());
        }
    }
    public static List<Summary> getTotalAmountAndCountByDateRangeAndType(SummaryResponse summaryResponse, List<Period> periods) {
        List<Summary> summaries = periods.stream()
                .map(period -> {
                    LocalDate startDate = LocalDate.parse(period.getStartDate());
                    LocalDate endDate = LocalDate.parse(period.getEndDate());

                    Map<String, int[]> typeTotals = summaryResponse.getData().getSummary()
                            .stream()
                            .filter(summaryItem -> {
                                LocalDate itemDate = LocalDate.parse(summaryItem.getDate());
                                return !itemDate.isBefore(startDate) && !itemDate.isAfter(endDate);
                            })
                            .collect(Collectors.groupingBy(summaryItem -> {
                                if(summaryItem.getType().equals("B")) {
                                    return "B";
                                }
                                else {
                                    return "Other";
                                }
                            }, Collectors.reducing(new int[]{0, 0},
                                    summaryItem -> {
                                        return new int[]{summaryItem.getAmount(), summaryItem.getCount()};
                                    },
                                    (total, summaryItem) -> {
                                        return new int[]{total[0] + summaryItem[0], total[1] + summaryItem[1]};
                                    })));

                    List<Summary> typeSummaries = new ArrayList<>();

                    int refundCount = 0;
                    int refundAmount = 0;
                    int amount = 0;
                    int count = 0;
                    for(Map.Entry<String, int[]> entry: typeTotals.entrySet()) {
                        int[] values = entry.getValue();
                        if(entry.getKey().equals("B")) {
                            refundAmount = values[0];
                            refundCount = values[1];
                        }
                        else {
                            amount += values[0];
                            count += values[1];
                        }
                    }

                    String periodStr = period.toString();
                    typeSummaries.add(new Summary(periodStr, amount, count, refundAmount, refundCount));

                    return typeSummaries;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return summaries;
    }

    private static Summary createSummaryWithType(String period, int amount, int count, int refundAmount,int refundCount ) {
        return new Summary(period, amount, count, refundAmount, refundCount);
    }

    public List<TransactionPeriodDate> groupPeriods(List<Period> periodList) {
        List<TransactionPeriodDate> groupedPeriods = new ArrayList<>();
        LocalDate latestEnd = null;
        String latestGroupBy = null;
        List<Period> currentGroupedPeriods = new ArrayList<>();

        for (Period period : periodList) {
            LocalDate start = LocalDate.parse(period.getStart());
            LocalDate end = LocalDate.parse(period.getEnd());
            String groupBy = period.getGroupBy();

            if (latestEnd == null || start.isAfter(latestEnd) || !groupBy.equals(latestGroupBy)) {
                // If there is no previous period or the current period does not overlap with the previous period or the groupBy value is different, add it to the grouped list
                if (!currentGroupedPeriods.isEmpty()) {
                    groupedPeriods.add(new TransactionPeriodDate(currentGroupedPeriods.get(0).getStart(), latestEnd, latestGroupBy, currentGroupedPeriods));
                    currentGroupedPeriods.clear();
                }
                currentGroupedPeriods.add(period);
                latestEnd = end;
                latestGroupBy = groupBy;
            } else {
                // If the current period overlaps with the previous period and has the same groupBy value, add it to the current group
                currentGroupedPeriods.add(period);
                latestEnd = end;
            }
        }

        if (!currentGroupedPeriods.isEmpty()) {
            groupedPeriods.add(new TransactionPeriodDate(currentGroupedPeriods.get(0).getStart(), latestEnd, latestGroupBy, currentGroupedPeriods));
        }

        return groupedPeriods;
    }

}


class Summary {
    private String period;
    private int amount;
    private int count;

    private int refundAmount;
    private int refundCount;
    //private String type;

    public Summary(String period, int amount, int count, int refundAmount, int refundCount) {
        this.period = period;
        this.amount = amount;
        this.count = count;
        this.refundAmount = refundAmount;
        this.refundCount = refundCount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // getters and setters

    @Override
    public String toString() {
        return "Summary{" +
                "period='" + period + '\'' +
                ", amount=" + amount +
                ", count=" + count +
                ", refundAmount='" + refundAmount + '\'' +
                ", refundCount='" + refundCount + '\'' +

                '}';
    }
}

class SummaryResponse {
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private List<SummaryItem> summary;
        private List<String> fields;

        public List<SummaryItem> getSummary() {
            return summary;
        }

        public void setSummary(List<SummaryItem> summary) {
            this.summary = summary;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }
    }

    public static class SummaryItem {
        private String date;
        private int amount;
        private int count;
        private String type;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        // getters and setters
    }
}

class Period {
    private String startDate;
    private String endDate;

    public Period(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return startDate + " to " + endDate;
    }
}