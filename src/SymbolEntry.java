
public class SymbolEntry {
    SymbolType type;
    String name;
    boolean isInitialized;
    int stackOffset;

    public SymbolEntry(SymbolType type, String name, boolean isDeclared, int stackOffset) {
        this.type = type;
        this.name = name;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
    }

    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }

    public SymbolType getType() { return this.type; }

    public boolean isConstant() { return this.type == SymbolType.Const;}

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
