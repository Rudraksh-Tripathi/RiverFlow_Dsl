package River;

class FlowLiteral {
    final double amount;
    final String unit;
    final double startDay;
    final double spread;

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
