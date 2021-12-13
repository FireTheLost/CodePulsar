package org.codepulsar.pulsar;

import java.util.ArrayList;
import java.util.Arrays;

import static org.codepulsar.pulsar.TokenType.*;

public class Disassembler {
    public static void tokens(ArrayList<Token> tokens) {
        TokenType[] literals = {TK_STRING, TK_INTEGER, TK_DOUBLE, TK_IDENTIFIER};
        int line = 0;
        System.out.println("-- Tokens --");

        for (Token token: tokens) {
            if (token.getLine() == line) {
                System.out.print("            ");
            } else {
                line = token.getLine();
                System.out.print("\n" + line + "           ");
            }
            if (Arrays.asList(literals).contains(token.getTtype())) {
                System.out.println(token.getTtype() + spaces(token) + "(" + token.getLiteral() + ")");
            } else {
                System.out.println(token.getTtype());
            }
        }
    }

    public static void disassemble(ArrayList<Instruction> instructions) {
        int line = 0;
        System.out.println("\n-- Disassembled Bytecode --");

        for (Instruction instruction : instructions) {
            if (instruction.getLine() == line) {
                System.out.print("            ");
            } else {
                line = instruction.getLine();
                System.out.print("\n" + line + "           ");
            }
            decide(instruction);
        }
    }

    private static void decide(Instruction instruction) {
        switch (instruction.getOpcode()) {
            case OP_CONSTANT -> constant(instruction);
            case OP_ADD, OP_SUBTRACT, OP_MULTIPLY, OP_DIVIDE, OP_NEGATE, OP_NOT -> opcode(instruction);
        }
    }

    private static void opcode(Instruction instruction) {
        System.out.println(instruction.getOpcode() + spaces(instruction) + instruction.getLine());
    }

    private static void constant(Instruction instruction) {
        System.out.println("OP_CONSTANT" + spaces(instruction) + instruction.getOperand() + "  (" + Parser.values.get(instruction.getOperand()) + ")");
    }

    private static String spaces(Instruction instruction) {
        int length = instruction.getOpcode().toString().length();
        return giveSpaces(length);
    }

    private static String spaces(Token token) {
        int length = token.getTtype().toString().length();
        return giveSpaces(length);
    }

    private static String giveSpaces(int length) {
        StringBuilder spaces = new StringBuilder();
        for (int i = 20; i >= length; i--) {
            spaces.append(" ");
        }

        return spaces.toString();
    }
}
