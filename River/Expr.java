package riverlang;

/*
 * These are all the different types of expressions our language can have.
 * Basically comes from chapter 5 of the book (Representing Code). Instead
 * of making one massive class that tries to handle every kind of
 * expression, each kind gets its own tiny class instead. Then the
 * Visitor interface is what lets us write things like the printer (and
 * later the interpreter) without having to touch these classes again
 * every time we add something new.
 *
 * Base Lox already has Binary, Grouping, Literal and Unary. On top of
 * that we added two more for our own language:
 *
 * Variable - just points to a river/dam that's already been named,
 * like googong
 *
 * WaterFlow - our actual flow literal, like flow[10ML @ 1 ~ 2]
 */
abstract class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
        R visitWaterFlowExpr(WaterFlow expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

    // left OPERATOR right, like googong + jerrabombarra
    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    // just something wrapped in brackets
    static class Grouping extends Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    // a plain value - number, string, true, false, or nil
    static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    // OPERATOR right, like -5 or !flag
    static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    // points back to a name that's already been declared, like googong
    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    // a flow literal, like flow[10ML @ 1 ~ 2]
    static class WaterFlow extends Expr {
        final FlowLiteral value;

        WaterFlow(FlowLiteral value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitWaterFlowExpr(this);
        }
    }
}