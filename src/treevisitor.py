# treevisitor.py - Reed Foster
# Utility for visiting tree nodes

from parser import *
from enums import *
from lexer import *
from ast import *

class Visitor(object):

    def __init__(self):
        self.compname = ''

    def visit(self, node, depth):
        methodname = 'visit' + type(node).__name__
        visitorfunction = getattr(self, methodname, self.genericvisit)
        return visitorfunction(node, depth)

    def genericvisit(self, node, depth):
        raise Exception('no visit{} method'.format(type(node).__name__))

    def indent(self, string):
        return '    ' + string.replace('\n', '\n    ')

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
        return 'signal ' + self.visit(node.name, depth + 1) + ' : ' + node.sigtype.value

    def visitVariable(self, node, depth):
        return 'variable ' + self.visit(node.name, depth + 1) + ' : ' + node.vartype.value

    def visitGeneric(self, node, depth):
        return self.visit(node.name, depth + 1) + ' : ' + node.gentype.value

    def visitPortList(self, node, depth):
        ports = ''
        for port in node.children:
            sep = ';\n' if len(ports) > 1 else ''
            ports += sep + self.visit(port, depth + 1)
        string = 'port\n(\n' + self.indent(ports) + '\n);'
        return string

    def visitPort(self, node, depth):
        return self.visit(node.name, depth + 1) + ' : ' + node.direction.value[:-3] + ' ' * (7 - len(node.direction.value)) + node.porttype.value

    def visitComponent(self, node, depth):
        self.compname = self.visit(node.name, depth + 1)
        return 'library ieee;\nuse ieee.std_logic_1164.all;\nuse ieee.numeric_std.all;\n\nentity ' + self.visit(node.name, depth + 1) + '\n' + self.visit(node.body, depth + 1)

    def visitComponentBody(self, node, depth):
        string = ''
        generics = ''
        port = ''
        for item in node.children:
            if type(item).__name__ == 'Generic':
                sep = ';\n' if len(generics) > 1 else ''
                generics += sep + self.visit(item, depth + 1)
            elif type(item).__name__ == 'PortList':
                port = self.visit(item, depth + 1)
            else:
                arch = self.visit(item, depth + 1)
        string += 'generic\n(\n' + self.indent(generics) + '\n);' if len(generics) > 1 else ''
        string += '\n' + port
        string = self.indent(string) + '\nend entity;'
        string += '\n\n' + arch
        return string

    def visitArch(self, node, depth):
        return 'architecture ' + self.visit(node.name, depth + 1) + ' of ' + self.compname + ' is\n' + self.visit(node.body, depth + 1) + '\nend architecture;'    

    def visitArchBody(self, node, depth):
        string = 'archbody'
        sigdecs = ''
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