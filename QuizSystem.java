import java.sql.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class QuizSystem {

    static final int ADD_QUESTION = 1;
    static final int EDIT_QUESTION = 2;
    static final int DELETE_QUESTION = 3;
    static final int VIEW_ALL_QUESTIONS = 4;
    static final int VIEW_SCORES = 5;
    static final int GENERATE_REPORT = 6;
    static final int ADMIN_LOGOUT = 7;

    static final int TAKE_QUIZ = 1;
    static final int VIEW_SCORES_USER = 2;
    static final int RESET_PASSWORD = 3;
    static final int USER_LOGOUT = 4;

    static final int SIGNUP_ADMIN = 1;
    static final int SIGNUP_USER = 2;
    static final int LOGIN_ADMIN = 3;
    static final int LOGIN_USER = 4;
    static final int EXIT = 5;

    static Connection connection;

    // Database connection method
    static void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:quiz_system.db");
            System.out.println("Connected to the database.");
        } catch (SQLException e) {
            System.out.println("Error connecting to the database.");
            e.printStackTrace();
        }
    }

    // Create tables in the database
    static void createTables() {
        String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "is_admin BOOLEAN NOT NULL)";

        String createQuestionTable = "CREATE TABLE IF NOT EXISTS questions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "quiz_id TEXT NOT NULL, " +
                "question TEXT NOT NULL, " +
                "answer TEXT NOT NULL)";

        String createResultTable = "CREATE TABLE IF NOT EXISTS results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "quiz_id TEXT NOT NULL, " +
                "score INTEGER NOT NULL, " +
                "date_taken TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES users(id))";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUserTable);
            stmt.execute(createQuestionTable);
            stmt.execute(createResultTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class Question {
        String quizId;
        String question;
        String answer;

        Question(String quizId, String question, String answer) {
            this.quizId = quizId;
            this.question = question;
            this.answer = answer;
        }
    }

    static class User {
        int id;
        String username;
        String password;
        boolean isAdmin;

        User(int id, String username, String password, boolean isAdmin) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.isAdmin = isAdmin;
        }
    }

    static class Result {
        int userId;
        String quizId;
        int score;
        String dateTaken;

        Result(int userId, String quizId, int score, String dateTaken) {
            this.userId = userId;
            this.quizId = quizId;
            this.score = score;
            this.dateTaken = dateTaken;
        }
    }

    // Database operations for Users
    static boolean isUsernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static void signUpAdmin(String username, String password) {
        if (isUsernameExists(username)) {
            System.out.println("Username already exists.");
            return;
        }

        String insertAdmin = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertAdmin)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setBoolean(3, true);
            pstmt.executeUpdate();
            System.out.println("Admin account created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void signUpUser(String username, String password) {
        if (isUsernameExists(username)) {
            System.out.println("Username already exists.");
            return;
        }

        String insertUser = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setBoolean(3, false);
            pstmt.executeUpdate();
            System.out.println("User account created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean authenticateAdmin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND is_admin = 1";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND is_admin = 0";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Database operations for Questions
    static void addQuizQuestion(String quizId, String question, String answer) {
        String insertQuestion = "INSERT INTO questions (quiz_id, question, answer) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuestion)) {
            pstmt.setString(1, quizId);
            pstmt.setString(2, question);
            pstmt.setString(3, answer);
            pstmt.executeUpdate();
            System.out.println("Question added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static List<Question> getQuestionsByQuizId(String quizId) {
        List<Question> quizQuestions = new ArrayList<>();
        String query = "SELECT * FROM questions WHERE quiz_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, quizId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String q = rs.getString("question");
                String a = rs.getString("answer");
                quizQuestions.add(new Question(quizId, q, a));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return quizQuestions;
    }

    // Database operations for Results
    static void recordResult(int userId, String quizId, int score, String dateTaken) {
        String insertResult = "INSERT INTO results (user_id, quiz_id, score, date_taken) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertResult)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, quizId);
            pstmt.setInt(3, score);
            pstmt.setString(4, dateTaken);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static List<Result> getResultsByQuizId(String quizId) {
        List<Result> quizResults = new ArrayList<>();
        String query = "SELECT * FROM results WHERE quiz_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, quizId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                int score = rs.getInt("score");
                String dateTaken = rs.getString("date_taken");
                quizResults.add(new Result(userId, quizId, score, dateTaken));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return quizResults;
    }

    // Main method and user interaction logic
    public static void main(String[] args) {
        connectToDatabase();
        createTables();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("+----------------------------------------+");
            System.out.println("| ************ Main Menu *************** |");
            System.out.println("+----------------------------------------+");
            System.out.println("| 1. Admin Signup                        |");
            System.out.println("| 2. User Signup                         |");
            System.out.println("| 3. Admin Login                         |");
            System.out.println("| 4. User Login                          |");
            System.out.println("| 5. Exit                                |");
            System.out.println("+----------------------------------------+");

            System.out.print("Enter your choice: ");
            int choice = -1;
            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.nextLine(); // Consume invalid input
                continue;
            }

            switch (choice) {
                case SIGNUP_ADMIN:
                    System.out.print("Enter Username: ");
                    String adminUsername = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String adminPassword = scanner.nextLine();
                    signUpAdmin(adminUsername, adminPassword);
                    break;

                case SIGNUP_USER:
                    System.out.print("Enter Username: ");
                    String userUsername = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String userPassword = scanner.nextLine();
                    signUpUser(userUsername, userPassword);
                    break;

                case LOGIN_ADMIN:
                    System.out.print("Enter Username: ");
                    String loginAdminUsername = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String loginAdminPassword = scanner.nextLine();
                    if (authenticateAdmin(loginAdminUsername, loginAdminPassword)) {
                        System.out.println("Admin login successful.");
                        // menuAdmin(scanner);
                    } else {
                        System.out.println("Invalid username or password for admin.");
                    }
                    break;

                case LOGIN_USER:
                    System.out.print("Enter Username: ");
                    String loginUserUsername = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String loginUserPassword = scanner.nextLine();
                    if (authenticateUser(loginUserUsername, loginUserPassword)) {
                        System.out.println("User login successful.");
                        // Find user ID
                        // int userId = users.stream().filter(u -> u.username.equals(loginUserUsername)).findFirst().get().id;
                        // menuUser(scanner, userId);
                    } else {
                        System.out.println("Invalid username or password for user.");
                    }
                    break;

                case EXIT:
                    System.out.println("Exiting the system.");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }
}

