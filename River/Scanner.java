package River;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static River.TokenTypes.*;




public class Scanner {
    private final String source;
    private final List<Tokens> tokens = new ArrayList<>();
    private int start = 0, current = 0, line = 1;
    private static final Map<String, TokenTypes> keywords = new HashMap<>();
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



    List<Tokens> scanTokens() {
        while (!AtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Tokens(EOF, "", null, line));
        return tokens;
    }


    private void skipWhiteSpaceOnly(){
        while (peek() == ' ' || peek() == '\t') advance();
    }



    
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
        while (!AtEnd() && (peek() == ' ' || peek() == '\t' || peek() == '\n')) {
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
 



    private void identifier(){
        while (isAlphanumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenTypes type = keywords.get(text);

        if (type == null) type = IDENTIFIER;

        if (type == FLOW){
            skipWhiteSpaceOnly();
            if(peek() == '[') {
                advance();
                waterFlowLiteral();
                return;
            }
        }
        addToken(type);
    }



    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case '[': addToken(LEFT_BRACKET); break;
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

            case '!': addToken(match('=') ? BANG_EQUAL : BANG);
                break;

            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); 
                break;

            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); 
                break;

            case '<':
                if (match('-')) {
                    addToken(ARROW);
                } else {
                    addToken(match('=') ? LESS_EQUAL : LESS);
                }
                break;
                
            case '/':  
                if (match('/')) {
                    while (peek() != '\n' && !AtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
                
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            case '"': string(); 
                break;

            default:
                if (isDigit(c)){
                    number();
                }
                else if (isAlpha(c)){
                    identifier();
                }
                else {
                    Lox.error(line, "Unknown char '" + c + "'");

                }
                    break;

            }
        }




    private boolean AtEnd() {
        return current >= source.length();

    }

    private char advance() {
        return source.charAt(current++);

    }

    private char peek() {
        if (AtEnd()) return '\0';
        return source.charAt(current);

    }

    private char peekNext() {
        if(current >= source.length() -1) return '\0';
        return source.charAt(current + 1);

    }

    private boolean match(char guess){
        if (AtEnd()) {
            return false;
        }
        if (source.charAt(current) != guess) {
            return false;
        }
        current += 1;
        return true;

    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';

    }

    private boolean isAlpha(char c){
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';

    }

    private boolean isAlphanumeric(char c){
        return isDigit(c) || isAlpha(c);

    }

    private void addToken(TokenTypes type){
        addToken(type, null);

    }

    private void addToken(TokenTypes type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Tokens(type, text, literal, line));

    }





    private void string(){
        while(peek() != '"' && !AtEnd()){
            if(peek() == '\n') line++;
            advance();
        }
        if(AtEnd()){
            Lox.error(line, "string is not terminated.");
            return;
        }
        advance();
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number(){
        while (isDigit(peek())) advance();
        if(peek() == '.' && isDigit(peekNext())){
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    
}
