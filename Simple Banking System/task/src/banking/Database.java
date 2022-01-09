package banking;

import java.sql.*;

public class Database {
    static String SUCCESS = "Success!";
    static String TECHNICAL_PROBLEM ="Technical Problem. Please try again";
    static String ACCOUNT_CLOSED = "The account has been closed!";

    String dbUrl;
    static Database dbInstance;
    private Database() {}

    public static Database getInstance()
    {
        if (dbInstance == null)
        {
            dbInstance = new Database();
        }

        return dbInstance;
    }

    public boolean validateLogin(String creditNumber, String pin)
    {
        String sql = "SELECT number, pin FROM card WHERE number = ? AND pin = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            pstmt.setString(1, creditNumber);
            pstmt.setString(2, pin);
            ResultSet rs  = pstmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public void closeAccount(Account account)
    {
        String sql = "DELETE FROM card WHERE number = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the corresponding param
            pstmt.setString(1, account.creditNumber);
            // execute the delete statement
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.println(ACCOUNT_CLOSED);
    }

    public int getCurrentBalance(Account account)
    {
        String sql = " Select balance FROM card WHERE number = " + account.creditNumber;
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            return rs.getInt("balance");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }

    public boolean existsCreditCard(String number)
    {
        String sql = " Select number FROM card WHERE number = " + number;
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            return rs.getString("number").equals(number);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    /**
     * Transfer money to another bank account and Check if:
     * 1. transferNumber is not the same creidt number of the current account
     * 2. current account has enough money which can be transfered
     * 3. the credit number is a valid one with the Luhn Algortihmn
     * 4. the card exists in the database
     * 5. make the transaction and return SUCCESS
     * @param account
     * @param numberToTransfer
     * @param value
     * @return
     */
    public synchronized String transferMoney(Account account, String numberToTransfer, int value)
    {
        if(!transferTransaction(account, numberToTransfer, value)) return TECHNICAL_PROBLEM;

        return SUCCESS;
    }

    private boolean transferTransaction(Account account, String numberToTransfer, int value) {
        String sqlFromAccount = "UPDATE card set balance = balance - ?" +
                " \nWHERE number = ?";

        String sqlToAccount = "UPDATE card set balance = balance + ?"  +
                "\nWHERE number = ?";

        ResultSet rs = null;
        Connection conn = null;
        PreparedStatement pstmt1 = null, pstmt2 = null;

        try {
            // connect to the database
            conn = this.connect();
            if (conn == null)
                return false;

            // set auto-commit mode to false
            conn.setAutoCommit(false);

            // 1. update fromAccount
            pstmt1 = conn.prepareStatement(sqlFromAccount);
            pstmt1.setInt(1, value);
            pstmt1.setString(2, account.creditNumber);
            int rowAffected = pstmt1.executeUpdate();

            if (rowAffected != 1) {
                conn.rollback();
            }
            // 2. update toAccount
            pstmt2 = conn.prepareStatement(sqlToAccount);
            pstmt2.setInt(1, value);
            pstmt2.setString(2, numberToTransfer);
            pstmt2.executeUpdate();

            // commit work
            conn.commit();

            return true;

        } catch (SQLException e1) {
            System.out.println(e1.getMessage());
            return false;
        } finally {
            try {
                if (pstmt1 != null) {
                    pstmt1.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e3) {
                System.out.println(e3.getMessage());
            }
        }
    }

    public void addIncome(Account account, int addIncome)
    {
        String sql = "UPDATE card SET balance = balance + ?"
                + " \nWHERE number = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the corresponding param
            pstmt.setInt(1, addIncome);
            pstmt.setString(2, account.creditNumber);
            // update
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public  void createDatebase(String filename) {
        String url = "jdbc:sqlite:" + filename;
        this.dbUrl = url;

        try {
            Connection conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        createTable(url);
    }

    private void createTable(String url) {
        String sql = "CREATE TABLE IF NOT EXISTS card ( \n" +
                "id integer PRIMARY key, \n" +
                "number VARCHAR(20), \n" +
                "pin VARCHAR(5), \n" +
                "balance INTEGER \n" +
                ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Connect to the test.db database
     *
     * @return the Connection object
     */
    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    /**
     * Insert a new row into the card table
     *
     * @param creditNumber
     * @param pin
     */
    public void insertAccount(String creditNumber, String pin) {
        String sql = "INSERT INTO card (number, pin, balance) VALUES(?,?,?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, creditNumber);
             pstmt.setString(2, pin);
             pstmt.setInt(3, 0);
             pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
