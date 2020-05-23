package vm;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

public class gui {
    static int nRegisterRows;
    static JPanel parent, left, registerPanel, registerLeftPanel, flagPanel, registerRowPanels[], registerColPanels[],
            flagRowPanels[];

    static JLabel registerLabels[], registerValueLabels[], flagLabels[], flagValueLabels[];
    static JEditorPane editor;
    static JTable memory;
    static AbstractTableModel tableModel;
    static JButton execute, step, next;
    static JToolBar jt;

    static HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
    static Highlighter highlighter;
    static Object tag = null;
    static String[] mapping = { "A", "B", "C", "D", "E", "H", "L", "S", "P" };

    JTable create_table() {
        tableModel = new AbstractTableModel() {
            public String getColumnName(int col) {
                if (col == 0) {
                    return "Address";
                } else {
                    return "Data";
                }
            }

            public int getColumnCount() {
                return 2;
            }

            public int getRowCount() {
                return 65536;
            }

            public Object getValueAt(int row, int col) {
                if (col == 0) {
                    return row;
                } else {
                    return machine.mem[row];
                }
            }

            public void setValueAt(Object a, int row, int col) {
                machine.mem[row] = new Byte(a.toString());
            }

            public boolean isCellEditable(int row, int col) {
                if(col == 1) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        return new JTable(tableModel);
    }

    JToolBar create_toolbar() {
        jt = new JToolBar();
        execute = new JButton("Execute");
        step = new JButton("Step");
        next = new JButton("Next");
        execute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = editor.getText();
                parser.execute(text);
            }
        });

        step.addActionListener((ActionEvent e) -> {
            step.setEnabled(false);
            next.setEnabled(true);
            parser.step(editor.getText());
        });

