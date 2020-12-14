public class BreakAndContinue {
    Instruction instruction;
    int location;
    int while_level;
    public BreakAndContinue(Instruction instruction, int location, int while_level){
        this.instruction = instruction;
        this.location = location;
        this.while_level = while_level;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public int getLocation() {
        return location;
    }

    public int getWhileLevel() {
        return while_level;
    }
}
