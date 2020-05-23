package vm;

import java.util.ArrayList;
import java.util.HashMap;

enum Type {
    REG, NUM, ADDR;
}

class Arg {
    Type type;
    String reg;
    int num;

    public String toString() {
        String out = "";
        out = type.toString() + " " + reg + " " + Integer.toString(num);
        return out;
    }
}

interface Instruction {
    public void execute(Arg[] args);
}

class Instructions {
    HashMap<String, Instruction> ins;

    byte get_M() {
        int low = machine.registers.get("L");
        int high = machine.registers.get("H");
        int address = high * 256 + low;
        return machine.mem[address];
    }

    void put_M(byte num) {
        int low = machine.registers.get("L");
        int high = machine.registers.get("H");
        int address = high * 256 + low;
        machine.mem[address] = num;
        return;
    }

    boolean getParity(int n) {
        boolean parity = true;
        while (n != 0) {
            parity = !parity;
            n = n & (n - 1);
        }
        return parity;

    }

    void jump(String addr) {
        parser.i = parser.jump_list.get(addr);
        parser.highlight();
        parser.i -= 1; // Decreasing by 1, since it will be increased by parser.next
    }

    byte handle_flags(String reg) {
        byte flag = 0;
        if (reg == null) {
            reg = "A";
        }
        byte a = machine.registers.get(reg);
        if (a < 0) {
            flag = (byte) (flag | 128);
        }
        if (a == 0) {
            flag |= 64;
        }
        if (getParity(a)) {
            flag |= 4;
        }
        return flag;
    }

    void set_flag(byte flag) {
        machine.registers.put("F", flag);
    }

    byte get_reg(String reg) {
        if (reg.compareTo("M") == 0) {
            return get_M();
        } else {
            return machine.registers.get(reg);
        }
    }

    void put_reg(String reg, byte num) {
        if (reg.compareTo("M") == 0) {
            put_M(num);
        } else {
            machine.registers.put(reg, num);
        }
    }

    short get_pair(String p1) {
        String p2;
        switch (p1) {
        case "B":
            p2 = "C";
            break;
        case "D":
            p2 = "E";
            break;
        case "H":
            p2 = "L";
            break;
        case "SP":
        case "sp":
            p1 = "S";
            p2 = "P";
            break;
        default:
            System.out.println("Couldn't understand register for put_pair: " + p1);
            p2 = "C";
            p1 = "B";
        }

        short low = machine.registers.get(p2);
        short high = machine.registers.get(p1);
        low = (short)((low + 256) % 256);
        high = (short)((high + 256) % 256);
        return (short) (high * 256 + low);
    }

    void put_pair(String p1, short num) {
        String p2;
        switch (p1) {
        case "B":
            p2 = "C";
            break;
        case "D":
            p2 = "E";
            break;
        case "H":
            p2 = "L";
            break;
        case "SP":
        case "sp":
            p1 = "S";
            p2 = "P";
            break;
        default:
            System.out.println("Couldn't understand register for put_pair: " + p1);
            p2 = "C";
            p1 = "B";
        }

        machine.registers.put(p2, (byte) (num & 255));
        machine.registers.put(p1, (byte) (num >>> 8));
    }

