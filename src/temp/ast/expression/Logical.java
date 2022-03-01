package temp.ast.expression;

import temp.ast.Expression;
import temp.lang.Token;

public class Logical extends Expression {
    private final Expression left;
    private final Token operator;
    private final Expression right;

    public Logical(Expression left, Token operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public String toString() {
        return "(" + this.left + this.operator.getLiteral() + this.right + ")";
    }
}
