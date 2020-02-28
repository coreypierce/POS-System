package edu.wit.se16.system.logging.console;

import java.awt.Color;
import java.awt.Toolkit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class ConsoleHelpDialog extends JDialog {
	private static final long serialVersionUID = 3330809697766292087L;

	public ConsoleHelpDialog() {
		setTitle("Help");
		setIconImage(Toolkit.getDefaultToolkit().getImage(ConsoleHelpDialog.class.getResource("/javax/swing/plaf/metal/icons/ocean/question.png")));
		
		setSize(450, 300);
		setLocationRelativeTo(null);

        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);

		// ================== Console Commands ================== \\
		
		JPanel consolePanel = new JPanel();
		consolePanel.setBorder(new TitledBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 5, 5, 5)), "Console Commands", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
		contentPanel.add(consolePanel);

		consolePanel.add(Box.createVerticalStrut(5));
		
		consolePanel.add(new JLabel("<html>\r\n\t<b>clear</b><br/>\r\n\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\r\n\tClears the console\r\n</html>"));
		consolePanel.add(Box.createVerticalStrut(10));
		
		consolePanel.add(new JLabel("<html>\r\n\t<b>textwrap=[true/false]</b><br/>\r\n\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\r\n\tEnables/Disables text-wrapping in the console\r\n</html>"));
		consolePanel.add(Box.createVerticalStrut(10));
		
		consolePanel.add(new JLabel("<html>\r\n\t<b>stacktrace=[true/false]</b><br/>\r\n\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\r\n\tSet whether stack-traces should be shown\r\n</html>"));
		consolePanel.add(Box.createVerticalGlue());

		// ================== Server Commands ================== \\
		
		JPanel serverPanel = new JPanel();
		serverPanel.setBorder(new TitledBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(0, 5, 5, 5)), "Server Commands", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.Y_AXIS));
		contentPanel.add(serverPanel);

		serverPanel.add(Box.createVerticalStrut(5));
		
		serverPanel.add(new JLabel("<html>\r\n\t<b>exit/shutdown/stop</b><br/>\r\n\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\r\n\tShutdown web-server and terminates the program\r\n</html>"));
		serverPanel.add(Box.createVerticalStrut(10));
		
		serverPanel.add(new JLabel("<html>\r\n\t<b>sql [statment]</b><br/>\r\n\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\r\n\tExecutes the provided SQL statement, statement must be inside [ ... ]\r\n</html>"));
		serverPanel.add(Box.createVerticalGlue());
	}

}
