
public class SymbolEntry {
    // 全局变量、函数、参数、局部变量
    SymbolType type;
    String name;
    // 变量的tt是变量的类型，函数的tt是函数的返回值类型
    TokenType tt;
    // 是否初始化
    boolean is_initialized;
    // 是否是常量
    boolean is_const;
    int stackOffset;
    Object value;
    int level;

    public SymbolEntry(SymbolType type, String name, TokenType tt, boolean is_initialized, boolean is_const, int stackOffset, Object value, int level) {
        this.type = type;
        this.name = name;
        this.tt = tt;
        this.is_initialized = is_initialized;
        this.is_const = is_const;
        this.stackOffset = stackOffset;
        this.value = value;
        this.level = level;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Object getValue() { return value; }

    public void setValue(Object value) { this.value = value; }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = level; }

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
        return is_initialized;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }

    public SymbolType getType() { return this.type; }

    public Boolean isConstant() {
        return this.is_const;
    }

    public Boolean isFunction() {
        return this.type == SymbolType.Function;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.is_initialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
