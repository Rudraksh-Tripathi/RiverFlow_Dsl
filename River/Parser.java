package riverlang;

import java.util.ArrayList;
import java.util.List;

import static riverlang.TokenType.*;

/*
 * This is my actual parser. I followed chapter 6, parsing expressions,
 * for the expression side of things, and chapter 8, statements and
 * state, for handling whole statements.
 *
 * Here's the grammar I ended up with so far, written in Nystrom's
 * notation:
 *
 *   program    → declaration* EOF ;
 *   declaration→ varDecl | statement ;
 *   varDecl    → "var" IDENTIFIER "=" expression ";" ;
 *   statement  → exprStmt | printStmt ;
 *   exprStmt   → expression ";" ;
 *   printStmt  → "print" expression ";" ;
 *
 *   expression → equality ;
 *   equality   → comparison ( ( "!=" | "==" ) comparison )* ;
 *   comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 *   term       → factor ( ( "-" | "+" | "<-" ) factor )* ;
 *   factor     → unary ( ( "/" | "*" ) unary )* ;
 *   unary      → ( "!" | "-" ) unary | primary ;
 *   primary    → NUMBER | STRING | WATERFLOW | IDENTIFIER
 *              | "true" | "false" | "nil" | "(" expression ")" ;
 *
 *   waterflow  → "flow" "[" NUMBER UNIT "@" NUMBER "~" NUMBER "]" ;
 *   UNIT       → "GL" | "ML" | "kL" | "L" ;
 *
 * print(x); just parses as the PRINT keyword followed by an expression
 * that happens to be a parenthesized grouping. I didn't need an extra
 * rule just for the parentheses, primary already handles
 * "(" expression ")" on its own.
 *
 * Everything down to term I left unchanged from base Lox. I handle +
 * between two flows exactly the same way syntactically as + between
 * two plain numbers. The actual difference in meaning, adding two lists
 * of numbers versus adding two numbers, is something I decided to leave
 * for the interpreter to sort out later, not something the parser
 * needs to care about.
 *
 * What I actually added on top of chapter 6 and 8:
 *
 * I made primary also accept WATERFLOW literals, my own flow[...] syntax
 *
 * I made term also accept <- (ARROW) alongside +, so something like
 * dam1 <- (googong + jerrabombarra) parses fine. What <- actually does
 * is something I'm leaving for the interpreter to decide, not the parser
 *
 * I made varDecl always require an initializer after the =, since my
 * language doesn't really have a concept of an empty, uninitialized
 * river, so var x; on its own isn't legal in what I've built
 */
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // parses a whole program, just a list of statements
    List<Stmt> parseProgram() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // parses just a single expression, I kept this around for quick testing and the AstPrinter demo
    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // statements

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect a name after 'var'.");
        consume(EQUAL, "Expect '=' after variable name.");
        Expr initializer = expression();
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case VAR:
                case PRINT:
                    return;
                default:
                    break;
            }
            advance();
        }
    }

    // expressions

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS, ARROW)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
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

    // helpers

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }
}