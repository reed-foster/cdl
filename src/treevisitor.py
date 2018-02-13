# treevisitor.py - Reed Foster
# Utility for visiting tree nodes

import parser
import lexer
import ast
from enums import *

class Visitor(object):

    def __init__(self):
        self.compname = ''
        self.subcomps = {}
        self.tempsigs = {}
        self.portmaps = {}

    def visit(self, node, depth):
        methodname = 'visit' + type(node).__name__
        visitorfunction = getattr(self, methodname, self.genericvisit)
        return visitorfunction(node, depth)

    def genericvisit(self, node, depth):
        raise Exception('no visit{} method'.format(type(node).__name__))

    def indent(self, string):
        return '    ' + string.replace('\n', '\n    ')

    def removeblanklines(self, string):
        return '\n'.join([line for line in string.split('\n') if line.rstrip()])

    def visitTernaryOp(self, node, depth):
        output = '(' + self.visit(node.left, depth + 1) + ') when (' + self.visit(node.boolean, depth + 1) + ') else (' + self.visit(node.right, depth + 1) + ')'
        return output

    def visitBinaryOp(self, node, depth):
        sep = '' if node.op.value == '.' else ' '
        output = self.visit(node.left, depth + 1) + sep + node.op.value + sep + self.visit(node.right, depth + 1)
        return output

    def visitUnaryOp(self, node, depth):
        if node.op.type == 'PAREN':
            output = '(' + self.visit(node.right, depth + 1) + ')'
        else:
            output = node.op.value + ' ' + self.visit(node.right, depth + 1)
        return output

    def visitSplice(self, node, depth):
        topidx = self.visit(node.top, depth + 1)
        bottomidx = self.visit(node.bottom, depth + 1)
        splicestr = topidx + (' downto ' + bottomidx if topidx != bottomidx else '')
        return self.visit(node.identifier, depth + 1) + '(' + splicestr + ')'

    def visitCompInst(self, node, depth):
        name = self.visit(node.name, depth + 1)
        comptype = self.visit(node.comptype, depth + 1)
        genericmap = ''
        if node.generics is not None:
            genericmap = 'generic map\n(\n' + self.indent(self.visit(node.generics, depth + 1).replace(' , ', ',\n').replace(' = ', ' => ')) + '\n)'
        portmap = ''
        for port in self.portmaps[name]:
            sep = ',\n' if len(portmap) > 1 else ''
            portmap += sep + port + ' => ' + self.portmaps[name][port]
        portmap = 'port map\n(\n' + self.indent(portmap) + '\n)'
        return name + ' : ' + comptype + '\n' + (self.indent(genericmap) if genericmap is not None else '') + '\n' + self.indent(portmap)

    def genvecwidth(self, width):
        try:
            return str(int(width) - 1)
        except ValueError:
            return width + ' - 1'

    def visitSignal(self, node, depth):
        name = self.visit(node.name, depth + 1)
        sigtype = node.sigtype.value
        width = '' if node.width is None else '(' + self.genvecwidth(node.width) + ' downto 0)'
        return 'signal ' + name + ' : ' + sigtype + width

    def visitVariable(self, node, depth):
        width = '' if node.width is None else '(' + self.genvecwidth(node.width) + ' downto 0)'
        return 'variable ' + self.visit(node.name, depth + 1) + ' : ' + node.vartype.value + width

    def visitGeneric(self, node, depth):
        width = '' if node.width is None else '(' + str(int(node.width) - 1) + ' downto 0)'
        return self.visit(node.name, depth + 1) + ' : ' + node.gentype.value + width

    def visitPortList(self, node, depth):
        ports = ''
        for port in node.children:
            sep = ';\n' if len(ports) > 1 else ''
            ports += sep + self.visit(port, depth + 1)
        string = 'port\n(\n' + self.indent(ports) + '\n);'
        return string

    def visitPort(self, node, depth):
        width = '' if node.width is None else '(' + self.genvecwidth(node.width) + ' downto 0)'
        return self.visit(node.name, depth + 1) + ' : ' + node.direction.value[:-3] + ' ' * (7 - len(node.direction.value)) + node.porttype.value + width

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
        string = self.removeblanklines(self.indent(string) + '\nend entity;')
        string += '\n\n' + arch
        return string

    def visitArch(self, node, depth):
        return self.removeblanklines('architecture ' + self.visit(node.name, depth + 1) + ' of ' + self.compname + ' is\n' + self.visit(node.body, depth + 1) + '\nend architecture;')

    def visitArchBody(self, node, depth):
        # This method can probably be optimized (needs at least 2 passes; 4 right now, might be able to make that 3)
        body = ''
        sigdecs = ''
        compdecs = ''
        compinsts = ''
        # initial pass for compiling list of compinsts and portmaps
        for item in node.children:
            if type(item).__name__ == 'CompInst':
                name = self.visit(item.name, depth + 1)
                self.subcomps[name] = self.visit(item.comptype, depth + 1)
                self.portmaps[name] = {}
        # replace instances of ports with temp signals
        for item in node.children:
            if type(item).__name__ == 'BinaryOp' and item.token.type == SIGASSIGN:
                # current line is a signal assignment
                # TODO: check sources for subcomponents to get port types and widths as well as verify port direction
                if type(item.left).__name__ == 'BinaryOp' and item.left.token.type == PERIOD:
                    # left side of assingment is a subcomponent port
                    identifier = self.visit(item.left, depth + 1)
                    comp, port = identifier.split('.')
                    signalname = '_'.join((comp, port))
                    self.tempsigs[signalname] = ast.Signal(ast.Identifier(token.Token(ID, signalname)), token.Token(TYPE, 'placeholder'))
                    self.portmaps[comp][port] = signalname
                    node.children[node.children.index(item)].left = ast.Identifier(token.Token(ID, signalname)) # replace port with temp signal
                else:
                    right = self.visit(item.right, depth + 1)
                    if '.' in right:
                        # right side of assignment has a subcomponent port
                        # janky solution for now: replace right side with Identifier node containing string of generated VHDL
                        for term in right.split(' '):
                            if '.' in term:
                                comp, port = term.split('.')
                                signalname = '_'.join((comp, port))
                                self.tempsigs[signalname] = ast.Signal(ast.Identifier(token.Token(ID, signalname)), token.Token(TYPE, 'placeholder'))
                                self.portmaps[comp][port] = signalname
                        node.children[node.children.index(item)].right = ast.Identifier(token.Token(ID, right.replace('.', '_')))

        # add signal declarations for temp signals
        for signal in self.tempsigs:
            sep = '\n' if len(sigdecs) > 1 else ''
            sigdecs += sep + self.visit(self.tempsigs[signal], depth + 1) + ';'
        # generate vhdl archbody source
        for item in node.children:
            if type(item).__name__ == 'Signal':
                sep = '\n' if len(sigdecs) > 1 else ''
                sigdecs += sep + self.visit(item, depth + 1) + ';'
            elif type(item).__name__ == 'CompInst':
                sep = '\n' if len(compinsts) > 1 else ''
                compinsts += sep + self.visit(item, depth + 1) + ';'
            else:
                sep = '\n' if len(body) > 1 else ''
                body += sep + self.visit(item, depth + 1) + ';'
        return self.indent(sigdecs) + '\nbegin\n' + self.indent(compinsts) + '\n' + self.indent(body)

    def visitIdentifier(self, node, depth):
        return node.token.value

    def visitConstant(self, node, depth):
        return node.token.value

