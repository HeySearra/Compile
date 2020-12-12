import java.util.*;
public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> global_instructions;
    int global_slot;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    Definition def_table;

    // 表达式栈
    ExprStack expr_stack;


    // 暂存函数的相关内容
    List<Instruction> function_body;
    List<SymbolEntry> param_table;
    int param_slot;
    List<SymbolEntry> local_table;
    int local_slot;
    TokenType return_type;
    private boolean onAssign;
    boolean is_returned;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.global_instructions = new ArrayList<>();
    }

    public void analyse() throws CompileError {
        analyseProgram();
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

    private Token expect(TokenType... tt) throws CompileError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
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

    private Instruction getLocalOrParamAddress(Token token) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
        if(sym == null)
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        if(sym.getType() == SymbolType.Function)
            throw new AnalyzeError(ErrorCode.FunctionHasNoAddr, token.getStartPos());
        else if(sym.getType() == SymbolType.Param){
            return new Instruction(Operation.arga, (long)sym.getId());
        }else if(sym.getType() == SymbolType.Local) {
            return new Instruction(Operation.loca, (long)sym.getId());
        }else{
            return new Instruction(Operation.globa, (long)sym.getId());
        }
    }

    private void functionAddParam(TokenType tt, String name, Pos pos, boolean is_const) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(this.param_slot++, name, SymbolType.Param, tt, true, is_const, pos, null, 1);
        this.param_table.add(se);
    }

    private SymbolEntry functionAddLocal(TokenType tt, String name,Boolean is_init, Boolean is_const, Pos pos, int level) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(this.local_slot++, name, SymbolType.Local, tt, is_init, is_const, pos, null, level);
        this.local_table.add(se);
        return se;
    }

    public Function getStartFunction() throws AnalyzeError {
        return this.def_table.generate(this.global_instructions);
    }

    private void analyseProgram() throws CompileError {
        this.def_table = new Definition();
        this.expr_stack = new ExprStack();
        this.global_instructions = new ArrayList<>();
        while(!check(TokenType.EOF)){
            if(check(TokenType.FN_KW)){
                analyseFunction();
            }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                this.global_instructions.addAll(analyseDeclStmt(0));
            }else{
                throw new ExpectedTokenError(
                    Format.generateList(TokenType.LET_KW, TokenType.CONST_KW, TokenType.FN_KW), peek());
            }
        }
        this.def_table.instruction = this.global_instructions;
        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError{
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        this.function_body = new ArrayList<>();
        this.param_table = new ArrayList<>();
        this.param_slot = 0;
        this.local_table = new ArrayList<>();
        this.local_slot = 0;
        this.return_type = null;
        this.def_table.level = 1;
        this.onAssign = false;
        this.is_returned = false;

        Function func = this.def_table.addFunction(nameToken.getValueString(), null, nameToken.getStartPos());
        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN))
            analyseFunctionParamList();
        func.setParams(this.param_table);
        func.setParamSlot(this.param_slot);
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Token return_tt = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        this.return_type = return_tt.getTokenType();
        func.setReturnType(this.return_type);
        this.function_body = analyseBlockStmt(return_tt.getTokenType(), 1);
        if(!this.is_returned){
            this.function_body.add(new Instruction(Operation.ret));
        }
        func.setFunctionBody(this.function_body);
        func.setLocals(this.local_table);
        func.setLocalSlot(this.local_slot);
    }

    // return_type
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
        // 只能清符号表，不能清函数local表
        this.def_table.levelDown();
        return res_ins;
    }

    private List<Instruction> analyseStmt(TokenType return_type, int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(check(TokenType.LET_KW) || check((TokenType.CONST_KW))){
            res_ins.addAll(analyseDeclStmt(level));
        } else if(check(TokenType.IF_KW)){
            res_ins.addAll(analyseIfStmt(level));
        } else if(check(TokenType.WHILE_KW)){
            res_ins.addAll(analyseWhileStmt(level));
        } else if(check(TokenType.BREAK_KW)){
            res_ins.addAll(analyseBreakStmt());
        } else if(check(TokenType.CONTINUE_KW)){
            res_ins.addAll(analyseContinueStmt());
        } else if(check(TokenType.RETURN_KW)){
            res_ins.addAll(analyseReturnStmt());
        } else if(check(TokenType.L_BRACE)){
            this.def_table.level = level + 1;
            res_ins.addAll(analyseBlockStmt(return_type, level + 1));
        } else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
        } else{
            res_ins.addAll(analyseExprStmt());
            res_ins.addAll(expr_stack.addAllReset());
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
            res_ins.addAll(analyseLiteralExpr());
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

    private List<Instruction> analyseLiteralExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        Token token = expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        if(token.getTokenType() == TokenType.UINT_LITERAL){
            // 直接push进栈
            res_ins.add(new Instruction(Operation.push, (long)token.getValue()));
        }
        else if(token.getTokenType() == TokenType.STRING_LITERAL){
            // 新建全局变量， 变量名是该字符串，变量值也是该字符串
            int global_index = this.def_table.addGlobal(SymbolType.Global, token.getValueString(),
                TokenType.STRING_LITERAL, true, true, token.getStartPos(), token.getValueString());
            res_ins.add(new Instruction(Operation.push, (long)global_index));
        }
        else{
            throw new AnalyzeError(ErrorCode.ExpectedToken, token.getStartPos());
        }
        return res_ins;
    }

    // todo: 类型转换没写完
    private List<Instruction> analyseAsExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.AS_KW);
        Token ty = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        res_ins.addAll(expr_stack.addAllReset());
        while(check(TokenType.AS_KW)){
            res_ins.addAll(analyseAsExpr());
        }
        return res_ins;
    }

    private List<Instruction> analyseOperatorExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        Token token = expect(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE);
        res_ins.addAll(expr_stack.addTokenAndGenerateInstruction(token.getTokenType()));
        res_ins.addAll(analyseExpr());
        return res_ins;
    }

    private List<Instruction> analyseIdentExpr(Token token) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(this.def_table.getSymbol(token.getValueString()) == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        if(!this.def_table.getSymbol(token.getValueString()).isInitialized()){
            throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
        }
        res_ins.add(getLocalOrParamAddress(token));
        res_ins.add(new Instruction(Operation.load64));
        return res_ins;
    }

    private List<Instruction> analyseAssignExpr(Token token) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.ASSIGN);
        res_ins.add(getLocalOrParamAddress(token));
        res_ins.addAll(analyseExpr());
        initializeSymbol(token.getValueString(), token.getStartPos());
        res_ins.add(new Instruction(Operation.store64));
        return res_ins;
    }

    private List<Instruction> analyseCallExpr(Token token) throws CompileError{
        Function func = this.def_table.getFunction(token.getValueString());
        List<Instruction> res_ins = new ArrayList<>();

        // 分配return的slot
        res_ins.add(new Instruction(Operation.stackalloc, (long)func.getReturnSlot()));
        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN)) {
            // 准备参数，分配空间并放入参数
            res_ins.addAll(analyseCallParamList(func.getParams()));
        }
        if(func.isSTDFunction()){
            res_ins.add(new Instruction(Operation.callname, (long)func.getId()));
        }
        else{
            res_ins.add(new Instruction(Operation.call, (long)func.getId()));
        }
        expect(TokenType.R_PAREN);
        return res_ins;
    }

    private List<Instruction> analyseCallParamList(List<SymbolEntry> param_list) throws CompileError{
        int param_num = 1;
        List<Instruction> res_ins = new ArrayList<>(analyseExpr());
        // 将栈中表达式全计算完
        res_ins.addAll(this.expr_stack.addAllReset());
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            // todo: 返回值类型检查
            res_ins.addAll(analyseExpr());
            res_ins.addAll(this.expr_stack.addAllReset());
            param_num++;
        }
        if(param_num != param_list.size()){
            System.out.println("当前参数个数：" + param_num + " ，期望参数个数：" + param_list.size());
            throw new AnalyzeError(ErrorCode.ParamNumWrong, this.peekedToken.getStartPos());
        }
        return res_ins;
    }

    private List<Instruction> analyseGroupExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.L_PAREN);
        res_ins.addAll(this.expr_stack.addTokenAndGenerateInstruction(TokenType.L_PAREN));
        analyseExpr();
        // 这里为什么没有allreset
        expect(TokenType.R_PAREN);
        res_ins.addAll(this.expr_stack.addTokenAndGenerateInstruction(TokenType.R_PAREN));
        return res_ins;
    }

    private List<Instruction> analyseNegateExpr() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.MINUS);
        res_ins.add(new Instruction(Operation.push, 0L));
        res_ins.addAll(analyseExpr());
        res_ins.add(new Instruction((Operation.sub_i)));
        return res_ins;
    }

    private List<Instruction> analyseReturnStmt() throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.RETURN_KW);
        if(!check(TokenType.SEMICOLON)){
            // 有返回值
            if(this.return_type == TokenType.VOID_KW){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peekedToken.getStartPos());
            }
            // todo: 返回值类型检查
            // 返回值off是0
            res_ins.add(new Instruction(Operation.arga, (long)0));
            res_ins.addAll(analyseExpr());
            res_ins.addAll(expr_stack.addAllReset());
            res_ins.add(new Instruction(Operation.store64));
        }
        else if(this.return_type != TokenType.VOID_KW){
            throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peek().getStartPos());
        }
        res_ins.addAll(expr_stack.addAllReset());
        res_ins.add(new Instruction(Operation.ret));
        expect(TokenType.SEMICOLON);
        this.is_returned = true;
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
        expect(TokenType.WHILE_KW);
        List<Instruction> res_ins = new ArrayList<>();
        res_ins.add(new Instruction(Operation.br, (long)0));

        // start，记录开始计算while条件的指令位置
        int start = res_ins.size();
        res_ins.addAll(analyseExpr());
        res_ins.addAll(expr_stack.addAllReset());

        // br_true，如果是真的话跳过br指令，如果是假的话跳到br指令跳出循环
        res_ins.add(new Instruction(Operation.br_true, (long)1));

        //br，跳出循环体，参数待填
        Instruction br = new Instruction(Operation.br);
        res_ins.add(br);

        // 记录while循环体开始处
        int index = res_ins.size();

        res_ins.addAll(analyseBlockStmt(null, level + 1));

        // br_start，跳到while条件判断处，参数待填
        Instruction br_start = new Instruction(Operation.br);
        res_ins.add(br_start);
        br_start.setNum((long)(start - res_ins.size()));

        br.setNum((long)(res_ins.size() - index));
        return res_ins;
