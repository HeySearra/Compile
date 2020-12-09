
public enum TokenType {
    /** 空 */
    None, IDENT, PLUS, MINUS, MUL, DIV, ASSIGN, EQ, NEQ, LT, GT, LE, GE,
    L_PAREN, R_PAREN, EOF, L_BRACE, R_BRACE, ARROW, COMMA, COLON, SEMICOLON,
    FN_KW, LET_KW, CONST_KW, AS_KW, WHILE_KW, IF_KW, ELSE_KW, RETURN_KW, BREAK_KW, CONTINUE_KW,
    INT_KW, VOID_KW, DOUBLE_KW,
    UINT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL, CHAR_LITERAL,
    COMMENT,
    ;

    @Override
    public String toString() {
        switch (this) {
            case None:
                return "NullToken";
            case L_BRACE:
                return "left breace";
            case ARROW:
                return "arrow";
            case COLON:
                return "colon";
            case COMMA:
                return "comma";
            case R_BRACE:
                return "right breace";
            case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case AS_KW:
                return "as";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case BREAK_KW:
                return "break";
            case CONTINUE_KW:
                return "continue";
            case CONST_KW:
                return "Const";
            case DIV:
                return "DivisionSign";
            case EOF:
                return "EOF";
            case ASSIGN:
                return "Assign";
            case IDENT:
                return "Ident";
            case L_PAREN:
                return "Left paren";
            case MINUS:
                return "MinusSign";
            case MUL:
                return "MultiplicationSign";
            case PLUS:
                return "PlusSign";
            case R_PAREN:
                return "Right Paren";
            case SEMICOLON:
                return "Semicolon";
            case UINT_LITERAL:
                return "unsigned integer";
            case DOUBLE_LITERAL:
                return "double";
            case STRING_LITERAL:
                return "string";
            case CHAR_LITERAL:
                return "char";
            case INT_KW:
                return "int kw";
            case VOID_KW:
                return "void kw";
            case DOUBLE_KW:
                return "double kw";
            case EQ:
                return "eq";
            case NEQ:
                return "neq";
            case LT:
                return "lt";
            case GT:
                return "gt";
            case LE:
                return "le";
            case GE:
                return "ge";
            case COMMENT:
                return "comment";
            default:
                return "InvalidToken";
        }
    }
}
