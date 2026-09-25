package River;

public class Tokens {
    final TokenTypes types;
    final String lexeme;
    final Object literal;
    final int line;

    Tokens(TokenTypes types, String lexeme, Object literal, int line) {
        this.types = types;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return types + " " + lexeme + (literal != null ? " " + literal : "");
    }
}
