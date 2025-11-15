package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a statement for a given invoice of performances.
 */
public class StatementPrinter {
    private final Invoice invoice;
    private final Map<String, Play> plays;

    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    public Invoice getInvoice() {
        return this.invoice;
    }

    public Map<String, Play> getPlays() {
        return this.plays;
    }

    /**
     * Gets the play associated with the given performance.
     * @param performance the performance
     * @return the play associated with the performance
     */
    private Play getPlay(Performance performance) {
        return plays.get(performance.getPlayID());
    }

    /**
     * Formats an amount in cents as US currency.
     * @param amount the amount in cents
     * @return the formatted currency string
     */
    private String usd(int amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount / Constants.PERCENT_FACTOR);
    }

    /**
     * Calculates the volume credits for a given performance.
     * @param performance the performance
     * @return the volume credits for the performance
     */
    private int getVolumeCredits(Performance performance) {
        final Play play = getPlay(performance);
        int result = Math.max(performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);
        // add extra credit for every five comedy attendees
        if ("comedy".equals(play.getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }
        return result;
    }

    /**
     * Calculates the total volume credits for all performances.
     * @return the total volume credits
     */
    private int getTotalVolumeCredits() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getVolumeCredits(performance);
        }
        return result;
    }

    /**
     * Calculates the total amount for all performances.
     * @return the total amount
     */
    private int getTotalAmount() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getAmount(performance);
        }
        return result;
    }

    /**
     * Calculates the amount for a given performance.
     * @param performance the performance
     * @return the calculated amount
     * @throws RuntimeException if the play type is not known
     */
    private int getAmount(Performance performance) {
        final Play play = getPlay(performance);
        int result = 0;
        switch (play.getType()) {
            case "tragedy":
                result = Constants.TRAGEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.TRAGEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.TRAGEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.TRAGEDY_AUDIENCE_THRESHOLD);
                }
                break;
            case "comedy":
                result = Constants.COMEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + (Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD));
                }
                result += Constants.COMEDY_AMOUNT_PER_AUDIENCE * performance.getAudience();
                break;
            default:
                throw new RuntimeException(String.format("unknown type: %s", play.getType()));
        }
        return result;
    }

    /**
     * Returns a formatted statement of the invoice associated with this printer.
     * @return the formatted statement
     * @throws RuntimeException if one of the play types is not known
     */
    public String statement() {
        final StringBuilder result = 
            new StringBuilder("Statement for " + invoice.getCustomer() + System.lineSeparator());
        for (Performance performance : invoice.getPerformances()) {
            final Play play = getPlay(performance);

            // print line for this order
            result.append(String.format("  %s: %s (%s seats)%n", play.getName(),
                    usd(getAmount(performance)), performance.getAudience()));
        }
        result.append(String.format("Amount owed is %s%n", usd(getTotalAmount())));
        result.append(String.format("You earned %s credits%n", getTotalVolumeCredits()));
        return result.toString();
    }
}
