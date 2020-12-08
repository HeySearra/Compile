
public enum SymbolType {
  Const, Param, Var,
  ;

  @Override
  public String toString() {
    switch (this) {
      case Param:
        return "param symbol";
      case Const:
        return "const symbol";
      case Var:
        return "var symbol";
      default:
        return "unknown symbol";
    }
  }
}