import java.util.Objects;

public class Instruction {
    private Operation opt;
    Integer num;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.num = 0;
    }

    public Instruction(Operation opt, Integer num) {
        this.opt = opt;
        this.num = num;
    }

    public Instruction() {
        this.opt = Operation.nop;
        this.num = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(num, that.num);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, num);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    @Override
    public String toString() {
        switch (this.opt) {
            case nop:
            case pop:
            case dup:
            case load8:
            case load16:
            case load32:
            case load64:
            case store8:
            case store32:
            case store64:
            case alloc:
            case free:
            case add_i:
            case sub_i:
            case mul_i:
            case div_i:
            case add_f:
            case mul_f:
            case div_f:
            case div_u:
            case shl:
            case shr:
            case and:
            case or:
            case xor:
            case not:
            case cmp_i:
            case cmp_u:
            case cmp_f:
            case neg_i:
            case neg_f:
            case itof:
            case ftoi:
            case shrl:
            case set_lt:
            case set_gt:
            case ret:
            case scan_i:
            case scan_c:
            case scan_f:
            case print_i:
            case print_c:
            case print_f:
            case print_s:
            case println:
            case panic:
                return String.format("%s", this.opt);
            case push:
            case popn:
            case loca:
            case arga:
            case globa:
            case stackalloc:
            case br:
            case br_false:
            case br_true:
            case call:
            case callname:
                return String.format("%s %s", this.opt, this.num);
            default:
                return "nop";
        }
    }
}
