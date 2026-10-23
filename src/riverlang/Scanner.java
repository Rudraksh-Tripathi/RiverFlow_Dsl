package riverlang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static riverlang.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("and", AND);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);

        // these are the extra keywords for my own river language
        keywords.put("flow", FLOW);
        keywords.put("dam", DAM);
        keywords.put("when", WHEN);
        keywords.put("default", DEFAULT);
        keywords.put("inflow", INFLOW);
        keywords.put("level", LEVEL);
        keywords.put("plot", PLOT);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ']': addToken(RIGHT_BRACKET); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case ':': addToken(COLON); break;
            case '@': addToken(AT); break;
            case '~': addToken(TILDE); break;

            case '[':
                // I use this as part of the flow[ ... ] literal, see how
                // identifier() handles the flow keyword below. Tokenizing
                // it here too means it's still usable on its own if I ever
                // want a general array literal later on.
                addToken(LEFT_BRACKET);
                break;

            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '<':
                if (match('-')) {
                    addToken(ARROW); // this is the <- arrow
                } else {
                    addToken(match('=') ? LESS_EQUAL : LESS);
                }
                break;

            case '/':
                if (match('/')) {
                    // it's a line comment, so just skip to the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                break; // just ignoring whitespace here

            case '\n':
                line++;
                break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character '" + c + "'.");
                }
                break;
        }
    }

    // identifiers and keywords

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;

        if (type == FLOW) {
            // looking ahead for the '[' that opens a flow literal, something
            // like flow[10ML @ 1 ~ 2]
            skipWhitespaceOnly();
            if (peek() == '[') {
                advance(); // consume the '['
                waterFlowLiteral();
                return;
            }
            // if flow shows up on its own with nothing after it, I just
            // let it fall through and get emitted as a normal FLOW keyword
        }

        addToken(type);
    }

    // just moves past spaces and tabs, doesn't touch newlines or the line
    // counter. this lets me write flow [10ML ...] with a space as well as
    // flow[10ML ...] with no space
    private void skipWhitespaceOnly() {
        while (peek() == ' ' || peek() == '\t') advance();
    }

    // this is my flow literal: flow[ NUMBER UNIT '@' NUMBER '~' NUMBER ]

    private void waterFlowLiteral() {
        skipInnerWhitespace();
        double amount = readNumber();

        skipInnerWhitespace();
        String unit = readUnit();

        skipInnerWhitespace();
        consume('@', "Expect '@' after flow amount in flow[...] literal.");
        skipInnerWhitespace();
        double startDay = readNumber();

        skipInnerWhitespace();
        consume('~', "Expect '~' after start day in flow[...] literal.");
        skipInnerWhitespace();
        double spread = readNumber();

        skipInnerWhitespace();
        consume(']', "Expect ']' to close flow[...] literal.");

        FlowLiteral value = new FlowLiteral(amount, unit, startDay, spread);
        addToken(WATERFLOW, value);
    }

    private void skipInnerWhitespace() {
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t' || peek() == '\n')) {
            if (peek() == '\n') line++;
            advance();
        }
    }

    private double readNumber() {
        int numStart = current;
        if (!isDigit(peek())) {
            Lox.error(line, "Expect a number in flow[...] literal.");
            return 0;
        }
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        return Double.parseDouble(source.substring(numStart, current));
    }

    private static final List<String> UNITS = List.of("GL", "ML", "kL", "L");

    private String readUnit() {
        for (String unit : UNITS) {
            if (source.regionMatches(current, unit, 0, unit.length())) {
                current += unit.length();
                return unit;
            }
        }
        Lox.error(line, "Expect a unit (L, kL, ML, or GL) after flow amount.");
        return "";
    }

    private void consume(char expected, String message) {
        if (peek() != expected) {
            Lox.error(line, message);
            return;
        }
        advance();
    }

    // strings and numbers, same as base lox

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // this is the closing quote
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // helpers

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}