//        booleanTree.setTrueInstructions(analyseBlockStmt(null, level + 1));
//        return whileTree.generate();
    }

    private List<Instruction> analyseIfStmt(int level) throws CompileError{
        expect(TokenType.IF_KW);

        List<Instruction> res_ins = new ArrayList<>(analyseExpr());
        res_ins.addAll(expr_stack.addAllReset());

        //brTrue
        res_ins.add(new Instruction(Operation.br_true, (long)1));
        //br
        Instruction br = new Instruction(Operation.br, (long)0);
        res_ins.add(br);
        int index = res_ins.size();

        res_ins.addAll(analyseBlockStmt(null, level + 1));

        int size = res_ins.size();

        if(res_ins.get(size - 1).getOpt() == Operation.ret){
            // 如果if block分析完成后最后一个语句是ret
            int dis = size - index;
            br.setNum((long)dis);
            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.IF_KW)){
                    // else if，递归调用if分析
                    res_ins.addAll(analyseIfStmt(level));
                }
                else{
                    // else 语句
                    res_ins.addAll(analyseBlockStmt(null, level));
                    res_ins.add(new Instruction(Operation.br, (long)0));
                }
            }
        }
        else{
            // if执行完成后要跳转到else block之后
            Instruction jumpInstruction = new Instruction(Operation.br);
            res_ins.add(jumpInstruction);
            int jump = res_ins.size();

            int dis = jump - index;
            br.setNum((long)dis);

            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.IF_KW)){
                    // else if，递归调用if分析
                    res_ins.addAll(analyseIfStmt(level));
                }
                else{
                    // else 语句
                    res_ins.addAll(analyseBlockStmt(null, level));
                    res_ins.add(new Instruction(Operation.br, (long)0));
                }
            }
            dis = res_ins.size() - jump;
            jumpInstruction.setNum((long)dis);
        }
        return res_ins;
    }

    private void analyseFunctionParamList() throws CompileError{
        analyseFunctionParam();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }

    }

    private void analyseFunctionParam() throws CompileError{
        boolean is_const = false;
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            is_const = true;
        }
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        functionAddParam(type.getTokenType(), nameToken.getValueString(), nameToken.getStartPos(), is_const);
    }

    // level == 0是全局
    private List<Instruction> analyseDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        if(check(TokenType.LET_KW)){
            res_ins.addAll(analyseLetDeclStmt(level));
        } else if (check(TokenType.CONST_KW)){
            res_ins.addAll(analyseConstDeclStmt(level));
        }
        return res_ins;
    }

    // level == 0是全局
    private List<Instruction> analyseConstDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        if(this.def_table.getSymbol(nameToken.getValueString()) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
        }
        expect(TokenType.COLON);
        Token type = expect(TokenType.INT_KW, TokenType.DOUBLE_KW);
        expect(TokenType.ASSIGN);
        this.onAssign = true;
        if(level == 0){// 全局
            int global_id = this.def_table.addGlobal(SymbolType.Global, nameToken.getValueString(), nameToken.getTokenType(), false, true, nameToken.getStartPos(), 0);
            res_ins.add(new Instruction(Operation.globa, (long)global_id));
        }
        else{
            SymbolEntry se = functionAddLocal(type.getTokenType(),nameToken.getValueString(), true, true, nameToken.getStartPos(), level);
            res_ins.add(new Instruction(Operation.loca, (long)se.getId()));
        }
        res_ins.addAll(analyseExpr());
        res_ins.addAll(this.expr_stack.addAllReset());
        res_ins.add(new Instruction((Operation.store64)));
        this.onAssign = false;
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

    private List<Instruction> analyseLetDeclStmt(int level) throws CompileError{
        List<Instruction> res_ins = new ArrayList<>();
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.INT_KW, TokenType.DOUBLE_KW);
        // todo: double类型检查
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            this.onAssign = true;
            if(level == 0){
                // 全局变量
                int global_id = this.def_table.addGlobal(SymbolType.Global, nameToken.getValueString(), nameToken.getTokenType(), true, false, nameToken.getStartPos(), 0);
                res_ins.add(new Instruction(Operation.globa, (long)global_id));
            }
            else{
                // 局部变量
                SymbolEntry se = functionAddLocal(type.getTokenType(),nameToken.getValueString(), true, false, nameToken.getStartPos(), level);
                res_ins.add(new Instruction(Operation.loca, (long)se.getId()));
            }
            res_ins.addAll(analyseExpr());
            res_ins.addAll(this.expr_stack.addAllReset());
            res_ins.add(new Instruction((Operation.store64)));
            this.onAssign = false;
        }
        else{
            if(level == 0){
                // 全局变量
                this.def_table.addGlobal(SymbolType.Global, nameToken.getValueString(), nameToken.getTokenType(), true, false, nameToken.getStartPos(), 0);
            }
            else{
                // 局部变量
                functionAddLocal(type.getTokenType(),nameToken.getValueString(), false, false, nameToken.getStartPos(), level);
            }
        }
        expect(TokenType.SEMICOLON);
        return res_ins;
    }

}
