import java.util.ArrayList;
import java.util.List;

public class Function {
  // 函数体
  List<Instruction> function_body;

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

  SymbolEntry se;

  public Function(SymbolEntry se, String name, Pos pos, TokenType return_type) {
    this.name = name;//main 这种
    this.pos = pos;
//    this.function_body = new ArrayList<>();
//    this.param_table = new ArrayList<>();
//    this.local_table = new ArrayList<>();
    this.return_type = return_type;
    this.se = se;
  }

  public int getParamSlot() { return param_slot; }

  public void setParamSlot(int param_slots) { this.param_slot = param_slots; }

  public int getLocalSlot() { return local_slot; }

  public void setLocalSlot(int local_slot) { this.local_slot = local_slot; }

  public void setFunctionBody(List<Instruction> function_body) { this.function_body = function_body; }

  public int getReturnSlot() {
    if(this.return_type == TokenType.INT_KW){
      return 1;
    }
    return 0;
  }

  public List<SymbolEntry> getParams() { return this.param_table; }

  public void setParams(List<SymbolEntry> params) {
    this.param_table = params;
  }

  public void setLocals(List<SymbolEntry> locals) {
    this.local_table = locals;
  }

  public int getId() { return se.getId(); }

  public void setId(int id) { this.se.setId(id); }

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

  public Boolean isSTDFunction(){
    String[] std_function = {"getint", "getdouble", "getchar", "putint", "putdouble", "putchar",
        "putstr", "putln"};
    for(String n: std_function) {
      if (this.se.getName().equals(n)){
        return true;
      }
    }
    return false;
  }
}