        next.addActionListener((ActionEvent e) -> {
            boolean isLeft = parser.next();
            if(!isLeft) {
                next.setEnabled(false);
                step.setEnabled(true);
                if(tag != null) {
                    highlighter.removeHighlight(tag);
                }
            }
        });
        jt.add(execute);
        jt.add(step);
        jt.add(next);
        next.setEnabled(false);
        return jt;
    }

    void setup_registers() {
        JLabel registerTitle = new JLabel("REGISTERS",SwingConstants.CENTER);
        registerPanel = new JPanel(new GridLayout(nRegisterRows + 1, 1, 0, 0));
        registerPanel.add(registerTitle);
        registerPanel.setPreferredSize(new Dimension(150, 400));

        registerRowPanels = new JPanel[nRegisterRows];
        for (int i = 0; i < nRegisterRows; i++) {
            registerRowPanels[i] = new JPanel(new GridLayout(1, 2, 5, 0));
            registerRowPanels[i].setSize(120, 30);
        }

        registerLabels = new JLabel[nRegisterRows];
        String labels[] = { "A", "B C", "D E", "H L", "S P" };
        for (int i = 0; i < nRegisterRows; i++) {
            registerLabels[i] = new JLabel(labels[i]);
            registerLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
            registerLabels[i].setSize(40, 25);
        }

        registerColPanels = new JPanel[nRegisterRows - 1];
        for (int i = 0; i < nRegisterRows - 1; i++) {
            registerColPanels[i] = new JPanel(new GridLayout(1, 2, 15, 0));
        }

        registerValueLabels = new JLabel[2 * nRegisterRows - 1];
        for (int i = 0; i < (2 * nRegisterRows - 1); i++) {
            registerValueLabels[i] = new JLabel("0");
            registerValueLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
            registerValueLabels[i].setSize(40, 25);
            if (i > 0) {
                registerColPanels[(i + 1) / 2 - 1].add(registerValueLabels[i]);
            }
        }

        for (int i = 0; i < nRegisterRows; i++) {
            registerRowPanels[i].add(registerLabels[i]);

            if (i > 0) {
                registerRowPanels[i].add(registerColPanels[i - 1]);
            } else {
                registerRowPanels[i].add(registerValueLabels[0]);
            }

            registerPanel.add(registerRowPanels[i]);

        }

    }

    void setup_flags() {
        JLabel flagTitle = new JLabel("FLAGS");
        flagPanel = new JPanel(new GridLayout(6, 1, 0, 0));
        flagPanel.setSize(80, 400);
        flagPanel.add(flagTitle);

        flagRowPanels = new JPanel[5];
        String[] flags = { "S", "Z", "AC", "P", "CY" };
        flagLabels = new JLabel[5];
        flagValueLabels = new JLabel[5];
        for (int i = 0; i < 5; i++) {
            flagRowPanels[i] = new JPanel(new GridLayout(1, 2, 5, 0));
            flagLabels[i] = new JLabel(flags[i]);
            flagLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
            flagValueLabels[i] = new JLabel("0");
            flagValueLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
            flagRowPanels[i].add(flagLabels[i]);
            flagRowPanels[i].add(flagValueLabels[i]);
            flagPanel.add(flagRowPanels[i]);
        }
    }

    static void highlightLine(int p1, int p2) {
        if (tag != null) {
            highlighter.removeHighlight(tag);
        }
        try {
            tag = highlighter.addHighlight(p1, p2, highlightPainter);
        } catch (BadLocationException e) {
            System.out.println(e);
        }
    }

    gui() {
        JFrame j = new JFrame("Sample");
        j.setSize(1000, 700);
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        nRegisterRows = 5;
        // parent = new JPanel(new BorderLayout());
        parent = new JPanel(new BorderLayout());
        GridBagConstraints c = new GridBagConstraints();
        left = new JPanel(new GridLayout(2, 1, 0, 50));
        // left = new JPanel(new GridBagLayout());
        left.setPreferredSize(new Dimension(300, 700));

        // JLabel registerTitle = new JLabel("REGISTERS",SwingConstants.CENTER);
        // JLabel flagTitle = new JLabel("FLAGS");
        registerLeftPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        registerLeftPanel.setPreferredSize(new Dimension(300, 300));
        // registerLeftPanel.add(registerTitle);
        memory = create_table();
        // memory.createDefaultEditors();
        // memory.setSize(300, 300);
        // memory.setPreferredSize(new Dimension(300, 400));
        JScrollPane memory_pane = new JScrollPane(memory);
        memory_pane.setPreferredSize(new Dimension(300, 400));

        // registerPanel = new JPanel(new GridLayout(nRegisterRows + 1, 1, 0, 0));
        // registerPanel.add(registerTitle);
        // registerPanel.setSize(120, 400);

        // flagPanel = new JPanel(new GridLayout(6, 1, 0, 0));
        // flagPanel.setSize(80, 400);
        // flagPanel.add(flagTitle);

        // flagRowPanels = new JPanel[5];
        // String[] flags = { "S", "Z", "AC", "P", "CY" };
        // flagLabels = new JLabel[5];
        // flagValueLabels = new JLabel[5];
        // for (int i = 0; i < 5; i++) {
        //     flagRowPanels[i] = new JPanel(new GridLayout(1, 2, 5, 0));
        //     flagLabels[i] = new JLabel(flags[i]);
        //     flagLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
        //     flagValueLabels[i] = new JLabel("0");
        //     flagValueLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
        //     flagRowPanels[i].add(flagLabels[i]);
        //     flagRowPanels[i].add(flagValueLabels[i]);
        //     flagPanel.add(flagRowPanels[i]);
        // }

        // registerRowPanels = new JPanel[nRegisterRows];
        // for (int i = 0; i < nRegisterRows; i++) {
        //     registerRowPanels[i] = new JPanel(new GridLayout(1, 2, 5, 0));
        //     registerRowPanels[i].setSize(120, 20);
        //     // registerPanel.add(registerRowPanels[i]);
        // }

        // registerLabels = new JLabel[nRegisterRows];
        // String labels[] = { "A", "B C", "D E", "H L", "S P" };
        // for (int i = 0; i < nRegisterRows; i++) {
        //     registerLabels[i] = new JLabel(labels[i]);
        //     registerLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
        //     registerLabels[i].setSize(40, 15);
        //     // registerRowPanels[i].add(registerLabels[i]);
        // }

        // registerColPanels = new JPanel[nRegisterRows - 1];
        // for (int i = 0; i < nRegisterRows - 1; i++) {
        //     registerColPanels[i] = new JPanel(new GridLayout(1, 2, 15, 0));
        //     // registerColPanels[i] = new JPanel(new FlowLayout());
        //     // registerColPanels[i].setSize(100, 35);
        //     // registerRowPanels[i + 1].add(registerColPanels[i]);
        // }

        // registerValueLabels = new JLabel[2 * nRegisterRows - 1];
        // for (int i = 0; i < (2 * nRegisterRows - 1); i++) {
        //     registerValueLabels[i] = new JLabel("0");
        //     registerValueLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
        //     registerValueLabels[i].setSize(40, 15);
        //     if (i > 0) {
        //         registerColPanels[(i + 1) / 2 - 1].add(registerValueLabels[i]);
        //     }
        // }

        // for (int i = 0; i < nRegisterRows; i++) {
        //     registerRowPanels[i].add(registerLabels[i]);

        //     if (i > 0) {
        //         registerRowPanels[i].add(registerColPanels[i - 1]);
        //     } else {
        //         registerRowPanels[i].add(registerValueLabels[0]);
        //     }

        //     registerPanel.add(registerRowPanels[i]);

        // }

        editor = new JEditorPane();
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editor.setFont(new Font("Monospace", Font.PLAIN, 20));
        JScrollPane editorPane = new JScrollPane(editor);
        editor.setSize(600, 400);
        highlighter = editor.getHighlighter();
        

        setup_registers();
        setup_flags();
        registerLeftPanel.add(registerPanel);
        registerLeftPanel.add(flagPanel);

        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        left.add(registerLeftPanel);
        c.gridx = 0;
        c.weighty = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        // c.weighty = 
        left.add(memory_pane);
        parent.add(editorPane, BorderLayout.CENTER);
        parent.add(left, BorderLayout.WEST);

        var jt = create_toolbar();
        parent.add(jt, BorderLayout.NORTH);

        j.add(parent);
        j.setVisible(true);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new gui();
        });
    }
}