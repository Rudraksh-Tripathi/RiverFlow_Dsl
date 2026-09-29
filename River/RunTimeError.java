package River;

class RunTimeError extends RuntimeException {
    final Tokens token;

    RunTimeError(Tokens token, String message) {
        super(message);
        this.token = token;
    }
}
