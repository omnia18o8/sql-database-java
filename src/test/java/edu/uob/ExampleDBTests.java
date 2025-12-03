package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class ExampleDBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test data, then queries it.
    // It then checks the response to see that a couple of the entries in the table are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"), "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"), "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n"," ").trim();
        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the entry)
        String lastToken = tokens[tokens.length-1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was " + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Simon"), "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }

    // Test to make sure that the [ERROR] tag is returned in the case of an error (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        assertTrue(response.contains("[ERROR]"), "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"), "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }


    //MY OWN TESTS
    @Test
    public void testCRUDOperations() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");


        sendCommandToServer("CREATE TABLE employees (name, age, salary);");


        sendCommandToServer("INSERT INTO employees VALUES ('Alice', 30, 50000);");
        sendCommandToServer("INSERT INTO employees VALUES ('Bob', 40, 70000);");


        String selectResponse = sendCommandToServer("SELECT * FROM employees;");
        assertTrue(selectResponse.contains("Alice"), "Alice should be in the table.");
        assertTrue(selectResponse.contains("Bob"), "Bob should be in the table.");

        sendCommandToServer("UPDATE employees SET salary = 60000 WHERE name == 'Alice';");
        String updatedResponse = sendCommandToServer("SELECT salary FROM employees WHERE name == 'Alice';");
        assertTrue(updatedResponse.contains("60000"), "Alice's salary should be updated to 60000.");


        sendCommandToServer("DELETE FROM employees WHERE name == 'Bob';");
        String afterDeleteResponse = sendCommandToServer("SELECT * FROM employees;");
        assertFalse(afterDeleteResponse.contains("Bob"), "Bob should be deleted from the table.");


        sendCommandToServer("DROP TABLE employees;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }

    @Test
    public void testTableAlterationsAndDuplicateColumns() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");


        String duplicateColumnResponse = sendCommandToServer("CREATE TABLE students (name, age, name);");
        assertTrue(duplicateColumnResponse.contains("[ERROR]"), "Duplicate column names should return an error.");


        sendCommandToServer("CREATE TABLE students (name, age);");
        sendCommandToServer("ALTER TABLE students ADD grade;");


        sendCommandToServer("INSERT INTO students VALUES ('Charlie', 22, 'B');");
        String selectResponse = sendCommandToServer("SELECT * FROM students;");
        assertTrue(selectResponse.contains("Charlie"), "Charlie should be in the table.");
        assertTrue(selectResponse.contains("B"), "Grade column should exist.");


        sendCommandToServer("ALTER TABLE students DROP age;");
        String afterDropResponse = sendCommandToServer("SELECT * FROM students;");
        assertFalse(afterDropResponse.contains("22"), "Age column should be removed.");


        sendCommandToServer("DROP TABLE students;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }

    @Test
    public void testJoinsAndComparisons() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");


        sendCommandToServer("CREATE TABLE coursework (task, submission);");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");


        sendCommandToServer("INSERT INTO coursework VALUES ('OXO', 1);");
        sendCommandToServer("INSERT INTO coursework VALUES ('DB', 2);");


        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");


        String joinResponse = sendCommandToServer("JOIN coursework AND marks ON submission AND id;");
        assertTrue(joinResponse.contains("coursework.task"), "Joined table should rename attributes.");
        assertTrue(joinResponse.contains("marks.name"), "Joined table should rename attributes.");

        sendCommandToServer("CREATE TABLE products (name, price);");
        sendCommandToServer("INSERT INTO products VALUES ('Laptop', 1200);");
        sendCommandToServer("INSERT INTO products VALUES ('Phone', 800);");


        String responseGT = sendCommandToServer("SELECT name FROM products WHERE price > 1000;");
        assertTrue(responseGT.contains("Laptop"), "Laptop should be in the result for price > 1000.");
        assertFalse(responseGT.contains("Phone"), "Phone should not be in the result for price > 1000.");

        String responseLIKE = sendCommandToServer("SELECT name FROM products WHERE name LIKE 'Lap';");
        assertTrue(responseLIKE.contains("Laptop"), "LIKE should return exact substring matches.");


        sendCommandToServer("DROP TABLE coursework;");
        sendCommandToServer("DROP TABLE marks;");
        sendCommandToServer("DROP TABLE products;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }

    @Test
    public void testSpecialCharactersAndSignedNumbers() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");


        sendCommandToServer("CREATE TABLE test_special (name, symbol, price, temperature);");


        sendCommandToServer("INSERT INTO test_special VALUES ('Alice@2024', '#$%^&*', 3.14, -10.5);");
        sendCommandToServer("INSERT INTO test_special VALUES ('Bob_+', '(!)_-', +9.99, -2.75);");


        String selectResponse = sendCommandToServer("SELECT * FROM test_special;");
        assertTrue(selectResponse.contains("Alice@2024"), "Special characters in name should be retrieved.");
        assertTrue(selectResponse.contains("#$%^&*"), "Special symbols should be retrieved.");
        assertTrue(selectResponse.contains("+9.99"), "Positive float should be retrieved.");
        assertTrue(selectResponse.contains("-2.75"), "Negative float should be retrieved.");

        sendCommandToServer("DROP TABLE test_special;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }

    @Test
    public void testConditionalSelectUpdateDelete() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");


        sendCommandToServer("CREATE TABLE students (name, age, grade, passed);");


        sendCommandToServer("INSERT INTO students VALUES ('Alice', 20, 85, TRUE);");
        sendCommandToServer("INSERT INTO students VALUES ('Bob', 22, 40, FALSE);");
        sendCommandToServer("INSERT INTO students VALUES ('Charlie', 23, 70, TRUE);");
        sendCommandToServer("INSERT INTO students VALUES ('David', 21, 30, FALSE);");

        String response1 = sendCommandToServer("SELECT name FROM students WHERE grade >= 50 AND passed == TRUE;");
        assertTrue(response1.contains("Alice"), "Alice should be selected (grade >= 50 AND passed == TRUE).");
        assertTrue(response1.contains("Charlie"), "Charlie should be selected (grade >= 50 AND passed == TRUE).");
        assertFalse(response1.contains("Bob"), "Bob should not be selected (failed).");

        String response2 = sendCommandToServer("SELECT name FROM students WHERE age < 22 OR grade > 80;");
        assertTrue(response2.contains("Alice"), "Alice should be selected (age < 22 OR grade > 80).");
        assertTrue(response2.contains("David"), "David should be selected (age < 22 OR grade > 80).");
        assertFalse(response2.contains("Charlie"), "Charlie should not be selected.");

        String response3 = sendCommandToServer("SELECT name FROM students WHERE (age > 20 AND grade < 50) OR passed == TRUE;");
        assertTrue(response3.contains("Alice"), "Alice should be selected.");
        assertTrue(response3.contains("Charlie"), "Charlie should be selected.");
        assertTrue(response3.contains("Bob"), "Bob should be selected.");
        assertTrue(response3.contains("David"), "David should be selected.");


        sendCommandToServer("UPDATE students SET passed = TRUE WHERE grade <= 50 AND passed == FALSE;");
        String updateResponse = sendCommandToServer("SELECT passed FROM students WHERE name == 'Bob';");
        assertTrue(updateResponse.contains("TRUE"), "Bob should now be marked as passed.");

        sendCommandToServer("DELETE FROM students WHERE age > 22 OR grade < 50;");
        String deleteResponse = sendCommandToServer("SELECT * FROM students;");
        assertFalse(deleteResponse.contains("Charlie"), "Charlie should be deleted.");
        assertFalse(deleteResponse.contains("David"), "David should be deleted.");
        assertTrue(deleteResponse.contains("Alice"), "Alice should remain in the table.");
        assertFalse(deleteResponse.contains("Bob"), "Bob should be deleted.");


        sendCommandToServer("DROP TABLE students;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }


    @Test
    public void testIdPersistsAfterDeletion() {
        String dbName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE employees (name, age);");

        sendCommandToServer("INSERT INTO employees VALUES ('Alice', 30);");
        sendCommandToServer("INSERT INTO employees VALUES ('Bob', 40);");
        sendCommandToServer("INSERT INTO employees VALUES ('Charlie', 50);");


        String responseBeforeDelete = sendCommandToServer("SELECT * FROM employees;");
        assertTrue(responseBeforeDelete.contains("Alice"), "Alice should have ID 1.");
        assertTrue(responseBeforeDelete.contains("2\tBob"), "Bob should have ID 2.");
        assertTrue(responseBeforeDelete.contains("3\tCharlie"), "Charlie should have ID 3.");


        sendCommandToServer("DELETE FROM employees where id < 4;");
        System.out.println(responseBeforeDelete);


        sendCommandToServer("INSERT INTO employees VALUES ('David', 35);");
        String responseAfterDelete = sendCommandToServer("SELECT * FROM employees;");
        System.out.println(responseAfterDelete);
        assertTrue(responseAfterDelete.contains("4\tDavid"), "David should have ID 4 after deletion.");


        sendCommandToServer("INSERT INTO employees VALUES ('Eve', 45);");
        String responseFinal = sendCommandToServer("SELECT * FROM employees;");
        assertTrue(responseFinal.contains("5\tEve"), "Eve should have ID 5 after David.");


        sendCommandToServer("DROP TABLE employees;");
        sendCommandToServer("DROP DATABASE " + dbName + ";");
    }


}