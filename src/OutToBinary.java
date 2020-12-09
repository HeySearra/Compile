import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class OutToBinary {
  private Function start;
  Definition def_table;
  private List<Byte> output;

  //DataOutputStream out = new DataOutputStream(new FileOutputStream(new File("src/out.txt")));

  int magic=0x72303b3e;
  int version=0x00000001;

  public OutToBinary(Definition def_table, Function start){
    this.start = start;
    this.def_table = def_table;
    output = new ArrayList<>();
  }

  public List<Byte> generate() throws IOException {
    //magic
    List<Byte> magic=int2bytes(4,this.magic);
    output.addAll(magic);
    //version
    List<Byte> version=int2bytes(4,this.version);
    output.addAll(version);

    //globals.count
    output.addAll(int2bytes(4, def_table.getGlobalListCount()));

    //out.writeBytes(globalCount.toString());

    for(SymbolEntry g : def_table.getGlobalList()){
      //isConst
      List<Byte> isConst=int2bytes(1, g.isConstant().compareTo(false));
      output.addAll(isConst);

      List<Byte> global_value_count;
      List<Byte> global_value;

      if (g.getValue() == null) {
        global_value_count = int2bytes(4, 8);
        global_value = long2bytes(8,0);
      }
      else {
        global_value = String2bytes(g.getValue().toString());
        global_value_count = int2bytes(4, global_value.size());
      }

      output.addAll(global_value_count);
      output.addAll(global_value);
    }

    //functions.count
    List<Byte> functionsCount=int2bytes(4, def_table.getFunctionListCount() + 1);
    output.addAll(functionsCount);

    generateFunction(start);

    for (String name: def_table.getFunctionList().keySet()) {
      generateFunction(def_table.getFunctionList().get(name));
    }

    return output;
  }

  private void generateFunction(Function function) throws IOException {
    //name
    List<Byte> name = int2bytes(4,function.getId());
    output.addAll(name);
    //out.writeBytes(name.toString());

    //retSlot
    List<Byte> retSlots = int2bytes(4,function.getReturnSlot());
    output.addAll(retSlots);
    //out.writeBytes(retSlots.toString());

    //paramsSlots
    List<Byte> paramsSlots=int2bytes(4,function.getParamSlot());
    output.addAll(paramsSlots);
    //out.writeBytes(paramsSlots.toString());

    //locSlots
    List<Byte> locSlots=int2bytes(4,function.getLocalSlot());
    output.addAll(locSlots);
    //out.writeBytes(locSlots.toString());

    List<Instruction> ins = function.getFunctionBody();

    //bodyCount
    List<Byte> bodyCount=int2bytes(4, ins.size());
    output.addAll(bodyCount);
    //out.writeBytes(bodyCount.toString());

    //body
    for(Instruction i : ins){
      //type
      List<Byte> type = int2bytes(1, i.getCode());
      output.addAll(type);
      //out.writeBytes(type.toString());

      if(i.getNum() != null){
        List<Byte>  x;
        if(i.getCode() == 1)
          // push是8字节，其他是4字节
          x = long2bytes(8, i.getNum());
        else
          x = long2bytes(4, i.getNum());
        output.addAll(x);
        //out.writeBytes(x.toString());
      }
    }
  }

  private List<Byte> Char2bytes(char value) {
    List<Byte>  AB=new ArrayList<>();
    AB.add((byte)(value&0xff));
    return AB;
  }

  private List<Byte> String2bytes(String valueString) {
    List<Byte>  AB=new ArrayList<>();
    for (int i=0;i<valueString.length();i++){
      char ch=valueString.charAt(i);
      AB.add((byte)(ch&0xff));
    }
    return AB;
  }

  private List<Byte> long2bytes(int length, long value) {
    ArrayList<Byte> bytes = new ArrayList<>();
    int start = 8 * (length-1);
    for(int i = 0 ; i < length; i++){
      bytes.add((byte) (( value >> ( start - i * 8 )) & 0xFF ));
    }
    return bytes;
  }

  /*
   * length 长度
   * target 值
   */
  private ArrayList<Byte> int2bytes(int length, int target){
    ArrayList<Byte> bytes = new ArrayList<>();
    int start = 8 * (length-1);
    for(int i = 0 ; i < length; i++){
      bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
    }
    return bytes;
  }
}
