public enum ErrorCode {
    NoError, // Should be only used internally.
    StreamError, EOF, InvalidInput, InvalidIdentifier, IntegerOverflow, // int32_t overflow.
    NoBegin, NoEnd, NeedIdentifier, ConstantNeedValue, NoSemicolon,
    InvalidVariableDeclaration, IncompleteExpression, NotDeclared,
    AssignToConstant, DuplicateDeclaration, NotInitialized, InvalidAssignment,
    InvalidPrint, ExpectedToken, InvalidDouble, InvalidInt, InvalidChar,
    NoSuchGlobal, ReturnTypeWrong, ParamNumWrong, FunctionHasNoAddr, AssignToFunction,
    CantFindMain, NotSTDFunction, AssignFaild, InvalidString, AssignTypeWrong, AsTypeWrong,
    ExprTypeWrong, OutWhile,
}
