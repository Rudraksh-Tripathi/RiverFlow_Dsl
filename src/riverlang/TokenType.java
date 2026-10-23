package riverlang;

enum TokenType {
    // single character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    COLON, AT, TILDE,

    // one or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    ARROW, // this is my <- symbol, meaning "is downstream of"

    // literals
    IDENTIFIER, STRING, NUMBER,
    WATERFLOW, // my flow literal, something like flow[10ML @ 1 ~ 2]

    // the normal keywords, same as base lox
    AND, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, TRUE, VAR, WHILE,

    // keywords just for my river language
    FLOW, DAM, WHEN, DEFAULT, INFLOW, LEVEL, PLOT,

    EOF
}