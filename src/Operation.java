
public enum Operation {
//    ILL, LIT, LOD, STO, ADD, SUB, MUL, DIV, WRT,
    nop(0x00), push(0x01), pop(0x02), popn(0x03), dup(0x04),
    loca(0x0a), arga(0x0b), globa(0x0c), load8(0x10), load16(0x11),
    load32(0x12), load64(0x13), store8(0x14), store16(0x15), store32(0x16), store64(0x17),
    alloc(0x18), free(0x19), stackalloc(0x1a), add_i(0x20), sub_i(0x21),
    mul_i(0x22), div_i(0x23), add_f(0x24), sub_f(0x25), mul_f(0x26),
    div_f(0x27), div_u(0x28), shl(0x29), shr(0x2a), and(0x2b),
    or(0x2c), xor(0x2d), not(0x2e), cmp_i(0x30), cmp_u(0x31), cmp_f(0x32),
    neg_i(0x34), neg_f(0x35), itof(0x36), ftoi(0x37), shrl(0x38),
    set_lt(0x39), set_gt(0x3a), br(0x41), br_false(0x42), br_true(0x43),
    call(0x48), ret(0x49), callname(0x4a), scan_i(0x50), scan_c(0x51),
    scan_f(0x52), print_i(0x54), print_c(0x55), print_f(0x56), print_s(0x57),
    println(0x58), panic(0xfe);


    private Integer value;
    Operation(int value) { this.value = value; }
    public int getValue() {
        return this.value;
    }
}