    Instructions() {
        ins = new HashMap<String, Instruction>();
        ins.put("mov", (Arg[] args) -> {
            var reg1 = args[0].reg;
            var reg2 = args[1].reg;
            put_reg(reg1, get_reg(reg2));
        });

        ins.put("mvi", (Arg[] args) -> {
            var reg = args[0].reg;
            var num = args[1].num;
            put_reg(reg, (byte) num);
        });

        ins.put("lxi", (Arg[] args) -> {
            var reg = args[0].reg;
            short num = (short) args[1].num;
            put_pair(reg, num);
        });

        ins.put("lda", (Arg[] args) -> {
            var address = args[0].num;
            put_reg("A", machine.mem[address]);
        });

        ins.put("lhld", (Arg[] args) -> {
            var address = args[0].num;
            put_reg("L", machine.mem[address]);
            put_reg("H", machine.mem[address+1]);
        });

        ins.put("shld", (Arg[] args) -> {
            var address = args[0].num;
            machine.mem[address] = get_reg("L");
            machine.mem[address+1] = get_reg("H");
        });

        ins.put("sta", (Arg[] args) -> {
            var address = args[0].num;
            machine.mem[address] = get_reg("A");
        });

        ins.put("ldax", (Arg[] args) -> {
            var reg = args[0].reg;
            assert(reg.equals("B") || reg.equals("D")); // TODO: this check is not working
            short address = get_pair(reg);
            put_reg("A", machine.mem[address]);
        });

        ins.put("stax", (Arg[] args) -> {
            var reg = args[0].reg;
            short address = get_pair(reg);
            machine.mem[address] = get_reg("A");
        });

        ins.put("push", (Arg[] args) -> {
            short address = get_pair("sp");
            var reg = args[0].reg;
            short num = get_pair(reg);
            byte low = (byte) (num & 255);
            byte high = (byte) (num >>> 8);
            machine.mem[address - 1] = high;
            machine.mem[address - 2] = low;
            put_pair("sp", (short) (address - 2));
        });

        ins.put("pop", (Arg[] args) -> {
            short address = get_pair("sp");
            short low = machine.mem[address];
            short num = machine.mem[address + 1];
            num = (short) ((num << 8) | low);
            put_pair("sp", (short) (address + 2));
            put_pair(args[0].reg, num);
        });

        ins.put("add", (Arg[] args) -> {
            args[0].num = get_reg(args[0].reg);
            ins.get("adi").execute(args);
        });

        ins.put("adi", (Arg[] args) -> {
            var num = (args[0].num + 256) % 256;
            var num1 = (get_reg("A") + 256) % 256;
            int ans = num + num1;
            put_reg("A", (byte) ans);
            byte flag = handle_flags("A");
            if (ans > 256) {
                flag |= 1;
            }
            set_flag(flag);
        });

        ins.put("aci", (Arg[] args) -> {
            var num = args[0].num;
            var num1 = (get_reg("A") + 256) % 256;
            byte flag = get_reg("F");
            int carry = (flag & 1) == 1 ? 1 : 0;
            int ans = num + num1 + carry;
            put_reg("A", (byte) ans);
            flag = handle_flags("A");
            if (ans > 256) {
                flag |= 1;
            }
            set_flag(flag);
        });

        ins.put("adc", (Arg[] args) -> {
            args[0].num = get_reg(args[0].reg);
            ins.get("aci").execute(args);
        });

        ins.put("dad", (Arg[] args) -> {
            short num1 = get_pair(args[0].reg);
            short num2 = get_pair("H");
            int ans = num1 + num2;
            put_pair("H", (short) ans);
            byte flag = get_reg("F");
            if (ans > 65536) {
                flag |= 1;
            }
            set_flag(flag);
        });

        ins.put("sub", (Arg[] args) -> {
            args[0].num = get_reg(args[0].reg);
            ins.get("sui").execute(args);
        });

        ins.put("sui", (Arg[] args) -> {
            int num1, num2;
            num1 = args[0].num;
            num2 = get_reg("A");
            num1 = (num1 + 256) % 256;
            num2 = (num2 + 256) % 256;

            // num1 = 256 - num1;
            // System.out.println("SUB a = " + num2 + " " + reg + " = " + num1);
            int res = num2 - num1;
            machine.registers.put("A", (byte) (res));

            byte flag = handle_flags("A");
            if (res < 0) {
                flag |= 1;
            }

            set_flag(flag);
        });

        ins.put("sbi", (Arg[] args) -> {
            args[0].num += get_reg("F") & 1;
            ins.get("sui").execute(args);
        });

        ins.put("sbb", (Arg[] args) -> {
            args[0].num = get_reg(args[0].reg);
            ins.get("sui").execute(args);
        });
        ins.put("cmp", (Arg[] args) -> {
            var old = get_reg("A");
            ins.get("sub").execute(args);
            put_reg("A", old);
        });

        ins.put("cpi", (Arg[] args) -> {
            var old = get_reg("A");
            ins.get("sui").execute(args);
            put_reg("A", old);
        });

        ins.put("inr", (Arg[] args) -> {
            var reg = args[0].reg;
            byte num1 = get_reg(reg);
            machine.registers.put(reg, (byte) (num1 + 1));
            byte flag = handle_flags(reg);
            set_flag(flag);
        });

        ins.put("dcr", (Arg[] args) -> {
            var reg = args[0].reg;
            byte num1 = get_reg(reg);
            machine.registers.put(reg, (byte) (num1 - 1));
            byte flag = handle_flags(reg);
            set_flag(flag);
        });

        ins.put("inx", (Arg[] args) -> {
            var reg = args[0].reg;
            short num = get_pair(reg);
            num += 1;
            put_pair(reg, num);
        });

        ins.put("dcx", (Arg[] args) -> {
            var reg = args[0].reg;
            short num = get_pair(reg);
            num -= 1;
            put_pair(reg, num);
        });

        ins.put("ana", (Arg[] args) -> {
            args[0].num = get_reg(args[0].reg);
            ins.get("ani").execute(args);
        });

        ins.put("ani", (Arg[] args) -> {
            byte num1 = (byte) args[0].num;
            byte num2 = get_reg("A");
            put_reg("A", (byte) (num1 & num2));
            byte flag = handle_flags("A");
            flag &= 254;
            flag |= 16;
            set_flag(flag);
        });

        ins.put("xri", (Arg[] args) -> {
            byte num1 = (byte) args[0].num;
            byte num2 = get_reg("A");
            put_reg("A", (byte) (num1 ^ num2));
            byte flag = handle_flags("A");
            flag &= 236;
            set_flag(flag);
        });

        ins.put("xra", (Arg[] args)-> {
            args[0].num = get_reg(args[0].reg);
            ins.get("xri").execute(args);
        });

        ins.put("ori", (Arg[] args) -> {
            byte num1 = (byte) args[0].num;
            byte num2 = get_reg("A");
            put_reg("A", (byte) (num1 | num2));
            byte flag = handle_flags("A");
            flag &= 236;
            set_flag(flag);
        });

        ins.put("ora", (Arg[] args)-> {
            args[0].num = get_reg(args[0].reg);
            ins.get("xri").execute(args);
        });

        ins.put("jmp", (Arg[] args) -> {
            jump(args[0].reg);
        });

        ins.put("jc", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 1) != 0) {
                jump(args[0].reg);
            }
        });

        ins.put("jnc", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 1) == 0) {
                jump(args[0].reg);
            }
        });

        ins.put("jz", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 64) != 0) {
                jump(args[0].reg);
            }
        });

        ins.put("jnz", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 64) == 0) {
                jump(args[0].reg);
            }
        });

        ins.put("jp", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 128) == 0) {
                jump(args[0].reg);
            }
        });

        ins.put("jm", (Arg[] args) -> {
            byte flag = machine.registers.get("F");
            if ((flag & 128) != 0) {
                jump(args[0].reg);
            }
        });

        ins.put("rrc", (Arg[] args) -> {
            byte a = get_reg("A");
            byte f = get_reg("F");
            byte bit = (byte) (a & 1);
            if (bit == 1) {
                f |= 1;
            } else {
                f &= 254;
            }

            set_flag(f);
            a = (byte) ((a >>> 1) | (bit << 7));
            put_reg("A", a);
        });

        ins.put("rlc", (Arg[] args)-> {
            byte a = get_reg("A");
            byte f = get_reg("F");
            byte bit = ((a & 128) != 0 ) ? (byte)1 : (byte)0;
            a = (byte)((a << 1) | bit);
            if(bit == 1) {
                f |= 1;
            } else {
                f &= 254;
            }
            set_flag(f);
            put_reg("A", a);
        });

        ins.put("ral", (Arg[] args) -> {
            byte old = get_reg("F");
            ins.get("rlc").execute(args);
            byte a = get_reg("A");
            if((old & 1) == 1) {
                a = (byte)(a | 1);
            } else {
                a = (byte)(a & 254);
            }
            put_reg("A", a);
        });

        ins.put("rar", (Arg[] args) -> {
            byte old = get_reg("F");
            ins.get("rrc").execute(args);
            byte a = get_reg("A");
            if((old & 1) == 1) {
                a = (byte)(a | 128);
            } else {
                a = (byte)(a & 127);
            }
            put_reg("A", a);
        });
        
        ins.put("cma", (Arg[] args) -> {
            byte a = get_reg("A");
            a = (byte)~a;
            put_reg("A", a);
        });

        ins.put("cmc", (Arg[] args) -> {
            byte f = get_reg("F");
            f = (byte)(f ^ 1);
            put_reg("F", f);
        });

        ins.put("stc", (Arg[] args) -> {
            byte f = get_reg("F");
            f = (byte)(f | 1);
            put_reg("F", f);
        });

        ins.put("xthl", (Arg[] args)-> {
            byte h = get_reg("H");
            byte l = get_reg("L");
            short address = get_pair("SP");
            byte low = machine.mem[address];
            byte high = machine.mem[address+1];
            machine.mem[address] = l;
            machine.mem[address+1] = h;
            put_reg("H", high);
            put_reg("L", low);
        });

        ins.put("sphl", (Arg[] args) -> {
            short num = get_pair("H");
            put_pair("SP", num);
        });

        ins.put("hlt", (Arg[] args) -> {
            parser.i = parser.ins_list.size() - 1;
        });

        ins.put("xchg", (Arg[] args) -> {
            var h = get_pair("H");
            var d = get_pair("D");
            put_pair("D", h);
            put_pair("H", d);
        });
        ins.put("nop", (Arg[] args) -> {
        });
    }
}