def test():

    multisrc = '''
    component XorGate
    {
        port
        {
            input vec A;
            input vec B;
            output vec S;
        }

        arch
        {
            S <= A xor B;
        }
    }

    component AndGate
    {
        port
        {
            input vec A;
            input vec B;
            output vec S;
        }

        arch
        {
            S <= A and B;
        }
    }

    component HalfAdder
    {
        port
        {
            input vec A;
            input vec B;
            output vec S;
            output vec Co;
        }

        arch
        {
            XorGate XOR = new XorGate();
            AndGate AND = new AndGate();
            XOR.A <= A;
            XOR.B <= B;
            AND.A <= A;
            AND.B <= B;
            S <= XOR.S;
            Co <= AND.S;
        }
    }
    '''

    splicesrc = '''
    component SpliceTest
    {
        port
        {
        }
        arch
        {
            signal vec testsig;
            testsig[3:2] <= testsig[2] & testsig[3];
        }
    }
    '''

    src = splicesrc

    #components = ['component' + component for component in src.split('component')]

    vis = Visitor()

    lex = lexer.Lexer(src)
    parse = parser.Parser(lex)

    complist = parse.parse()

    for component in complist:
        print vis.visit(complist[component], 0)
        vis.compname = ''
        vis.subcomps = {}
        vis.signals = {}
        vis.tempsigs = {}
        vis.portmaps = {}
        print '\n'

test()