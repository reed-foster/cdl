# treevisitor.py - Reed Foster
# Utility for visiting tree nodes

from parser import *
from enums import *
from lexer import *
from ast import *

class Visitor(object):

    def __init__(self):
        self.compname = ''
        self.subcomps = {}
        self.signals = {}
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

    def visitCompInst(self, node, depth):
        name = self.visit(node.name, depth + 1)
        comptype = self.visit(node.comptype, depth + 1)
        genericmap = 'generic map\n(\n' + self.indent(self.visit(node.generics, depth + 1).replace(' , ', ',\n').replace(' = ', ' => ')) + '\n)'
        portmap = ''
        for port in self.portmaps[name]:
            sep = ',\n' if len(portmap) > 1 else ''
            portmap += sep + port + ' => ' + self.portmaps[name][port]
        portmap = 'port map\n(\n' + self.indent(portmap) + '\n)'
        return name + ' : ' + comptype + '\n' + self.indent(genericmap) + '\n' + self.indent(portmap)

    def visitSignal(self, node, depth):
        name = self.visit(node.name, depth + 1)
        sigtype = node.sigtype.value
        self.signals[name] = sigtype
        return 'signal ' + name + ' : ' + sigtype

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
                    self.tempsigs[signalname] = Signal(Identifier(Token(ID, signalname)), Token(TYPE, 'placeholder'))
                    self.portmaps[comp][port] = signalname
                    node.children[node.children.index(item)].left = Identifier(Token(ID, signalname)) # replace port with temp signal
                else:
                    right = self.visit(item.right, depth + 1)
                    if '.' in right:
                        # right side of assignment has a subcomponent port
                        # janky solution for now: replace right side with Identifier node containing string of parsed right side of assingment
                        for term in right.split(' '):
                            if '.' in term:
                                comp, port = term.split('.')
                                signalname = '_'.join((comp, port))
                                self.tempsigs[signalname] = Signal(Identifier(Token(ID, signalname)), Token(TYPE, 'placeholder'))
                                self.portmaps[comp][port] = signalname
                        node.children[node.children.index(item)].right = Identifier(Token(ID, right.replace('.', '_')))

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
            CompType foobar = new CompType(gen1 = 4, gen2 = x"5");
            foo <= foobar.foobarout + 5;
            foobar.foobarin <= foo;
        }
    }
    ''')
    parse = Parser(lex)

    tree = parse.component()

    print vis.visit(tree, 0)

test()