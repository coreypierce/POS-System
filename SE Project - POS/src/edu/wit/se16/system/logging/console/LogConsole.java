package edu.wit.se16.system.logging.console;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;

import edu.wit.se16.system.LocalVars;
import edu.wit.se16.system.logging.LoggingUtil;

public class LogConsole extends JFrame implements ActionListener, ItemListener, ComponentListener, KeyListener {
	private static final long serialVersionUID = -4657334060258001804L;
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private JPanel contentPane;

	private JToggleButton regexToggleButton;
	private JToggleButton caseToggleButton;
	
	private JTextField filterTextField;
	private JTextField commandTextField;
	
	private JCheckBox infoLevelCheckbox;
	private JCheckBox warnLevelCheckbox;
	private JCheckBox errorLevelCheckbox;
	private JCheckBox debugLevelCheckbox;
	private JCheckBox traceLevelCheckbox;
	
	private JButton helpButton;
	
	private JTextPane textPane;
	
	private ArrayList<String> consoleHistory;
	private String tempCommand;
	private int historyIndex;
	
	private LogConsoleStreamProcessor streamProcessor;

	public LogConsole() {
		streamProcessor = new LogConsoleStreamProcessor();
		
		consoleHistory = new ArrayList<>();
		historyIndex = 0;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignore) {
		}
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(600, 560);
		
