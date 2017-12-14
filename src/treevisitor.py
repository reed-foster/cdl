# treevisitor.py - Reed Foster
# Utility for visiting tree nodes

from parser import *
from enums import *
from lexer import *
from ast import *

class Visitor(object):
    def visit(self, node, depth):
        methodname = 'visit' + type(node).__name__
        visitorfunction = getattr(self, methodname, self.genericvisit)
        return visitorfunction(node, depth)

    def genericvisit(self, node, depth):
        raise Exception('no visit{} method'.format(type(node).__name__))

    def visitTernaryOp(self, node, depth):
        return 'ternary ?\n' + '  ' * depth + self.visit(node.boolean, depth + 1) + '\n' + '  ' * depth + self.visit(node.left, depth + 1) + '\n' + self.visit(node.right, depth + 1)

    def visitBinaryOp(self, node, depth):
        return 'binary ' + node.op.value + '\n' + '  ' * depth + self.visit(node.left, depth + 1) + '\n' + '  ' * depth + self.visit(node.right, depth + 1)

    def visitUnaryOp(self, node, depth):
        return 'unary ' + node.op.value + '\n' + '  ' * depth + self.visit(node.right, depth + 1)

    def visitCompInst(self, node, depth):
        return 'compinst ' + self.visit(node.name, depth + 1) + '\n' + '  ' * depth + self.visit(node.generics, depth + 1)

    def visitSignal(self, node, depth):
        return 'sigdec ' + self.visit(node.name, depth + 1) + ' type = ' + node.sigtype.value

    def visitVariable(self, node, depth):
        return 'vardec ' + self.visit(node.name, depth + 1) + ' type = ' + node.vartype.value

    def visitGeneric(self, node, depth):
        return 'gendec ' + self.visit(node.name, depth + 1) + ' type = ' + node.gentype.value

    def visitPortList(self, node, depth):
        string = 'portlist'
        for port in node.children:
            string += '\n' + '  ' * depth + self.visit(port, depth + 1)
        return string

    def visitPort(self, node, depth):
        return 'portdec ' + self.visit(node.name, depth + 1) + ' type = ' + node.porttype.value + ' dir = ' + node.direction.value

    def visitComponent(self, node, depth):
        return 'component ' + self.visit(node.name, depth + 1) + '\n' + '  ' * depth + self.visit(node.body, depth + 1)

    def visitComponentBody(self, node, depth):
        string = 'componentbody'
        for item in node.children:
            string += '\n' + '  ' * depth + self.visit(item, depth + 1)
        return string

    def visitArch(self, node, depth):
        return 'arch ' + self.visit(node.name, depth + 1) + '\n' + '  ' * depth + self.visit(node.body, depth + 1)

    def visitArchBody(self, node, depth):
        string = 'archbody'
        for item in node.children:
            string += '\n' + '  ' * depth + self.visit(item, depth + 1)
        return string

    def visitIdentifier(self, node, depth):
        return 'id ' + node.token.value

    def visitConstant(self, node, depth):
        return 'const ' + node.token.value

vis = Visitor()

lex = Lexer(
'''
component CompName
{
    int genericint;
    bool genericbool;
    vec genericvec;

    port
    {
        input int inputint;
        input vec inputvec;
        output bool outputbool;
    }

    arch
    {
        signal vec foo;
        foo <= fox < banana;
        CompType compinst = new CompType(lol = 3, foo = 5, banana = x"4");
    }
}
''')
parse = Parser(lex)

tree = parse.component()

#print tree.name.token
#print type(tree.name.token)

string = vis.visit(tree, 0)
print string