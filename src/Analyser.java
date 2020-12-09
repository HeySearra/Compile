import java.util.*;
public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    Definition def_table;

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    // 暂存函数的相关内容
    List<Instruction> function_body;
    List<SymbolEntry> param_table;
    int param_slot;
    List<SymbolEntry> local_table;
    int local_slot;
    String name;
    TokenType return_type;

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
    private boolean check(TokenType... tt) throws TokenizeError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        for(TokenType t: tt){
            if(token == t){
                return true;
            }
        }
        return false;
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
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(name);
        if (sym == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else if(sym.isConstant()) {
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        }else if(sym.isFunction()) {
            throw new AnalyzeError(ErrorCode.AssignToFunction, curPos);
        }else{
            sym.setInitialized(true);
        }
    }

    private Instruction getVarOrParamAddress(Token token) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
        if(sym == null)
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        if(sym.getType() == SymbolType.Function)
            throw new AnalyzeError(ErrorCode.FunctionHasNoAddr, token.getStartPos());
        else if(sym.getType() == SymbolType.Param){
            return new Instruction(Operation.arga, (long)sym.getStackOffset());
        }else if(sym.getType() == SymbolType.Var) {
            return new Instruction(Operation.loca, (long)sym.getStackOffset());
        }else{
            return new Instruction(Operation.globa, (long)sym.getStackOffset());
        }
    }

    private void functionAddParam(TokenType tt, String name, Boolean is_const, Pos pos) throws AnalyzeError {
        for(SymbolEntry p: this.param_table){
            if(name.equals(p.getName())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
            }
        }
        this.def_table.addSymbol(name, SymbolType.Param, tt, true, false, pos, this.param_table.size(), null, 1);
        this.param_table.add(new SymbolEntry(SymbolType.Param, name, tt, true, is_const, 0, null, 1));
    }


    private void functionAddVar(TokenType tt, String name, Boolean is_const, Pos pos, int level) throws AnalyzeError {
        for(SymbolEntry p: this.param_table){
            if(name.equals(p.getName())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
            }
        }
        this.def_table.addSymbol(name, SymbolType.Var, tt, true, false, pos, this.local_table.size(), null, level);
        this.local_table.add(new SymbolEntry(SymbolType.Var, name, tt, true, is_const, 0, null, level));
    }

    private void generateInstruction(){

    }

    public Function getStartFunction() throws AnalyzeError {
        return this.def_table.generate();
    }

    private void analyseProgram() throws CompileError {
        this.def_table = new Definition();
        while(!check(TokenType.EOF)){
            if(check(TokenType.FN_KW)){
                analyseFunction();
            }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                this.instructions.addAll(analyseDeclStmt(1));
            }else{
                throw new ExpectedTokenError(
                    Format.generateList(TokenType.LET_KW, TokenType.CONST_KW, TokenType.FN_KW), next());
            }
        }
        this.def_table.instruction = this.instructions;
        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError{
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        this.function_body = new ArrayList<Instruction>();
        this.param_table = new ArrayList<SymbolEntry>();
        this.param_slot = 0;
        this.local_table = new ArrayList<SymbolEntry>();
        this.local_slot = 0;
        this.return_type = null;
        this.def_table.level = 1;

        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN))
            analyseFunctionParamList();
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Token return_tt = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        this.return_type = return_tt.getTokenType();
        this.def_table.addFunction(nameToken.getValueString(), return_tt.getTokenType(), nameToken.getStartPos(), 0, this.function_body);
        this.function_body = analyseBlockStmt(return_tt.getTokenType(), 1);
        this.def_table.getFunction(nameToken.getValueString()).setFunctionBody(this.function_body);
    }

    // return_type
    // todo: 在函数退出前要清符号表
    private List<Instruction> analyseBlockStmt(TokenType return_type, int level) throws CompileError{
        this.def_table.level = level;
        expect(TokenType.L_BRACE);
        List<Instruction> res_ins = new ArrayList<>();
        while(!check(TokenType.R_BRACE)){
            List<Instruction> res = analyseStmt(return_type, level);
            if(res != null){
                res_ins.addAll(res);
            }
        }
        expect(TokenType.R_BRACE);
        this.def_table.levelDown();
        return res_ins;
    }

    private List<Instruction> analyseStmt(TokenType return_type, int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(check(TokenType.LET_KW) || check((TokenType.CONST_KW))){
            res_ins.addAll(analyseDeclStmt(level));
            return null;
        } else if(check(TokenType.IF_KW)){
            res_ins.addAll(analyseIfStmt(level));
            return null;
        } else if(check(TokenType.WHILE_KW)){
            res_ins.addAll(analyseWhileStmt(level));
            return null;
        } else if(check(TokenType.BREAK_KW)){
            res_ins.addAll(analyseBreakStmt());
            return null;
        } else if(check(TokenType.CONTINUE_KW)){
            res_ins.addAll(analyseContinueStmt());
            return null;
        } else if(check(TokenType.RETURN_KW)){
            res_ins.addAll(analyseReturnStmt());
            return null;
        } else if(check(TokenType.L_BRACE)){
            this.def_table.level = level + 1;
            res_ins.addAll(analyseBlockStmt(return_type, level + 1));
            return null;
        } else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
        } else{
            res_ins.addAll(analyseExprStmt());
            res_ins.addAll(ExprTree.addAllReset());
        }
        return res_ins;
    }

    private List<Instruction> analyseExprStmt() throws CompileError{
        List<Instruction> res_ins = analyseExpr();
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

    private List<Instruction> analyseExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(check(TokenType.MINUS)){
            res_ins.addAll(analyseNegateExpr());
        }
        else if(check(TokenType.L_PAREN)){
            res_ins.addAll(analyseGroupExpr());
        }
        else if(check(TokenType.IDENT)){
            Token nameToken = expect(TokenType.IDENT);

            if(check(TokenType.L_PAREN)) {
                res_ins.addAll(analyseCallExpr(nameToken));
            }
            else if(check(TokenType.ASSIGN)) {
                res_ins.addAll(analyseAssignExpr(nameToken));
            }
            else{
                res_ins.addAll(analyseIdentExpr(nameToken));
            }
        }
        else if(check(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL)){
            expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        } else{
            analyseLiteralExpr();
        }

        while(check(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE, TokenType.AS_KW)){
            if(check(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
                TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
                res_ins.addAll(analyseOperatorExpr());
            }
            else if(check(TokenType.AS_KW)){
                res_ins.addAll(analyseAsExpr());
            }
        }
        return res_ins;
    }

    private void analyseLiteralExpr() throws CompileError{
        Token token = expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        if(token.getTokenType() == TokenType.UINT_LITERAL){
            // 直接push进栈
            this.function_body.add(new Instruction(Operation.push, (long)token.getValue()));
        }
        else if(token.getTokenType() == TokenType.STRING_LITERAL){
            // 新建全局变量， 变量名是该字符串，变量值也是该字符串
            int global_index = this.def_table.addGlobal(SymbolType.Global, token.getValueString(),
                TokenType.STRING_LITERAL, true, true, token.getStartPos(), token.getValueString());
            this.function_body.add(new Instruction(Operation.push, (long)global_index));
        }
    }

    private List<Instruction> analyseAsExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.AS_KW);
        Token ty = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        res_ins.addAll(ExprTree.addAllReset());
        while(check(TokenType.AS_KW)){
            res_ins.addAll(analyseAsExpr());
        }
        return res_ins;
    }

    private List<Instruction> analyseOperatorExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        Token token = expect(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE);
        res_ins.addAll(ExprTree.addTokenAndGenerateInstruction(token.getTokenType()));
        res_ins.addAll(analyseExpr());
        return res_ins;
    }

    private List<Instruction> analyseIdentExpr(Token token) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        System.out.println("token:" + token);
        if(this.def_table.getSymbol(token.getValueString()) == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        if(!this.def_table.getSymbol(token.getValueString()).isInitialized()){
            throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
        }
        res_ins.add(getVarOrParamAddress(token));
        res_ins.add(new Instruction(Operation.store64));
        return res_ins;
    }

    private List<Instruction> analyseAssignExpr(Token token) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.ASSIGN);
        res_ins.add(getVarOrParamAddress(token));
        res_ins.addAll(analyseExpr());
        initializeSymbol(token.getValueString(), token.getStartPos());
        res_ins.add(new Instruction(Operation.store64));
        return res_ins;
    }

    private List<Instruction> analyseCallExpr(Token token) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
        if(sym == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        Function func = addstackllocInstruction(token);
        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN)) {
            res_ins.addAll(analyseCallParamList(func.getParams()));
        }
        expect(TokenType.R_PAREN);
        return res_ins;
    }

    public Function addstackllocInstruction(Token token) throws AnalyzeError {
        Function func=null;
        for (String f: this.def_table.function_list.keySet()){
            if(f.equals(token.getValueString())){
                func = this.def_table.function_list.get(f);
                break;
            }
        }
        if(func == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        this.function_body.add(new Instruction(Operation.stackalloc, (long)func.getReturnSlot()));
        return func;
    }

    private List<Instruction> analyseCallParamList(List<SymbolEntry> param_list) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        int param_num = 0;
        analyseExpr();
        res_ins.addAll(ExprTree.addAllReset());
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            // todo: 类型检查
            analyseExpr();
            res_ins.addAll(ExprTree.addAllReset());
            param_num++;
        }
        if(param_num != param_list.size()){
            throw new AnalyzeError(ErrorCode.ParamNumWrong, this.peekedToken.getStartPos());
        }
        return res_ins;
    }

    private List<Instruction> analyseGroupExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.L_PAREN);
        res_ins.addAll(ExprTree.addTokenAndGenerateInstruction(TokenType.L_PAREN));
        analyseExpr();
        // 这里为什么没有allreset
        expect(TokenType.R_PAREN);
        res_ins.addAll(ExprTree.addTokenAndGenerateInstruction(TokenType.R_PAREN));
        return res_ins;
    }

    private List<Instruction> analyseNegateExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.MINUS);
        res_ins.add(new Instruction(Operation.push, 0L));
        analyseExpr();
        res_ins.add(new Instruction((Operation.sub_i)));
        return res_ins;
    }

    private List<Instruction> analyseReturnStmt() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.RETURN_KW);
        if(!check(TokenType.SEMICOLON)){
            if(this.return_type != TokenType.INT_KW){
                System.out.println(this.return_type);
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peekedToken.getStartPos());
            }
            // 返回值off是0
            res_ins.add(new Instruction(Operation.arga, (long)0));
            this.return_type = TokenType.INT_KW;
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

    private List<Instruction> analyseContinueStmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        return null;
    }

    private List<Instruction> analyseBreakStmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        return null;
    }

    private List<Instruction> analyseWhileStmt(int level) throws CompileError{
        BooleanTree booleanTree = new BooleanTree();
        WhileTree whileTree = new WhileTree(booleanTree);
        expect(TokenType.WHILE_KW);
        analyseBooleanExpr(booleanTree);
        booleanTree.setTrueInstructions(analyseBlockStmt(null, level + 1));
        return whileTree.generate();
    }

    private List<Instruction> analyseIfStmt(int level) throws CompileError{
        expect(TokenType.IF_KW);
        BooleanTree booleanTree = new BooleanTree();
        ConditionTree conditionTree=new ConditionTree();
        analyseBooleanExpr(booleanTree);
        booleanTree.setTrueInstructions(analyseBlockStmt(null, level + 1));
        conditionTree.add(booleanTree);
        while(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if(check(TokenType.IF_KW)){
                expect(TokenType.IF_KW);
                analyseBooleanExpr(booleanTree);
                booleanTree.setTrueInstructions(analyseBlockStmt(null, level + 1));
                conditionTree.add(booleanTree);
            }
            else{
                booleanTree=new BooleanTree();
                List<Instruction> instructions=new ArrayList<>();
                booleanTree.setInstructions(instructions);
                booleanTree.setOffset(new Instruction(Operation.br,(long)0));
                booleanTree.setTrueInstructions(analyseBlockStmt(null, level + 1));
                conditionTree.add(booleanTree);
                break;
            }
        }
        return conditionTree.generate();
    }

    private void analyseBooleanExpr(BooleanTree booleanTree) throws CompileError {
        List<Instruction> res_ins = new ArrayList<>();
        analyseExpr();
        Instruction br;
        this.function_body.addAll(ExprTree.addAllReset());
        if(check(TokenType.EQ)){
            expect(TokenType.EQ);
            analyseExpr();
            res_ins.add(new Instruction(Operation.cmp_i));
            br = new Instruction(Operation.br_false,(long)-1);
        }
        else if(check(TokenType.NEQ)){
            expect(TokenType.NEQ);
            analyseExpr();

            res_ins.add(new Instruction(Operation.cmp_i));

            br = new Instruction(Operation.br_true,(long)-1);
        }
        else if(check(TokenType.LT)){
            expect(TokenType.LT);
            analyseExpr();
            //true -1 false 0 1
            res_ins.add(new Instruction(Operation.cmp_i));
            res_ins.add(new Instruction(Operation.set_lt));

            br = new Instruction(Operation.br_true,(long)-1);
        }
        else if(check(TokenType.GT)){
            expect(TokenType.GT);
            analyseExpr();
            //true 1 false 0 -1
            res_ins.add(new Instruction(Operation.cmp_i));
            //true 1 false 0
            this.function_body.add(new Instruction(Operation.set_gt));

            br = new Instruction(Operation.br_true,(long)-1);
        }
        else if(check(TokenType.LE)){
            expect(TokenType.LE);
            analyseExpr();
            //true -1 0 false 1
            this.function_body.add(new Instruction(Operation.cmp_i));
            //true 0 false 1
            this.function_body.add(new Instruction(Operation.set_gt));

            br = new Instruction(Operation.br_false,(long)-1);
        }
        else if(check(TokenType.GE)){
            expect(TokenType.GE);
            analyseExpr();
            //true 1 0 false -1
            this.function_body.add(new Instruction(Operation.cmp_i));
            //true 0 false 1
            this.function_body.add(new Instruction(Operation.set_lt));

            br = new Instruction(Operation.br_false,(long)-1);
        }else{
            br = new Instruction(Operation.br_true,(long)-1);
        }
        booleanTree.setOffset(br);
    }

    private void analyseFunctionParamList() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        analyseFunctionParam();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }
    }

    private void analyseFunctionParam() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        boolean is_const = false;
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            is_const = true;
        }
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        functionAddParam(type.getTokenType(), nameToken.getValueString(), is_const, nameToken.getStartPos());

    }

    private List<Instruction> analyseDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(check(TokenType.LET_KW)){
            res_ins.addAll(analyseLetDeclStmt(level));
        } else if (check(TokenType.CONST_KW)){
            res_ins.addAll(analyseConstDeclStmt(level));
        }
        return res_ins;
    }

    private List<Instruction> analyseConstDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        if(this.def_table.getSymbol(nameToken.getValueString()) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
        }
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);;
        expect(TokenType.ASSIGN);
        if(level == 0){// 全局
            this.def_table.addGlobal(SymbolType.Global, nameToken.getValueString(), nameToken.getTokenType(), false, true, nameToken.getStartPos(), 0);
        }
        else{
            functionAddVar(type.getTokenType(),nameToken.getValueString(), true, nameToken.getStartPos(), level);
            this.def_table.addSymbol(nameToken.getValueString(), SymbolType.Var, nameToken.getTokenType(), false, true, nameToken.getStartPos(), this.param_table.size(), null, level);
        }
        res_ins.add(getVarOrParamAddress(nameToken));
        res_ins.addAll(analyseExpr());
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

    private List<Instruction> analyseLetDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);;
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            if(level == 0){
                // 全局变量
                this.def_table.addGlobal(SymbolType.Global, nameToken.getValueString(), nameToken.getTokenType(), true, false, nameToken.getStartPos(), null);
                res_ins.add(new Instruction(Operation.globa, (long)this.def_table.getSymbol(nameToken.getValueString()).getStackOffset()));
            }
            else{
                // 局部变量
                functionAddVar(type.getTokenType(),nameToken.getValueString(), false, nameToken.getStartPos(), level);
                res_ins.add(new Instruction(Operation.loca, (long)this.def_table.getSymbol(nameToken.getValueString()).getStackOffset()));
            }
            analyseExpr();
            res_ins.addAll(ExprTree.addAllReset());
            res_ins.add(new Instruction((Operation.store64)));
        }
        else{
            this.def_table.addSymbol(nameToken.getValueString(), SymbolType.Var, nameToken.getTokenType(), false, false, nameToken.getStartPos(), this.local_table.size(), null, level);
        }
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

}
