package error;

import util.Pos;

public abstract class CompileError extends Exception {

    private static final long serialVersionUID = 1L;

    public abstract error.ErrorCode getErr();

    public abstract Pos getPos();
}
