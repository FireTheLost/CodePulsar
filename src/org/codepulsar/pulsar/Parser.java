package org.codepulsar.pulsar;

import org.codepulsar.primitives.*;

import java.util.ArrayList;

import static org.codepulsar.pulsar.TokenType.*;
import static org.codepulsar.pulsar.ByteCode.*;

public class Parser {
    private final String sourceCode;
    private ArrayList<Token> tokens;
    private final ArrayList<Instruction> instructions;
    public static ArrayList<Primitive> values;
    private int current;
    public final ArrayList<Error> errors;
    boolean hasErrors;

    public Parser(String sourceCode) {
        this.sourceCode = sourceCode;
        this.tokens = new ArrayList<>();
        this.instructions = new ArrayList<>();
        values = new ArrayList<>();
        this.current = 0;
        this.errors = new ArrayList<>();
        this.hasErrors = false;
    }

    public ArrayList<Instruction> parse() {
        Lexer lexer = new Lexer(this.sourceCode + "\n");
        this.tokens = lexer.tokenize();

        if (CommandsKt.getDebug()) {
            Disassembler.tokens(this.tokens);
        }

        while (!match(TK_EOF)) {
            expressionStatement();
        }

        return this.instructions;
    }

    private void expressionStatement() {
        expression();
        makeOpCode(OP_POP, peekLine());
        look(TK_SEMICOLON, "A Semicolon Was Expected After The Expression", "Missing Character");
    }

    private void expression() {
        assignment();
    }

    private void assignment() {
        if (match(TK_IDENTIFIER)) {
        } else {
            logicalOr();
        }
    }

    private void logicalOr() {
        logicalAnd();
        int offset;

        while (match(TK_LOGICALOR)) {
            if (peekType() == TK_LOGICALOR) {
                int line = peekLine();
                advance();
                offset = makeJump(OP_JUMP_IF_TRUE);
                makeOpCode(OP_POP, line);
                logicalAnd();
                fixJump(offset, OP_JUMP_IF_TRUE);
            }
        }
    }

    private void logicalAnd() {
        equality();
        int offset;

        while (match(TK_LOGICALAND)) {
            if (peekType() == TK_LOGICALAND) {
                int line = peekLine();
                advance();
                offset = makeJump(OP_JUMP_IF_FALSE);
                makeOpCode(OP_POP, line);
                equality();
                fixJump(offset, OP_JUMP_IF_FALSE);
            }
        }
    }

    private void equality() {
        comparison();

        while (match(TK_EQUALEQUAL, TK_NOTEQUAL)) {
            if (peekType() == TK_EQUALEQUAL) {
                advance();
                comparison();
                makeOpCode(OP_COMPARE_EQUAL, peekLine());
            } else if (peekType() == TK_NOTEQUAL) {
                advance();
                comparison();
                makeOpCode(OP_COMPARE_EQUAL, peekLine());
                makeOpCode(OP_NOT, peekLine());
            }
        }
    }

    private void comparison() {
        term();

        while (match(TK_GT, TK_GTEQUAL, TK_LT, TK_LTEQUAL)) {
            if (peekType() == TK_GT) {
                advance();
                term();
                makeOpCode(OP_GREATER, peekLine());
            } else if (peekType() == TK_LT) {
                advance();
                term();
                makeOpCode(OP_LESSER, peekLine());
            } else if (peekType() == TK_GTEQUAL) {
                advance();
                term();
                makeOpCode(OP_LESSER, peekLine());
                makeOpCode(OP_NOT, peekLine());
            } else if (peekType() == TK_LTEQUAL) {
                advance();
                term();
                makeOpCode(OP_GREATER, peekLine());
                makeOpCode(OP_NOT, peekLine());
            }
        }
    }

