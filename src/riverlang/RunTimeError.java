package riverlang;

/* A runtime error, thrown while evaluating an already-parsed tree. */
class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}