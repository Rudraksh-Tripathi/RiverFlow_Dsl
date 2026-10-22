package riverlang;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps names to values.
 *
 * var statements add entries
 * Expr.Variable lookups read entries
 * One flat table for now, no nested scopes like blocks or functions yet
 */

class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        throw new RuntimeError(name, "Undefined river or dam '" + name.lexeme + "'.");
    }
}
