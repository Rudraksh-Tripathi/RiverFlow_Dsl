# RiverFlow DSL

A small domain-specific language for modelling water flow in river systems, with a scanner, parser and tree-walking interpreter written in Java.

You describe rivers as flow values, combine them where they meet, and the interpreter predicts the flow in litres per day over a 10 day window.

```
var mahers_creek = flow[6ML @ 1 ~ 1];
var joes_creek   = flow[1.2ML @ 1 ~ 0];
var lower_joes   = flow[2.2ML @ 1 ~ 1] + mahers_creek + joes_creek;
print(lower_joes);
```

```
Predicted flow (litres/day):
  Day  1: 5,300,000.0 L
  Day  2: 2,050,000.0 L
  Day  3: 1,025,000.0 L
  ...
  Day 10: 8,007.8 L
```

## Highlights

- **Flow literals with units.** `flow[10ML @ 1 ~ 2]` carries an amount, a unit (`L`, `kL`, `ML`, `GL`), a start day and a spread, so a value says what it means and units can't be mixed up silently.
- **Rivers combine with `+`.** Adding two flows adds them day by day, which models two rivers joining.
- **Variables and statements.** `var` names a river once so larger systems stay readable.
- **Full pipeline.** Scanner, recursive-descent parser, AST with the Visitor pattern, and an evaluator.
- **Clear errors.** Parse errors report a line number, and runtime errors exit with a distinct code.

## Quick start

Requires Java 11 or newer (developed on Java 21). No other dependencies.

**Mac / Linux**

```
javac -d out src/riverlang/*.java
java -cp out riverlang.Lox examples/confluence.river
```

**Windows (PowerShell)**

```
javac -d out (Get-ChildItem src\riverlang\*.java).FullName
java -cp out riverlang.Lox examples\confluence.river
```

Run with no file argument to start an interactive prompt: `java -cp out riverlang.Lox`.

Exit codes: `0` success, `64` bad usage, `65` syntax error, `70` runtime error.

## Language reference

### Flow literal

```
flow[ amount unit @ startDay ~ spread ]
```

| Part | Meaning |
|---|---|
| `amount` | Total volume of water |
| `unit` | `L`, `kL`, `ML` or `GL` |
| `startDay` | The day the water starts arriving (day 1 is the first day) |
| `spread` | How slowly the flow drains. `0` releases everything on the start day |

Each day, `1 / (spread + 1)` of the water still remaining is released, over a fixed 10 day window. So `flow[10L @ 1 ~ 0]` is 10 litres on day 1, and `flow[4ML @ 1 ~ 1]` releases half on day 1, a quarter on day 2, and so on.

### Statements

```
var name = expression;   // declare a river or value, an initial value is required
print(expression);       // show a result
expression;              // evaluate an expression
```

### Operators

| Operator | Meaning |
|---|---|
| `+` | Two flows: add day by day. Two numbers or strings: normal addition or concatenation |
| `-` `*` `/` | Number arithmetic |
| `<` `<=` `>` `>=` `==` `!=` | Comparison |
| `!` `-` | Not, negate |
| `<-` | Connects a dam to the flow entering it. **Parsed, not yet evaluated** (see Roadmap) |

Comments start with `//`.

## Grammar

Written in the notation used by *Crafting Interpreters*.

```
program     → declaration* EOF ;
declaration → varDecl | statement ;
varDecl     → "var" IDENTIFIER "=" expression ";" ;
statement   → exprStmt | printStmt ;
exprStmt    → expression ";" ;
printStmt   → "print" expression ";" ;

expression  → equality ;
equality    → comparison ( ( "!=" | "==" ) comparison )* ;
comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term        → factor ( ( "-" | "+" | "<-" ) factor )* ;
factor      → unary ( ( "/" | "*" ) unary )* ;
unary       → ( "!" | "-" ) unary | primary ;
primary     → NUMBER | STRING | WATERFLOW | IDENTIFIER
            | "true" | "false" | "nil" | "(" expression ")" ;
```

The scanner reads a whole flow literal and hands the parser a single `WATERFLOW` token:

```
WATERFLOW → "flow" "[" NUMBER UNIT "@" NUMBER "~" NUMBER "]" ;
UNIT      → "GL" | "ML" | "kL" | "L" ;
```

Doing this in the scanner keeps the parser simple. Whether `+` means flow addition or number addition is decided by the interpreter, not the parser.

## Examples

| File | What it shows |
|---|---|
| [`examples/tiny.river`](examples/tiny.river) | One flow, printed |
| [`examples/confluence.river`](examples/confluence.river) | A small network of five named rivers joined with `+` |
| [`examples/canberra.river`](examples/canberra.river) | The Canberra river system with two dams. It parses, but stops at runtime because dam declarations don't exist yet |

## Project structure

```
src/riverlang/
  Scanner.java        source text to tokens
  Token.java          a token with type, lexeme, literal and line
  TokenType.java      all token types
  FlowLiteral.java    the value inside a flow[...] literal
  Parser.java         recursive-descent parser, tokens to statements
  Expr.java           expression AST nodes (Visitor pattern)
  Stmt.java           statement AST nodes (Visitor pattern)
  AstPrinter.java     prints an expression tree, for debugging
  Interpreter.java    tree-walking evaluator
  Environment.java    variable storage
  RunTimeError.java   runtime error with a source line
  Lox.java            entry point: file runner and prompt
examples/             sample programs
```

## How it works

1. **Scan.** `Scanner` turns source text into tokens, including whole `flow[...]` literals.
2. **Parse.** `Parser` builds a list of statements from the tokens using recursive descent.
3. **Evaluate.** `Interpreter` walks the tree. A flow is a list of 10 numbers, one per day, and `+` adds two such lists.

## Limitations and roadmap

- `dam` declarations, and the `when`, `default`, `inflow` and `level` rules for deciding how much water a dam releases, are reserved keywords but not implemented yet.
- `<-` parses, but evaluating it is not implemented, so `canberra.river` stops with an "undefined river or dam" error.
- Variables live in one flat global scope. There are no blocks, functions or local scope yet.
- After 10 days a small fraction of the water can remain, because each day releases a share of what is left rather than emptying exactly.
- Rainfall is not an input. Each flow literal carries its own amount.

## Background

Built from the Lox interpreter in Robert Nystrom's [*Crafting Interpreters*](https://craftinginterpreters.com/) as part of a university programming languages course. The scanner, parser, AST and interpreter follow the structure of the book's chapters 4 to 8. The flow literal, units, `+` on flows, `<-` and the river domain are additions for this language.
