package River;

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

    /** left OPERATOR right, e.g. googong + jerrabombarra */
    static class Binary extends Expr {
        final Expr left;
        final Tokens operator;
        final Expr right;

        Binary(Expr left, Tokens operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    /** ( expression ) */
    static class Grouping extends Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    /** A plain value: number, string, true/false, nil */
    static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    /** OPERATOR right, e.g. -5 or !flag */
    static class Unary extends Expr {
        final Tokens operator;
        final Expr right;

        Unary(Tokens operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    /** A reference to a previously-declared name, e.g. googong */
    static class Variable extends Expr {
        final Tokens name;

        Variable(Tokens name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    /** A flow literal, e.g. flow[10ML @ 1 ~ 2] */
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