    private void term() {
        factor();

        while (match(TK_PLUS, TK_MINUS)) {
            if (peekType() == TK_PLUS) {
                advance();
                factor();
                makeOpCode(OP_ADD, peekLine());
            } else if (peekType() == TK_MINUS) {
                advance();
                factor();
                makeOpCode(OP_SUBTRACT, peekLine());
            }
        }
    }

    private void factor() {
        unary();

        while (match(TK_MULTIPLICATION, TK_DIVISION, TK_MODULUS)) {
            if (peekType() == TK_MULTIPLICATION) {
                advance();
                unary();
                makeOpCode(OP_MULTIPLY, peekLine());
            } else if (peekType() == TK_DIVISION) {
                advance();
                unary();
                makeOpCode(OP_DIVIDE, peekLine());
            } else if (peekType() == TK_MODULUS) {
                advance();
                unary();
                makeOpCode(OP_MODULO, peekLine());
            }
        }
    }

    private void unary() {
        if (matchAdvance(TK_NOT)) {
            unary();
            makeOpCode(OP_NOT, peekLine());
        } else if (matchAdvance(TK_MINUS)) {
            unary();
            makeOpCode(OP_NEGATE, peekLine());
        } else {
            primary();
        }
    }

    private void primary() {
        if (match(TK_INTEGER, TK_DOUBLE, TK_TRUE, TK_FALSE, TK_NULL)) {
            makeOpCode(OP_CONSTANT, peekLine());
            advance();
        }
    }

    private Instruction makeOpCode(ByteCode opcode, int line) {
        if (opcode == OP_CONSTANT) {
            Primitive lr = new Primitive();
            switch (peekType()) {
                case TK_INTEGER -> lr = new PInteger(Integer.parseInt(peekLiteral()));
                case TK_DOUBLE -> lr = new PDouble(Double.parseDouble(peekLiteral()));
                case TK_TRUE -> lr = new PBoolean(true);
                case TK_FALSE -> lr = new PBoolean(false);
                case TK_NULL -> lr = new PNull();
            }
            values.add(lr);
            Instruction instruction = new Instruction(OP_CONSTANT, values.size() - 1, line);
            this.instructions.add(instruction);
            return instruction;
        } else {
            Instruction instruction = new Instruction(opcode, null, line);
            this.instructions.add(instruction);
            return instruction;
        }
    }

    private Instruction makeOpCode(ByteCode opcode, Object operand, int line) {
        Instruction instruction = new Instruction(opcode, operand, line);
        this.instructions.add(instruction);
        return instruction;
    }

    private int makeJump(ByteCode opcode) {
        int size = this.instructions.size();

        makeOpCode(opcode, peekLine());

        return size;
    }

    private void fixJump(int offset, ByteCode opcode) {
        Instruction oldJump = this.instructions.get(offset);
        int line = oldJump.getLine();
        Instruction jumpOpCode = new Instruction(opcode, this.instructions.size(), line);
        this.instructions.set(offset, jumpOpCode);
    }

    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (peekType() == type) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAdvance(TokenType... types) {
        for (TokenType type: types) {
            if (peekType() == type) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        this.current++;
        return this.tokens.get(this.current - 1);
    }

    private void look(TokenType token, String message, String errorType) {
        if (peek().getTtype() != token) {
            setErrors(errorType, message, peek());
            synchronize();
        } else {
            advance();
        }
    }

    private void synchronize() {
        while (peek().getTtype() != TK_EOF) {
            if (peek().getTtype() == TK_SEMICOLON) {
                advance();
                return;
            }

            advance();
        }
    }

    private void setErrors(String etype, String message, Token token) {
        this.hasErrors = true;
        Error error = new Error(etype, message, token);
        this.errors.add(error);
    }

    private Token peek() {
        return this.tokens.get(this.current);
    }

    private TokenType peekType() {
        return this.tokens.get(this.current).getTtype();
    }

    private int peekLine() {
        return this.tokens.get(this.current).getLine();
    }

    private String peekLiteral() {
        return this.tokens.get(this.current).getLiteral();
    }
}
