#include "Function.h"


Pulsar::Function::Function(std::string name, Pulsar::PrimitiveType type, std::vector<Parameter*>* parameters, Block* statements, int line): type(type) {
    this->name = name;
    this->parameters = parameters;
    this->statements = statements;
    this->line = line;
}

std::any Pulsar::Function::accept(Pulsar::StmtVisitor& visitor) {
    return visitor.visitFunctionStatement(this);
}

int Pulsar::Function::getArity() {
    return this->parameters->size();
}

std::string Pulsar::Function::getName() {
    return this->name;
}

Pulsar::PrimitiveType Pulsar::Function::getType() {
    return this->type;
}

std::vector<Pulsar::Parameter*>* Pulsar::Function::getParameters() {
    return this->parameters;
}

Pulsar::Block* Pulsar::Function::getStatements() {
    return this->statements;
}

int Pulsar::Function::getLine() {
    return this->line;
}
