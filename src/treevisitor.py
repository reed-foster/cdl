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
        output = '(' + self.visit(node.left, depth + 1) + ') when (' + self.visit(node.boolean, depth + 1) + ') else (' + self.visit(node.right, depth + 1) + ')'
        return output

    def visitBinaryOp(self, node, depth):
        output = self.visit(node.left, depth + 1) + ' ' + node.op.value + ' ' + self.visit(node.right, depth + 1)
        return output

    def visitUnaryOp(self, node, depth):
        if node.op.type == 'PAREN':
            output = '(' + self.visit(node.right, depth + 1) + ')'
        else:
            output = node.op.value + ' ' + self.visit(node.right, depth + 1)
        return output

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
        return 'component ' + self.visit(node.name, depth + 1) + '\n' + '  ' * (depth + 1) + self.visit(node.body, depth + 2)

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
        return node.token.value

    def visitConstant(self, node, depth):
        return node.token.value

def test():

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
            cat <= (5 <= 3) ? fox : banana;
            moo <= fox - banana and 2;
            CompType compinst = new CompType(lol = 3, foo = 5, banana = x"4");
            foo <= ((5 xor 7 > foo) ^ true) ? moo : fox - (7 * banana) + 2;
        }
    }
    ''')
    parse = Parser(lex)

    tree = parse.component()

    print vis.visit(tree, 0)
    print ''
    print parse.genlist
    print parse.varlist
    print parse.siglist

test()