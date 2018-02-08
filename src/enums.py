# enums.py - Reed Foster
# String enumerations for various keywords

from token import *

ID, SIGASSIGN, VARASSIGN, ASSIGN = ('ID', 'SIGASSIGN', 'VARASSIGN', 'ASSIGN')
TYPE, BITWISEOP, ARCHTYPE, PORTDIR = ('TYPE', 'BITWISEOP', 'ARCHTYPE', 'PORTDIR')
SIGNAL, VARIABLE = ('SIGNAL', 'VARIABLE')
DECINTCONST, BININTCONST, HEXINTCONST, BINVECCONST, HEXVECCONST, BOOLCONST = ('DECINTCONST', 'BININTCONST', 'HEXINTCONST', 'BINVECCONST', 'HEXVECCONST', 'BOOLCONST')
ADD, SUB, MUL, DIV, MOD, EXP = ('ADD', 'SUB', 'MUL', 'DIV', 'MOD', 'EXP')
AND, OR, XOR, NOT = ('AND', 'OR', 'XOR', 'NOT')
CONCAT, DOWNTO = ('CONCAT', 'DOWNTO')
LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET = ('LPAREN', 'RPAREN', 'LBRACE', 'RBRACE', 'LBRACKET', 'RBRACKET')
TERNQ, TERNSEP = ('TERNQ', 'TERNSEP')
LT, GT, LE, GE, EQ, NE = ('LT', 'GT', 'LE', 'GE', 'EQ', 'NE')
EOF, EOL = ('EOF', 'EOL')
COMMA, PERIOD = ('COMMA', 'PERIOD')

BOOLSCOPE, IDSCOPE, OTHERSCOPE = ('BOOLSCOPE', 'IDSCOPE', 'OTHERSCOPE')

RESERVED_KEYWORDS = {
    'component' : Token('COMPONENT', 'component'),
    'port' : Token('PORT','port'),
    'arch' : Token('ARCH','arch'),
    'generate' : Token('GENERATE','generate'),
    'process' : Token('PROCESS', 'process'),
    'signal' : Token(SIGNAL,'signal'),
    'variable' : Token(VARIABLE,'variable'),
    'new' : Token('NEW','new'),
    'if' : Token('IF','if'),
    'else' : Token('ELSE', 'else'),
    'for' : Token('FOR','for'),
    'while' : Token('WHILE','while'),
    'input' : Token(PORTDIR, 'input'),
    'output' : Token(PORTDIR, 'output'),
    'int' : Token(TYPE,'int'),
    'uint' : Token(TYPE, 'uint'),
    'vec' : Token(TYPE,'vec'),
    'bool' : Token(TYPE,'bool'),
    'true' : Token(BOOLCONST, 'true'),
    'false' : Token(BOOLCONST, 'false'),
    'and' : Token(BITWISEOP, 'and'),
    'or' : Token(BITWISEOP, 'or'),
    'not' : Token(BITWISEOP, 'not'),
    'nand' : Token(BITWISEOP, 'nand'),
    'nor' : Token(BITWISEOP, 'nor'),
    'xor' : Token(BITWISEOP, 'xor'),
    'xnor' : Token(BITWISEOP, 'xnor'),
    'implementation' : Token(ARCHTYPE, 'implementation'),
    'verification' : Token(ARCHTYPE, 'verification')
}