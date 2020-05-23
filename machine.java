package vm;
import java.util.HashMap;
class machine {
    static HashMap<String, Byte> registers = new HashMap<>();
    static byte[] mem = new byte[65536]; 

    static void initialize() {
        // for(int i=0; i<65536; i++) {
        //     mem[i] = 0;
        // }
        registers.put("A", (byte)0);
        registers.put("B", (byte)0);
        registers.put("C", (byte)0);
        registers.put("D", (byte)0);
        registers.put("E", (byte)0);
        registers.put("H", (byte)0);
        registers.put("L", (byte)0);
        registers.put("F", (byte)0);
        registers.put("S", (byte)0);
        registers.put("P", (byte)0);

    }

    static void print_registers() {
        for(var reg: registers.keySet()) {
            System.out.println(reg + " -> " + registers.get(reg));
        }
    }
}