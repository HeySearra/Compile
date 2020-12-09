import java.util.ArrayList;
import java.util.List;

public class Function {
  // 函数体
  List<Instruction> function_body;

  // 函数编号，开始函数编号为0
  int id;
  // 参数
  List<SymbolEntry> param_table;
  // 参数slot
  int param_slot;
  // 局部变量
  List<SymbolEntry> local_table;
  // 局部变量slot
  int local_slot;
  // 函数名
  String name;
  // 返回值类型
  TokenType return_type;
  // 定义位置
  Pos pos;

  public Function(String name, Pos pos, TokenType return_type, List<Instruction> ins, int id) {
    this.name = name;//main 这种
    this.pos = pos;
    this.function_body = ins;
    this.param_table = new ArrayList<>();
    this.local_table = new ArrayList<>();
    this.return_type = return_type;
    this.id = id;
  }

  public int getParamSlot() { return param_slot; }

  public void setParamSlot(int param_slots) { this.param_slot = param_slots; }

  public int getLocalSlot() { return local_slot; }

  public void setLocalSlot(int local_slot) { this.local_slot = local_slot; }

  public void setFunctionBody(List<Instruction> function_body) { this.function_body = function_body; }

  public int getReturnSlot() {
    if(return_type == TokenType.INT_KW){
      return 1;
    }
    return 0;
  }

  public List<SymbolEntry> getParams() { return this.param_table; }

  public void setParams(List<SymbolEntry> params) {
    this.param_table = params;
    for(SymbolEntry t: params){

    }
  }

  public int getId() { return id; }

  public void setId(int id) { this.id = id; }

  public List<Instruction>  getFunctionBody(){ return this.function_body; }

  public List<SymbolEntry> getSymbolTable() {
    List<SymbolEntry> list = new ArrayList<>(this.param_table);
    list.addAll(this.local_table);
    return list;
  }

  public TokenType getReturnType() { return this.return_type; }

  public String getName() { return name; }

  public Pos getPos() { return pos; }

  public int getParamNum() { return this.param_table.size(); }

//  public int getVarNmum() { return this.let_table.size(); }

  public int getReturnNum() {
    if (this.return_type!= TokenType.VOID_KW)
      return 1;
    else return 0;
  }

  public void setPos(Pos pos) {
    this.pos = pos;
  }

  public void setReturnType(TokenType return_type) {
    this.return_type = return_type;
  }

//  public void addSymbolEntry(SymbolEntry symbolEntry) {
//    this.symbol_table.add(symbolEntry);
//    if(symbolEntry.getType() == SymbolType.Param)
//      this.paramSoltNum++;
//    else{
//      int currentvarSoltNUm = this.symbolEntries.size() - this.paramSoltNum;
//      if(currentvarSoltNUm > this.varSoltNmum)
//        this.varSoltNmum = currentvarSoltNUm;
//    }
//  }
//
//  public void outDeep(int deep) {
//    int i = symbolEntries.size()-1;
//    for(;i>=0;i--){
//      if (symbolEntries.get(i).getNametype() == SymbolType.Param)
//        return;
//      if(symbolEntries.get(i).getDeep()==deep)
//        symbolEntries.remove(i);
//      else break;
//    }
//  }
}
