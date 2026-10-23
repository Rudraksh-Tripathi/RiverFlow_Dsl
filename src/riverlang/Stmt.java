package riverlang;

/*
 * These are my statement AST nodes, from chapter 8, statements and
 * state. A Stmt is different from an Expr since it doesn't actually
 * produce a value, it does something instead, like printing something
 * out or defining a variable, as the interpreter works its way through
 * a program.
 *
 * A program is really just a list of these, run one after another from
 * top to bottom.
 */
abstract class Stmt {
    interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitVarStmt(Var stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    // just a bare expression on its own line, evaluated for whatever side
    // effect it has. right now that's nothing really, but I kept it in
    // since base Lox has it and a program still needs to be a complete
    // list of statements
    static class Expression extends Stmt {
        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    // print expression ; works out the expression and prints whatever it comes out to
    static class Print extends Stmt {
        final Expr expression;

        Print(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

    // var NAME = expression ; declares a new variable and stores its starting value
    static class Var extends Stmt {
        final Token name;
        final Expr initializer;

        Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
}