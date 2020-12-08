import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbol_table = new HashMap<String, SymbolEntry>();

    HashMap<String, Function> function_table = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     *
     * @throws TokenizeError
     * @return
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        return token == tt;
    }

    private boolean checkLiteral() throws TokenizeError {
        TokenType tt = peek().getTokenType();
        return tt == TokenType.UINT_LITERAL || tt == TokenType.DOUBLE_LITERAL || tt == TokenType.STRING_LITERAL || tt == TokenType.CHAR_LITERAL;
    }

    private boolean checkBinaryOperator() throws TokenizeError {
        TokenType tt = peek().getTokenType();
        return tt == TokenType.PLUS || tt == TokenType.MINUS || tt == TokenType.MUL || tt == TokenType.DIV
            || tt == TokenType.EQ || tt == TokenType.NEQ || tt == TokenType.LT || tt == TokenType.GT
            || tt == TokenType.LE || tt == TokenType.GE;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType... tt) throws CompileError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
        }
        for (TokenType t : tt) {
            if (token == t) {
                return next();
            }
        }
        throw new ExpectedTokenError(Format.generateList(tt), peek());
    }

    /**
     * 获取下一个变量的栈偏移
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbol_table.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbol_table.put(name, new SymbolEntry(SymbolType.Const, name, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.symbol_table.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.symbol_table.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.symbol_table.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getType() == SymbolType.Const;
        }
    }

    private void analyseProgram() throws CompileError {
        while(!check(TokenType.EOF)){
            if(check(TokenType.FN_KW)){
                analyseFunction();
            }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                analyseDeclStmt();
            }else{
                throw new ExpectedTokenError(
                    Format.generateList(TokenType.LET_KW, TokenType.CONST_KW, TokenType.FN_KW), next());
            }
        }
        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError{
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);

        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN))
            analyseFunctionParamList();
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        analyseBlockStmt();
    }

    private void analyseBlockStmt() throws CompileError{
        expect(TokenType.L_BRACE);
        while(!check(TokenType.R_BRACE)){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
    }

    private void analyseStmt() throws CompileError{
        if(check(TokenType.LET_KW) || check((TokenType.CONST_KW))){
            analyseDeclStmt();
        } else if(check(TokenType.IF_KW)){
            analyseIfStmt();
        } else if(check(TokenType.WHILE_KW)){
            analyseWhileStmt();
        } else if(check(TokenType.BREAK_KW)){
            analyseBreakStmt();
        } else if(check(TokenType.CONTINUE_KW)){
            analyseContinueStmt();
        } else if(check(TokenType.RETURN_KW)){
            analyseReturnStmt();
        } else if(check(TokenType.L_BRACE)){
            analyseBlockStmt();
        } else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
        } else{
            analyseExprStmt();
        }
    }

    private void analyseExprStmt() throws CompileError{
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }
    /*
    expr ->
      operator_expr
    | negate_expr
    | assign_expr
    | as_expr
    | call_expr
    | literal_expr
    | ident_expr
    | group_expr

    expr
    operator_expr -> expr binary_operator expr
    as_expr -> expr 'as' ty

    expr -> 其他的子expr {binary_operator expr | 'as' ty} *

    negate_expr -> '-' expr
    group_expr -> '(' expr ')'

    IDENT
    assign_expr -> '=' expr
    ident_expr ->
    call_expr -> '(' call_param_list? ')'

    call_param_list -> expr (',' expr)*

    literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL

    ty -> IDENT
     */
    private void analyseExpr() throws CompileError{
        if(check(TokenType.MINUS)){
            analyseNegateExpr();
        } else if(check(TokenType.L_PAREN)){
            analyseGroupExpr();
        } else if(check(TokenType.IDENT)){
            expect(TokenType.IDENT);

            if(check(TokenType.L_PAREN)) {
                analyseCallExpr();
            } else if(check(TokenType.ASSIGN)) {
                analyseAssignExpr();
            } else{
                analyseIdentExpr();
            }
        } else if(checkLiteral()){
            expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        } else{
            analyseLiteralExpr();
        }

        while(checkBinaryOperator() || check(TokenType.AS_KW)){
            if(checkBinaryOperator()) {
                analyseOperatorExpr();
            }
            else if(check(TokenType.AS_KW)){
                analyseAsExpr();//没有语句
            }
        }
    }

    private void analyseLiteralExpr() throws CompileError{
        expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
    }

    private void analyseAsExpr() throws CompileError{
        expect(TokenType.AS_KW);
        Token ty = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);;
    }

    private void analyseOperatorExpr() throws CompileError{
        expect(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE);
        analyseExpr();
    }

    private void analyseIdentExpr() throws CompileError{

    }

    private void analyseAssignExpr() throws CompileError{
        expect(TokenType.ASSIGN);
        analyseExpr();
    }

    private void analyseCallExpr() throws CompileError{
        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN)) {
            analyseCallParamList();
        }
        expect(TokenType.R_PAREN);
    }

    private void analyseCallParamList() throws CompileError{
        analyseExpr();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseExpr();
        }
    }

    private void analyseGroupExpr() throws CompileError{
        expect(TokenType.L_PAREN);
        analyseExpr();
        expect(TokenType.R_PAREN);
    }

    private void analyseNegateExpr() throws CompileError{
        expect(TokenType.MINUS);
        analyseExpr();
    }

    private void analyseReturnStmt() throws CompileError{
        expect(TokenType.RETURN_KW);
        if(!check(TokenType.SEMICOLON)){
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseContinueStmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
    }

    private void analyseBreakStmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
    }

    private void analyseWhileStmt() throws CompileError{
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlockStmt();
    }

    private void analyseIfStmt() throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpr();
        analyseBlockStmt();
        while(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if(check(TokenType.IF_KW)){
                expect(TokenType.IF_KW);
                analyseExpr();
                analyseBlockStmt();
            }else{
                analyseBlockStmt();
                break;
            }
        }
    }

    private void analyseFunctionParamList() throws CompileError{
        analyseFunctionParam();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }
    }

    private void analyseFunctionParam() throws CompileError{
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
        }
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);

    }

    private void analyseDeclStmt() throws CompileError{
        if(check(TokenType.LET_KW)){
            analyseLetDeclStmt();
        } else if (check(TokenType.CONST_KW)){
            analyseConstDeclStmt();
        }
    }

    private void analyseConstDeclStmt() throws CompileError{
        expect(TokenType.CONST_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);;
        expect(TokenType.ASSIGN);
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }

    private void analyseLetDeclStmt() throws CompileError{
        expect(TokenType.LET_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);;
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }
}
