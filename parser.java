package vm;

import java.util.Scanner;

import java.util.HashMap;

import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class parser {
    static HashMap<String, Type[]> format = new HashMap<>();
    static Instructions instructions = new Instructions();
    static ArrayList<loc> ins_list = new ArrayList<>();
    static HashMap<String, Integer> jump_list = new HashMap<>();
    static int i = 0;
    static boolean shouldHighlight = false;

    static class loc {
        int p1, p2;
        String ins;
        Arg[] args;

        loc(int p1, int p2, String ins, Arg[] args) {
            this.p1 = p1;
            this.p2 = p2;
            this.ins = ins;
            this.args = args;
        }

        public String toString() {
            String out;
            out = ins + ": ";
            for (int i = 0; i < args.length; i++) {
                out = out + args[i].toString() + " ";
            }
            return out;
        }

    }

    static void update_registers() {
        for (int i = 0; i < gui.mapping.length; i++) {
            int num = machine.registers.get(gui.mapping[i]);
            num = (num + 256) % 256;
            gui.registerValueLabels[i].setText(Integer.toString(num));
        }
        byte flag = machine.registers.get("F");
        if ((flag & 128) != 0)
            gui.flagValueLabels[0].setText("1");
        else
            gui.flagValueLabels[0].setText("0");

        if ((flag & 64) != 0)
            gui.flagValueLabels[1].setText("1");
        else
            gui.flagValueLabels[1].setText("0");

        if ((flag & 16) != 0)
            gui.flagValueLabels[2].setText("1");
        else
            gui.flagValueLabels[2].setText("0");

        if ((flag & 4) != 0)
            gui.flagValueLabels[3].setText("1");
        else
            gui.flagValueLabels[3].setText("0");

        if ((flag & 1) != 0)
            gui.flagValueLabels[4].setText("1");
        else
            gui.flagValueLabels[4].setText("0");

        gui.tableModel.fireTableDataChanged();
    }

    static public void parse(String s, int low, int high) {
        var out = new ArrayList<String>();
        int inc = 1;
        // System.out.println(s);
        var a = s.split(" |,|\t");
        for (var b : a) {
            if (b.length() != 0) {
                out.add(b);
            }
        }
        if (out.size() == 0) {
            return;
        }
        // System.out.println("Out = " + out);
        String opcode = out.get(0);
        if (opcode.charAt(opcode.length() - 1) == ':') {
            jump_list.put(opcode.substring(0, opcode.length() - 1), ins_list.size());
            opcode = out.get(1);
            inc = 2;
        }
        opcode = opcode.toLowerCase();
        var argument_type = format.get(opcode);
        if (argument_type == null) {
            System.out.println("Instruction not found");
        } else {
            int arg_size = argument_type.length;
            // System.out.println("argument size = " + arg_size);
            if (out.size() != arg_size + inc) {
                System.out.println("Error");
            }

            Arg[] args = new Arg[arg_size];
            for (int i = 0; i < arg_size; i++) {
                args[i] = new Arg();
                args[i].type = argument_type[i];
                if (args[i].type == Type.REG) {
                    args[i].reg = out.get(i + inc).toUpperCase();
                } else if (args[i].type == Type.NUM) {
                    args[i].num = Integer.parseInt(out.get(i + inc));
                } else {
                    args[i].reg = out.get(i + inc);
                }
            }

            ins_list.add(new loc(low, high, opcode, args));
        }

    }

    static Type[] func(Type... a) {
        return a;
    }

    static void init() {
        machine.initialize();
        format.put("mov", func(Type.REG, Type.REG));
        format.put("mvi", func(Type.REG, Type.NUM));
        format.put("lxi", func(Type.REG, Type.NUM));
        format.put("add", func(Type.REG));
        format.put("sub", func(Type.REG));
        format.put("sui", func(Type.NUM));
        format.put("sbi", func(Type.NUM));
        format.put("sbb", func(Type.REG));
        format.put("cmp", func(Type.REG));
        format.put("cpi", func(Type.REG));
        format.put("dcr", func(Type.REG));
        format.put("inr", func(Type.REG));
        format.put("dcx", func(Type.REG));
        format.put("inx", func(Type.REG));
        format.put("jmp", func(Type.ADDR));
        format.put("jz", func(Type.ADDR));
        format.put("jnz", func(Type.ADDR));
        format.put("jc", func(Type.ADDR));
        format.put("jnc", func(Type.ADDR));
        format.put("jpo", func(Type.ADDR));
        format.put("jpe", func(Type.ADDR));
        format.put("jp", func(Type.ADDR));
        format.put("jm", func(Type.ADDR));
        format.put("adi", func(Type.NUM));
        format.put("aci", func(Type.NUM));
        format.put("adc", func(Type.REG));
        format.put("dad", func(Type.REG));
        format.put("ana", func(Type.REG));
        format.put("ani", func(Type.NUM));
        format.put("xra", func(Type.REG));
        format.put("xri", func(Type.NUM));
        format.put("ora", func(Type.REG));
        format.put("ori", func(Type.NUM));
        format.put("lda", func(Type.NUM));
        format.put("sta", func(Type.NUM));
        format.put("lhld", func(Type.NUM));
        format.put("shld", func(Type.NUM));
        format.put("ldax", func(Type.REG));
        format.put("stax", func(Type.REG));
        format.put("push", func(Type.REG));
        format.put("pop", func(Type.REG));
        format.put("rrc", func());
        format.put("rlc", func());
        format.put("rar", func());
        format.put("ral", func());
        format.put("cma", func());
        format.put("cmc", func());
        format.put("stc", func());
        format.put("xthl", func());
        format.put("sphl", func());
        format.put("hlt", func());
        format.put("xchg", func());
        format.put("nop", func());

    }

    static void highlight() {
        if (shouldHighlight) {
            var ins = ins_list.get(i);
            gui.highlightLine(ins.p1, ins.p2);
        }
    }

    static boolean next() {
        var ins = ins_list.get(i);
        instructions.ins.get(ins.ins).execute(ins.args);
        update_registers();
        i = i + 1;
        if (i == ins_list.size()) {
            i = 0;
            shouldHighlight = false;
            return false;
        }
        highlight();
        return true;
    }

    static void step(String s) {
        ins_list.clear();
        jump_list.clear();
        process(s);
        // System.out.println(ins_list);
        // System.out.println(jump_list);
        update_registers();
        shouldHighlight = true;
        i = 0;
        if (ins_list.size() > 0) {
            highlight();
        }
    }

    static void execute(String s) {
        ins_list.clear();
        jump_list.clear();
        process(s);
        i = 0;
        while (next())
            ;
    }

    static void process(String s) {
        machine.initialize();
        ArrayList<String> codes = new ArrayList<>();
        ArrayList<Integer> low = new ArrayList<>(), high = new ArrayList<>();
        int l = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                if (l < i) {
                    codes.add(s.substring(l, i));
                    low.add(l);
                    high.add(i);
                }
                l = i + 1;
            }
        }
        if (l < s.length()) {
            codes.add(s.substring(l, s.length()));
            low.add(l);
            high.add(s.length());
        }

        for (int i = 0; i < codes.size(); i++) {
            parse(codes.get(i), low.get(i), high.get(i));
        }
    }

    public static void main(String[] args) {
        init();
        Scanner s = new Scanner(System.in);
        SwingUtilities.invokeLater(() -> {
            new gui();
        });
        // String a = s.nextLine();
        /*
         * try { // System.out.println("GIven -> " + a); while (true) { String a =
         * s.nextLine(); parse(a); } } catch (java.util.NoSuchElementException e) { //
         * Do nothing } // var k = parse(a); machine.print_registers();
         */
    }
}
