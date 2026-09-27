package River;

import java.util.List;

import static River.TokenTypes.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Tokens> tokens;
    private int current = 0;

    Parser(List<Tokens> tokens) {
        this.tokens = tokens;
    }

    /** Parses a single expression. Statements come in a later stage. */
    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return flow();
    }

    private Expr flow() {
        Expr expr = equality();
        if (match(ARROW)) {
            Tokens operator = previous();
            Expr right = flow();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Tokens operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Tokens operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Tokens operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Tokens operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Tokens operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(WATERFLOW)) {
            return new Expr.WaterFlow((FlowLiteral) previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    // --- helpers ------------------------------------------------------

    private boolean match(TokenTypes... types) {
        for (TokenTypes type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Tokens consume(TokenTypes type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenTypes type) {
        if (isAtEnd()) return false;
        return peek().types == type;
    }

    private Tokens advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().types == EOF;
    }

    private Tokens peek() {
        return tokens.get(current);
    }

    private Tokens previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Tokens token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }
}
