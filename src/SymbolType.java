
public enum SymbolType {
  Global, Param, Var, Function
  ;

  @Override
  public String toString() {
    switch (this) {
      case Param:
        return "param symbol";
      case Var:
        return "var symbol";
      case Global:
        return "global symbol";
      case Function:
        return "function symbol";
      default:
        return "unknown symbol";
    }
  }
}