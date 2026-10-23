package riverlang;

/*
 * This is the class that actually works out what an expression means,
 * instead of just printing it out like AstPrinter does. Same idea as
 * chapter 7 in the textbook, evaluating expressions.
 *
 * A value in this language ends up being one of a few things. It could
 * just be a plain number, kept over from base Lox, though it's not
 * really used for anything in a river program yet. It could be a
 * boolean or null, also just kept from base Lox. It could be a string,
 * same deal. Or it could be a list of 10 numbers, which is an actual
 * flow, the predicted litres per day over 10 days. That last one is
 * really the type that matters here, since it's what a flow[...]
 * literal actually turns into.
 *
 * For the rainfall side of things, it's just hardcoded for now, going
 * with the simplest option from the tutorial. Since a flow[...] literal
 * already carries everything it needs (an amount, a unit, a start day
 * and a spread), rainfall here just means a few named root rivers a
 * program can refer to by name, set up below. That's just standing in
 * for real rainfall data until it gets added properly later on.
 *
 * As for output, whatever the last expression works out to just gets
 * printed by Lox.run() once interpret() finishes, over in Lox.java.
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // The global environment. A few named root rivers get loaded in
    // here to start with (values taken from the Canberra example in the
    // assignment brief), which is basically standing in for rainfall
    // data the same way a hardcoded interpreter would. Now that var
    // exists though, a program can just declare its own rivers instead
    // of relying on these.
    private final Environment globals = new Environment();

    Interpreter() {
        globals.define("googong", flowFrom(new FlowLiteral(10, "ML", 1, 2)));
        globals.define("jerrabombarra", flowFrom(new FlowLiteral(4, "ML", 1, 1)));
        globals.define("upper_molongolo", flowFrom(new FlowLiteral(6, "ML", 2, 3)));
    }

    // Runs a whole program, just a list of statements executed one after another
    void interpret(java.util.List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // Kept around for quick single-expression testing from earlier weeks' demos
    Object interpret(Expr expression) {
        try {
            return evaluate(expression);
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
            return null;
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = evaluate(stmt.initializer);
        globals.define(stmt.name.lexeme, value);
        return null;
    }

    // Formats a value for print, a flow turns into a 10-day table,
    // anything else just prints normally
    static String stringify(Object value) {
        if (value instanceof double[]) {
            double[] flow = (double[]) value;
            StringBuilder sb = new StringBuilder("Predicted flow (litres/day):");
            for (int day = 0; day < flow.length; day++) {
                sb.append(String.format("%n  Day %2d: %,.1f L", day + 1, flow[day]));
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Object value = globals.get(expr.name);
        if (value instanceof double[]) {
            return ((double[]) value).clone();
        }
        return value;
    }

    @Override
    public Object visitWaterFlowExpr(Expr.WaterFlow expr) {
        return flowFrom(expr.value);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                // two flows meeting at a confluence, just add them day by day
                if (left instanceof double[] && right instanceof double[]) {
                    return addFlows(expr.operator, (double[]) left, (double[]) right);
                }
                // two plain numbers, normal addition, kept from base Lox
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands of '+' must be two flows or two numbers.");

            case ARROW:
                // dam <- upstream, applying a dam to whatever flow is coming in
                // Can't actually evaluate this one yet since there's no way to
                // declare a dam in the language yet. Parsing already handles it
                // fine from week 6, evaluating it is still future work.
                throw new RuntimeError(expr.operator,
                        "Dam connection '<-' is parsed but not yet evaluable " +
                        "-- dam declarations haven't been added to the language yet.");

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

            default:
                break;
        }

        return null;
    }

    // --- flow specific helpers ---

    /*
     * Turns a flow[...] literal into a list of 10 numbers, the litres
     * per day, using geometric decay. Basically each day releases a
     * fixed chunk of whatever volume is left over. The fraction
     * released each day works out to 1 divided by (spread + 1), so a
     * spread of 0 just dumps everything on the start day, a spread of 1
     * releases half each day, and so on.
     */
    static double[] flowFrom(FlowLiteral literal) {
        double totalLitres = literal.amount * litresPerUnit(literal.unit);
        double fraction = 1.0 / (literal.spread + 1);
        int startDay = (int) Math.round(literal.startDay);

        double[] days = new double[10];
        double remaining = totalLitres;
        for (int day = 1; day <= 10; day++) {
            if (day < startDay) {
                days[day - 1] = 0;
                continue;
            }
            double released = remaining * fraction;
            days[day - 1] = released;
            remaining -= released;
        }
        return days;
    }

    private static double litresPerUnit(String unit) {
        switch (unit) {
            case "L": return 1;
            case "kL": return 1_000;
            case "ML": return 1_000_000;
            case "GL": return 1_000_000_000;
            default: throw new IllegalArgumentException("Unknown unit: " + unit);
        }
    }

    private double[] addFlows(Token operator, double[] left, double[] right) {
        if (left.length != right.length) {
            throw new RuntimeError(operator, "Cannot combine flows of different lengths.");
        }
        double[] result = new double[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = left[i] + right[i];
        }
        return result;
    }

    // --- general helpers---

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}