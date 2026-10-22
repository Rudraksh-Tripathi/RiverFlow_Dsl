package riverlang;

/*
 * This is the actual value that sits behind a WATERFLOW token. So when
 * the scanner sees something like
 *
 *     flow[10ML @ 1 ~ 2]
 *
 * it turns it into one of these, with:
 *     amount = 10, unit = "ML", startDay = 1, spread = 2
 *
 * The exemplar language just does [start ~ spread]@magnitude with a
 * plain number for the magnitude. Ours adds a unit onto that number
 * instead, so a flow value actually says what it means and you can't
 * mix it up with just some random number floating around.
 */
class FlowLiteral {
    final double amount;
    final String unit;     // "L", "kL", "ML", or "GL"
    final double startDay; // day the water starts arriving, starts counting from 1
    final double spread;   // how spread out / slow the decay is

    FlowLiteral(double amount, String unit, double startDay, double spread) {
        this.amount = amount;
        this.unit = unit;
        this.startDay = startDay;
        this.spread = spread;
    }

    @Override
    public String toString() {
        return "flow[" + amount + unit + " @ " + startDay + " ~ " + spread + "]";
    }
}