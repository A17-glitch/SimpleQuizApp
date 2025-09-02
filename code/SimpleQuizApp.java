
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class SimpleQuizApp {

  private JFrame frame;
  private CardLayout cardLayout;
  private JPanel mainPanel;

  // User data
  private String currentUser;
  private int userScore;

  // Quiz data
  private ArrayList<Question> questions;
  private ArrayList<Question> userQuestions;
  private int currentQuestionIndex;

  // UI components for quiz panel
  private JLabel questionLabel;
  private JRadioButton[] optionButtons;
  private ButtonGroup optionsGroup;
  private JButton nextButton;

  // UI components for signup panel
  private JTextField usernameField;
  private JLabel signupErrorLabel;

  // UI components for result panel
  private JLabel resultLabel;

  // File for storing user questions
  private static final String QUESTIONS_FILE = "questions.txt";

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new SimpleQuizApp().initialize(); }
    });
  }

  public void initialize() {
    frame = new JFrame("Simple Quiz App");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(600, 500); // Increased height for larger input fields
    frame.setMinimumSize(new Dimension(600, 500)); // Prevent resizing too small
    frame.setLocationRelativeTo(null);             // Center the window

    cardLayout = new CardLayout();
    mainPanel = new JPanel(cardLayout);

    // Initialize userQuestions
    userQuestions = new ArrayList<Question>();

    // Load user questions from file
    loadUserQuestions();

    // Initialize components
    createSampleQuestions();

    // Create panels
    mainPanel.add(createWelcomePanel(), "welcome");
    mainPanel.add(createSignupPanel(), "signup");
    mainPanel.add(createQuizPanel(), "quiz");
    mainPanel.add(createResultPanel(), "result");
    mainPanel.add(createCreateQuizPanel(), "createQuiz");

    frame.add(mainPanel);
    frame.setVisible(true);

    // Show welcome panel initially
    cardLayout.show(mainPanel, "welcome");
  }

  private void loadUserQuestions() {
    try (Scanner scanner = new Scanner(new File(QUESTIONS_FILE))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] parts = line.split("\\|");
        if (parts.length == 6) {
          String questionText = parts[0];
          String[] options =
              new String[] {parts[1], parts[2], parts[3], parts[4]};
          int correctIdx = Integer.parseInt(parts[5]);
          userQuestions.add(new Question(questionText, options, correctIdx));
        }
      }
    } catch (FileNotFoundException e) {
      // File doesn't exist yet, which is fine for first run
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          frame, "Error loading user questions: " + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void saveUserQuestions() {
    try (PrintWriter writer = new PrintWriter(new FileWriter(QUESTIONS_FILE))) {
      for (Question q : userQuestions) {
        String[] opts = q.getOptions();
        writer.println(q.getQuestionText() + "|" + opts[0] + "|" + opts[1] +
                       "|" + opts[2] + "|" + opts[3] + "|" +
                       q.getCorrectAnswer());
      }
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          frame, "Error saving user questions: " + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private JPanel createWelcomePanel() {
    JPanel panel = new JPanel(new BorderLayout(20, 20));
    panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    JLabel titleLabel = new JLabel("Welcome to Simple Quiz App", JLabel.CENTER);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 32));

    JButton startButton = new JButton("Start Quiz");
    startButton.setFont(new Font("Arial", Font.PLAIN, 18));
    startButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cardLayout.show(mainPanel, "signup");
      }
    });

    JButton createButton = new JButton("Create Quiz");
    createButton.setFont(new Font("Arial", Font.PLAIN, 18));
    createButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cardLayout.show(mainPanel, "createQuiz");
      }
    });

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(startButton);
    buttonPanel.add(createButton);

    panel.add(titleLabel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createSignupPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel label = new JLabel("Enter Username:");
    label.setFont(new Font("Arial", Font.PLAIN, 20));
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(label, gbc);

    usernameField = new JTextField(20);
    usernameField.setFont(new Font("Arial", Font.PLAIN, 18));
    gbc.gridx = 1;
    gbc.gridy = 0;
    panel.add(usernameField, gbc);

    JButton submitButton = new JButton("Start Quiz");
    submitButton.setFont(new Font("Arial", Font.PLAIN, 18));
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    panel.add(submitButton, gbc);

    signupErrorLabel = new JLabel("");
    signupErrorLabel.setForeground(Color.RED);
    signupErrorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
    gbc.gridy = 2;
    panel.add(signupErrorLabel, gbc);

    submitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
          signupErrorLabel.setText("Username cannot be empty.");
          return;
        }
        signupErrorLabel.setText("");
        currentUser = username;
        userScore = 0;
        currentQuestionIndex = 0;

        // Shuffle questions for new quiz session
        Collections.shuffle(questions);

        showQuestion();
        cardLayout.show(mainPanel, "quiz");
      }
    });

    return panel;
  }

  private JPanel createQuizPanel() {
    JPanel panel = new JPanel(new BorderLayout(20, 20));
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    questionLabel = new JLabel("Question here");
    questionLabel.setFont(new Font("Arial", Font.BOLD, 22));
    questionLabel.setVerticalAlignment(JLabel.TOP);

    panel.add(questionLabel, BorderLayout.NORTH);

    JPanel optionsPanel = new JPanel();
    optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

    optionButtons = new JRadioButton[4];
    optionsGroup = new ButtonGroup();

    for (int i = 0; i < 4; i++) {
      optionButtons[i] = new JRadioButton();
      optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 18));
      optionsGroup.add(optionButtons[i]);
      optionsPanel.add(optionButtons[i]);
      optionsPanel.add(Box.createVerticalStrut(10));
    }

    panel.add(optionsPanel, BorderLayout.CENTER);

    nextButton = new JButton("Next");
    nextButton.setFont(new Font("Arial", Font.PLAIN, 18));
    nextButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        checkAnswer();
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
          showQuestion();
        } else {
          // Quiz finished
          cardLayout.show(mainPanel, "result");
          showResult();
        }
      }
    });

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottomPanel.add(nextButton);

    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createResultPanel() {
    JPanel panel = new JPanel(new BorderLayout(20, 20));
    panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    JLabel thankYouLabel = new JLabel("Quiz Completed!", JLabel.CENTER);
    thankYouLabel.setFont(new Font("Arial", Font.BOLD, 28));

    resultLabel = new JLabel("", JLabel.CENTER);
    resultLabel.setFont(new Font("Arial", Font.PLAIN, 22));

    JButton restartButton = new JButton("Restart");
    restartButton.setFont(new Font("Arial", Font.PLAIN, 18));
    restartButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        usernameField.setText("");
        userScore = 0;
        currentQuestionIndex = 0;
        cardLayout.show(mainPanel, "welcome");
      }
    });

    JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
    centerPanel.add(thankYouLabel);
    centerPanel.add(resultLabel);

    panel.add(centerPanel, BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel();
    bottomPanel.add(restartButton);
    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createCreateQuizPanel() {
    JPanel panel = new JPanel(new BorderLayout(20, 20));
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JLabel headerLabel =
        new JLabel("Create Your Own Quiz Questions", JLabel.CENTER);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 26));
    panel.add(headerLabel, BorderLayout.NORTH);

    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.fill = GridBagConstraints.BOTH; // Allow components to expand
    gbc.weightx = 1.0;                  // Distribute horizontal space

    JLabel questionTextLabel = new JLabel("Question:");
    questionTextLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.weighty = 0.2; // Give question field more vertical space
    formPanel.add(questionTextLabel, gbc);

    JTextArea questionTextArea = new JTextArea(4, 40); // 4 rows, 40 columns
    questionTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
    questionTextArea.setLineWrap(true);
    questionTextArea.setWrapStyleWord(true);
    JScrollPane questionScrollPane = new JScrollPane(questionTextArea);
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    formPanel.add(questionScrollPane, gbc);

    JLabel[] optionLabels = new JLabel[4];
    JTextField[] optionFields = new JTextField[4];

    for (int i = 0; i < 4; i++) {
      optionLabels[i] = new JLabel("Option " + (i + 1) + ":");
      optionLabels[i].setFont(new Font("Arial", Font.PLAIN, 18));
      gbc.gridx = 0;
      gbc.gridy = i + 1;
      gbc.gridwidth = 1;
      gbc.weighty = 0.1;
      formPanel.add(optionLabels[i], gbc);

      optionFields[i] = new JTextField(30); // Increased from 20 to 30 columns
      optionFields[i].setFont(new Font("Arial", Font.PLAIN, 16));
      gbc.gridx = 1;
      gbc.gridy = i + 1;
      gbc.gridwidth = 3;
      formPanel.add(optionFields[i], gbc);
    }

    JLabel correctAnswerLabel = new JLabel("Correct Option (1-4):");
    correctAnswerLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 1;
    gbc.weighty = 0.1;
    formPanel.add(correctAnswerLabel, gbc);

    JTextField correctAnswerField =
        new JTextField(5); // Increased from 2 to 5 columns
    correctAnswerField.setFont(new Font("Arial", Font.PLAIN, 16));
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = 1;
    formPanel.add(correctAnswerField, gbc);

    JLabel createErrorLabel = new JLabel("");
    createErrorLabel.setForeground(Color.RED);
    createErrorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
    gbc.gridx = 0;
    gbc.gridy = 6;
    gbc.gridwidth = 4;
    gbc.weighty = 0.1;
    formPanel.add(createErrorLabel, gbc);

    JPanel buttonPanel = new JPanel();

    JButton addButton = new JButton("Add Question");
    addButton.setFont(new Font("Arial", Font.PLAIN, 18));
    buttonPanel.add(addButton);

    JButton clearButton = new JButton("Clear");
    clearButton.setFont(new Font("Arial", Font.PLAIN, 18));
    buttonPanel.add(clearButton);

    JButton doneButton = new JButton("Done");
    doneButton.setFont(new Font("Arial", Font.PLAIN, 18));
    buttonPanel.add(doneButton);

    panel.add(formPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String qText = questionTextArea.getText().trim();
        String[] opts = new String[4];
        for (int i = 0; i < 4; i++) {
          opts[i] = optionFields[i].getText().trim();
        }
        String correctStr = correctAnswerField.getText().trim();

        // Validation
        if (qText.isEmpty()) {
          createErrorLabel.setText("Question text cannot be empty");
          return;
        }

        for (int i = 0; i < 4; i++) {
          if (opts[i].isEmpty()) {
            createErrorLabel.setText("All options must be filled");
            return;
          }
        }

        int correctIdx;
        try {
          correctIdx = Integer.parseInt(correctStr) - 1;
          if (correctIdx < 0 || correctIdx > 3) {
            createErrorLabel.setText("Correct option must be between 1 and 4");
            return;
          }
        } catch (NumberFormatException ex) {
          createErrorLabel.setText(
              "Correct option must be a number between 1 and 4");
          return;
        }

        // Check for duplicate question
        for (Question q : userQuestions) {
          if (q.getQuestionText().equalsIgnoreCase(qText)) {
            createErrorLabel.setText("This question already exists");
            return;
          }
        }

        createErrorLabel.setText("");
        Question newQuestion = new Question(qText, opts, correctIdx);
        userQuestions.add(newQuestion);

        // Save to file
        saveUserQuestions();

        // Clear fields
        questionTextArea.setText("");
        for (int i = 0; i < 4; i++) {
          optionFields[i].setText("");
        }
        correctAnswerField.setText("");

        JOptionPane.showMessageDialog(
            frame,
            "Question added! Total custom questions: " + userQuestions.size());
      }
    });

    clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        questionTextArea.setText("");
        for (int i = 0; i < 4; i++) {
          optionFields[i].setText("");
        }
        correctAnswerField.setText("");
        createErrorLabel.setText("");
      }
    });

    doneButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (userQuestions.isEmpty()) {
          JOptionPane.showMessageDialog(
              frame, "Please add at least one question before finishing.");
          return;
        }
        // Update main questions list with user questions
        questions.clear();
        createSampleQuestions();
        cardLayout.show(mainPanel, "welcome");
      }
    });

    return panel;
  }

  private void createSampleQuestions() {
    questions = new ArrayList<Question>();

    questions.add(
        new Question("What is the capital of France?",
                     new String[] {"London", "Paris", "Berlin", "Madrid"}, 1));

    questions.add(
        new Question("Which algorithm has O(n log n) average time complexity?",
                     new String[] {"Bubble Sort", "Quick Sort",
                                   "Selection Sort", "Insertion Sort"},
                     1));

    questions.add(new Question(
        "What does HTML stand for?",
        new String[] {"Hyper Text Markup Language", "High Tech Modern Language",
                      "Home Tool Markup Language",
                      "Hyperlinks and Text Markup Language"},
        0));

    if (userQuestions != null && !userQuestions.isEmpty()) {
      questions.addAll(userQuestions);
    }
  }

  private void showQuestion() {
    Question q = questions.get(currentQuestionIndex);
    questionLabel.setText("<html><body style='width: 500px'>" +
                          (currentQuestionIndex + 1) + ". " +
                          q.getQuestionText() + "</body></html>");

    String[] opts = q.getOptions();
    optionsGroup.clearSelection();
    for (int i = 0; i < optionButtons.length; i++) {
      optionButtons[i].setText(opts[i]);
    }
  }

  private void checkAnswer() {
    Question q = questions.get(currentQuestionIndex);
    int selected = -1;
    for (int i = 0; i < optionButtons.length; i++) {
      if (optionButtons[i].isSelected()) {
        selected = i;
        break;
      }
    }

    if (selected == q.getCorrectAnswer()) {
      userScore++;
    }
  }

  private void showResult() {
    String resultText = String.format(
        "<html>Thank you, <b>%s</b>!<br>Your score: <b>%d</b> out of <b>%d</b>.</html>",
        currentUser, userScore, questions.size());
    resultLabel.setText(resultText);
  }
}

class Question {

  private String questionText;
  private String[] options;
  private int correctAnswer;

  public Question(String questionText, String[] options, int correctAnswer) {
    this.questionText = questionText;
    this.options = options;
    this.correctAnswer = correctAnswer;
  }

  public String getQuestionText() { return questionText; }

  public String[] getOptions() { return options; }

  public int getCorrectAnswer() { return correctAnswer; }
}
