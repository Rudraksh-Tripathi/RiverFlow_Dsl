package River;


import static River.TokenTypes.*;

import javax.management.RuntimeErrorException;

class interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RunTimeError error) {
            Lox.runtimeError(error);
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    public Object VisitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    public Object VisitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    public Object VisitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.types) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
                default:
                    break;
        }
        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private void checkNumberOperand(Tokens operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RunTimeError(operator, "Operand is not a number.");
    }

    private void checkNumberOperands(Tokens operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RunTimeError(operator, "Operands are not numbers.");
    }

    public Object VisitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        Switch (expr.operator.types) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                if (left instanceof FlowLiteral && right instanceof FlowLiteral) {
                    return combineFlows(expr.operator, (FlowLiteral) left, (FlowLiteral) right);
                }
                throw new RunTimeError(expr.operator, "Operands used are not 2 numbers or 2 strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case ARROW:
                throw new RunTimeError(expr.operator, "Arrow operator is not supported in this method.");
            default:
                break;
        }
        return null;
    }

    public Object visitVariableExpr(Expr.Variable expr){
        throw new RuntimeError(expr.name,"undefined variable" + expr.name.lexeme + "add variable storage later");

    } 

    public Object visitWaterFlowExpr(Expr.WaterFlow expr){
        return expr.value;
    }

    private FlowLiteral combineFlows(Tokens operator, FlowLiteral left, FlowLiteral right){
        if (!.left.unit.equals(right.unit)){
                RuntimeError(operator,
                    "Cannot combine flows with different units ('" + left.unit + "' vs '" + right.unit + "').");
        }
        double amount = left.amount + right.amount;
        double startDay = Math.min(left.startDay, right.startDay);
        double spread = Math.max(left.spread, right.spread);
        return new FlowLiteral(amount, left.unit, startDay, spread);
    
        }
    }