		setIconImage(new ImageIcon(LogConsole.class.getClassLoader().getResource("root/images/icon96.png")).getImage());
		setTitle("POS - Console - " + LocalVars.LOCAL_ADDRESS.getHostName() + ":" + LocalVars.HTTP_PORT + "/" + LocalVars.HTTPS_PORT);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 5));
		JPanel topPanel = new JPanel();
		contentPane.add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		JPanel levelPanel = new JPanel();
		topPanel.add(levelPanel);
		levelPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Log Level", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.X_AXIS));
		
		infoLevelCheckbox = new JCheckBox("INFO", ConsoleVars.ENABLE_INFO_LOG);
		levelPanel.add(infoLevelCheckbox);
		
		warnLevelCheckbox = new JCheckBox("WARN", ConsoleVars.ENABLE_WARN_LOG);
		levelPanel.add(warnLevelCheckbox);
		
		errorLevelCheckbox = new JCheckBox("ERROR", ConsoleVars.ENABLE_ERROR_LOG);
		levelPanel.add(errorLevelCheckbox);
		
		debugLevelCheckbox = new JCheckBox("DEBUG", ConsoleVars.ENABLE_DEBUG_LOG);
		levelPanel.add(debugLevelCheckbox);
		
		traceLevelCheckbox = new JCheckBox("TRACE", ConsoleVars.ENABLE_TRACE_LOG);
		levelPanel.add(traceLevelCheckbox);
		
		Component verticalGlue = Box.createVerticalGlue();
		levelPanel.add(verticalGlue);
		
		JPanel filterPanel = new JPanel();
		filterPanel.setBorder(new TitledBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(1, 3, 3, 3)), "Filter", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		topPanel.add(filterPanel);
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
		
		filterTextField = new JTextField();
		filterPanel.add(filterTextField);
		filterTextField.setColumns(10);
		
		filterPanel.add(Box.createHorizontalStrut(5));
		
		regexToggleButton = new JToggleButton(".*");
		regexToggleButton.setToolTipText("Regular Expression");
		regexToggleButton.setFocusable(false);
		regexToggleButton.setMargin(new Insets(3, 6, 3, 6));
		filterPanel.add(regexToggleButton);
		
		caseToggleButton = new JToggleButton("Aa");
		caseToggleButton.setToolTipText("Case Sensitive");
		caseToggleButton.setFocusable(false);
		caseToggleButton.setMargin(new Insets(3, 6, 3, 6));
		filterPanel.add(caseToggleButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		textPane = new JTextPane(streamProcessor.getDocumnet());;
		textPane.setEditable(false);
		
		JPanel noWrapPanel = new JPanel(new BorderLayout());
        noWrapPanel.add(textPane);
		scrollPane.setViewportView(noWrapPanel);
		
		JPanel commandPanel = new JPanel();
		contentPane.add(commandPanel, BorderLayout.SOUTH);
		commandPanel.setLayout(new BorderLayout(5, 0));
		
		commandTextField = new JTextField();
		commandPanel.add(commandTextField);
		commandTextField.setColumns(10);
		
		helpButton = new JButton("Help");
		commandPanel.add(helpButton, BorderLayout.EAST);
		
		filterTextField.addActionListener(this);
		commandTextField.addActionListener(this);
		commandTextField.addKeyListener(this);
		
		helpButton.addActionListener(this);
		
		caseToggleButton.addItemListener(this);
		regexToggleButton.addItemListener(this);
		
		infoLevelCheckbox.addItemListener(this);
		warnLevelCheckbox.addItemListener(this);
		errorLevelCheckbox.addItemListener(this);
		debugLevelCheckbox.addItemListener(this);
		traceLevelCheckbox.addItemListener(this);

		scrollPane.addComponentListener(this);
		
		// set filters on stream-handler
		itemStateChanged(null);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				LogConsoleStreamProcessor.sendInput("exit");
			}
		});
		
		setLocationRelativeTo(null);
		setVisible(true);

		commandTextField.requestFocus();
		commandTextField.grabFocus();
	}
	
	private boolean handleCommand(String command) {
		String cmd = command;
		if(command.contains("=")) {
			cmd = command.substring(0, cmd.indexOf("="));
		}
		
		switch(cmd.toLowerCase()) {
		// =========== Clear Commands =========== \\
		
			case "clear": case "cls":
				streamProcessor.clear();
				return true;
				
		// =========== Line Wrap Commands =========== \\
			
			case "nowrap": case "no-wrap":
				streamProcessor.setLineWrap(false);
				return true;
				
			case "wrap": 
				if(!command.contains("=")) {
					streamProcessor.setLineWrap(true);
					return true;
				}
			case "linewrap": case "textwrap": case "line_wrap": case "text_wrap": {
				if(!command.contains("=")) {
					LOG.error("invalid format: {}=[true/false]", cmd);
					return true;
				}
				
				String state = command.substring(command.indexOf("=") + 1);
				state = state.toLowerCase();
				
				if(!(state.equals("true") || state.equals("false"))) {
					LOG.error("invalid format: {}=[true/false]", cmd);
					return true;
				}
				
				streamProcessor.setLineWrap(Boolean.parseBoolean(state));
				return true;
			}
			
		// =========== Stack-trace Commands =========== \\

			case "nostack": case "nostacktrace": 
			case "no-stack": case "no-stacktrace": 
			case "hidestack": case "hidestacktrace":
			case "hide_stack": case "hide_stacktrace":
				streamProcessor.setShowStack(false);
				return true;
				
			case "stack": case "stacktrace": 
			case "showstack": case "showstacktrace":
			case "show_stack": case "show_stacktrace": {
				if(!command.contains("=")) {
					streamProcessor.setShowStack(true);
					return true;
				}
				
				String state = command.substring(command.indexOf("=") + 1);
				state = state.toLowerCase();
				
				switch(state) {
					case "true": case "show": case "shown":
						streamProcessor.setShowStack(true);
						return true;

					case "false": case "hide": case "hidden":
						streamProcessor.setShowStack(false);
						return true;
					
					default:
						LOG.error("invalid format: {}=[true/false]", cmd);
						return true;
				}
			}
		}
		
		return false;
	}

	// ===================================== Event Handlers ===================================== \\
	
	public void itemStateChanged(ItemEvent e) {
		String filter = filterTextField.getText();
		
		if(!filter.isEmpty() && !regexToggleButton.isSelected()) {
			filter = Pattern.quote(filter);
		}
		
		filter += caseToggleButton.isSelected() ? "/i" :  "/.";
		
		streamProcessor.updateFilters(
				infoLevelCheckbox.isSelected(),
				warnLevelCheckbox.isSelected(),
				errorLevelCheckbox.isSelected(),
				debugLevelCheckbox.isSelected(),
				traceLevelCheckbox.isSelected(),
				filter
			);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == commandTextField) {
			String command = commandTextField.getText();
			
			if(!command.isEmpty()) {
				streamProcessor.writeInputMessage(command);
				commandTextField.setText("");
				
				// check if command is a console-command
				if(!handleCommand(command)) {
					// if not, pass it to the input-stream
					LogConsoleStreamProcessor.sendInput(command);
				}
				
				// reset command history
				historyIndex = 0;
				tempCommand = "";
				
				// add previous command to history
				if(!command.isEmpty()) 
					consoleHistory.add(command);
			}
			
			return;
		}
		
		if(e.getSource() == filterTextField) {
			this.itemStateChanged(null);
			return;
		}
		
		if(e.getSource() == helpButton) {
			JDialog helpPopup = new ConsoleHelpDialog();
			helpPopup.setVisible(true);
			return;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		if(e.getSource() == commandTextField) {
			if(e.getKeyCode() == KeyEvent.VK_UP) {
				// check if we aren't at the end (start-of-list)
				if(historyIndex < consoleHistory.size()) {
					historyIndex ++;
					
					// get history entry
					commandTextField.setText(consoleHistory.get(consoleHistory.size() - historyIndex));
				}

			} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
				// check if we aren't at the beginning (end-of-list)
				if(historyIndex > 0) {
					historyIndex --;

					// if at bottom of list
					if(historyIndex == 0) {
						commandTextField.setText(tempCommand);
					} else {
						// get history entry
						commandTextField.setText(consoleHistory.get(consoleHistory.size() - historyIndex));						
					}
				} else {
					commandTextField.setText(tempCommand);
				}
			
			// if escape-key is pressed, clear text-field
			} else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				commandTextField.setText("");
			}
			
			return;
		}
	}
	
	public void keyTyped(KeyEvent e) { 
		if(e.getSource() == commandTextField) {
			// skip non-letter keys
			if(e.getKeyChar() < ' ' || '~' < e.getKeyChar()) return;
			
			// record current text
			tempCommand = commandTextField.getText() + e.getKeyChar();
			return;
		}
	}

	public void componentResized(ComponentEvent e) {
		JScrollPane scrollPane = (JScrollPane) e.getComponent();
		streamProcessor.documnetContainerResized(textPane, scrollPane.getViewport().getWidth());
	}

	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }
	public void componentHidden(ComponentEvent e) { }

	public void keyReleased(KeyEvent e) { }
}
