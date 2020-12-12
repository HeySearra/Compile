
public enum SymbolType {
  Global, Param, Local, Function
  ;

  @Override
  public String toString() {
    switch (this) {
      case Param:
        return "param symbol";
      case Local:
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