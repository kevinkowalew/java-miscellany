package test.integration;

import databases.sql.Column;
import databases.sql.SqlTableController;
import databases.sql.postgresql.statements.builders.*;
import databases.sql.postgresql.statements.WhereClause;
import org.junit.Test;
import test.mocks.*;

import java.util.List;
import java.util.Optional;

import static databases.sql.postgresql.statements.WhereClauseOperator.EQUALS;
import static org.junit.Assert.*;
import static test.mocks.MockUsersColumn.EMAIL;
import static test.mocks.MockUsersColumn.ID;

public class PostgresqlDatabaseControllerIntegrationTests {
    private static final Integer MOCK_ID_ONE = 1;
    private static final String MOCK_ID_ONE_STRING = "1";
    private static final String MOCK_EMAIL_ONE = "john.doe@gmail.com";

    private static final Integer MOCK_ID_TWO = 2;
    private static final String MOCK_ID_TWO_STRING = "2";
    private static final String MOCK_EMAIL_TWO = "jane.doe@gmail.com";

    private static final String MOCK_SALT = "icsfwef91p2;UF!@PUFP!@P";
    private static final String MOCK_HASHED_PASSWORD = "wef 0p1q2q1q;lwelq2jeqwjlqkwjl";

    private static SqlTableController<MockUser> userController = MockUserDatabaseControllerModule.createController();
    private static SqlTableController<MockMessage> messagesController = MockMessageDatabaseControllerModule.createController();

    private static InsertStatement.Builder VALID_INSERT_STATEMENT_BUILDER = userController.insertStatementBuilder()
            .insert(MOCK_EMAIL_ONE, EMAIL)
            .insert(MOCK_SALT, MockUsersColumn.SALT)
            .insert(MOCK_HASHED_PASSWORD, MockUsersColumn.HASHED_PASSWORD);

    @Test
    public void test_TableExists_ReturnsTrue_For_Existing_Table() {
        // Arrange...
        userController.createTable();

        // Act...
        boolean tableExists = userController.tableExists();

        // Assert...
        assert (tableExists);
        final String tableName = new MockUserDatabaseControllerModule().getSchema().getTableName();
        final String expectedMessage = String.format("Created table with name %s", tableName);
        assertLoggerMessageWasRecorded(expectedMessage);
    }

    @Test
    public void test_TableExists_ReturnsFalse_For_Nonexistent_Table() {
        // Arrange...
        userController.dropTable();

        // Act...
        boolean tableExists = userController.tableExists();

        // Assert...
        assertFalse(tableExists);
    }

    @Test
    public void test_CreateTable_HappyPath() {
        // Arrange...
        userController.dropTable();

        // Act...
        boolean success = userController.createTable();

        // Assert...
        assert (success);
    }

    @Test
    public void test_CreateTable_RainyDay_ExistingTable_ReturnsFalse() {
        // Arrange...
        userController.dropTable();

        // Act...
        boolean tableExists = userController.tableExists();

        // Assert...
        assertFalse(tableExists);
    }

    @Test
    public void test_Insert_SunnyDay() {
        // Arrange...
        final InsertStatement.Builder insertBuilder = userController.insertStatementBuilder()
                .insert(MOCK_EMAIL_ONE, EMAIL)
                .insert(MOCK_SALT, MockUsersColumn.SALT)
                .insert(MOCK_HASHED_PASSWORD, MockUsersColumn.HASHED_PASSWORD);
        dropAndRecreateTableIfNeeded(userController);

        // Act...
        Optional<MockUser> user = userController.insert(insertBuilder);

        // Assert...
        assert (user.isPresent());
        assertEquals(MOCK_ID_ONE, user.get().getId());
        assertEquals(MOCK_EMAIL_ONE, user.get().getEmail());
        assertEquals(MOCK_SALT, user.get().getSalt());
        assertEquals(MOCK_HASHED_PASSWORD, user.get().getHashedPassword());
    }


    @Test
    public void test_Insert_SunnyDay_Return_SpecificValues() {
        // Arrange...
        final InsertStatement.Builder insertBuilder = userController.insertStatementBuilder()
                .insert(MOCK_EMAIL_ONE, EMAIL)
                .insert(MOCK_SALT, MockUsersColumn.SALT)
                .insert(MOCK_HASHED_PASSWORD, MockUsersColumn.HASHED_PASSWORD)
                .returning(ID, EMAIL);
        dropAndRecreateTableIfNeeded(userController);

        // Act...
        Optional<MockUser> user = userController.insert(insertBuilder);

        // Assert...
        assert (user.isPresent());
        assertEquals(MOCK_ID_ONE, user.get().getId());
        assertEquals(MOCK_EMAIL_ONE, user.get().getEmail());
        assertEquals("", user.get().getSalt());
        assertEquals("", user.get().getHashedPassword());
    }

    @Test
    public void test_Insert_RainyDay_MissingValues() {
        // Arrange...
        final InsertStatement.Builder insertBuilder = userController.insertStatementBuilder()
                .insert("154321", ID)
                .insert("john.doe@gmail.com", EMAIL)
                .insert("icsfwef91p2;UF!@PUFP!@P", MockUsersColumn.SALT);
        final SelectStatement.Builder selectStatement = userController.selectStatementBuilder();

        // Act...
        boolean success = userController.insert(insertBuilder).isPresent();

        // Assert...
        assertFalse(success);
    }

    @Test
    public void test_Insert_RainyDay_NonexistentColumn() {
        // Arrange...
        final Column missingColumn = Column.with("Missing Column", Column.Type.VARCHAR_255, false);
        final InsertStatement.Builder insertBuilder = userController.insertStatementBuilder()
                .insert("154321", missingColumn);
        final SelectStatement.Builder selectStatement = userController.selectStatementBuilder();

        // Act...
        boolean success = userController.insert(insertBuilder).isPresent();

        // Assert...
        assertFalse(success);
    }

    @Test
    public void test_Delete_SunnyDay() {
        // Arrange...
        final InsertStatement.Builder insertBuilder = VALID_INSERT_STATEMENT_BUILDER;
        final WhereClause clause = new WhereClause(ID, EQUALS, MOCK_ID_ONE);
        final SelectStatement.Builder selectBuilder = userController.selectStatementBuilder().where(clause);
        final DeleteStatement.Builder deleteBuilder = userController.deleteStatementBuilder().where(clause);

        // Act...
        boolean insertSuccess = userController.insert(insertBuilder).isPresent();
        Optional<List<MockUser>> firstReadResults = userController.read(selectBuilder);
        boolean deleteSuccess = userController.delete(deleteBuilder);
        Optional<List<MockUser>> secondReadResults = userController.read(selectBuilder);

        // Assert...
        assert (insertSuccess);
        assert (firstReadResults.isPresent());
        assertEquals(firstReadResults.get().size(), 1);
        assert (deleteSuccess);
        assert (secondReadResults.isPresent());
        assert (secondReadResults.get().isEmpty());
    }

    @Test
    public void test_Update_SunnyDay() {
        // Arrange...
        final String updatedSalt = "my new salt";
        final String updatedHashedPassword = "my new hashed password";
        final InsertStatement.Builder insertBuilder = VALID_INSERT_STATEMENT_BUILDER;
        final UpdateStatement.Builder updateBuilder = userController.updateStatementBuilder()
                .where(ID, EQUALS, MOCK_ID_ONE)
                .update(MOCK_EMAIL_TWO, EMAIL)
                .update(updatedSalt, MockUsersColumn.SALT)
                .update(updatedHashedPassword, MockUsersColumn.HASHED_PASSWORD);

        // Act...
        dropAndRecreateTableIfNeeded(userController);
        boolean insertSuccess = userController.insert(insertBuilder).isPresent();
        Optional<List<MockUser>> firstReadResults = userController.read(userController.selectStatementBuilder());
        boolean updateSuccess = userController.update(updateBuilder);
        Optional<List<MockUser>> secondReadResults = userController.read(userController.selectStatementBuilder());

        // Assert...
        assertEquals(firstReadResults.get().size(), 1);
        MockUser user = firstReadResults.get().get(0);
        assertEquals(MOCK_ID_ONE, user.getId());
        assertEquals(MOCK_EMAIL_ONE, user.getEmail());
        assertEquals(MOCK_SALT, user.getSalt());
        assertEquals(MOCK_HASHED_PASSWORD, user.getHashedPassword());

        assert (updateSuccess);

        assertEquals(secondReadResults.get().size(), 1);
        user = secondReadResults.get().get(0);
        assertEquals(MOCK_ID_ONE, user.getId());
        assertEquals(MOCK_EMAIL_TWO, user.getEmail());
        assertEquals(updatedSalt, user.getSalt());
        assertEquals(updatedHashedPassword, user.getHashedPassword());
    }

    @Test
    public void test_Read_WhereClause_OR_Handling() {
        // Arrange...
        insertTwoMockUsers();
        final SelectStatement.Builder selectBuilder = userController.selectStatementBuilder()
                .where(EMAIL, EQUALS, MOCK_EMAIL_ONE)
                .or(EMAIL, EQUALS, MOCK_EMAIL_TWO);

        // Act...
        Optional<List<MockUser>> results = userController.read(selectBuilder);

        // Assert...
        assert (results.isPresent());
        assertEquals(2, results.get().size());

        MockUser user = results.get().get(0);
        assertEquals(MOCK_ID_ONE, user.getId());
        assertEquals(MOCK_EMAIL_ONE, user.getEmail());
        assertEquals(MOCK_SALT, user.getSalt());
        assertEquals(MOCK_HASHED_PASSWORD, user.getHashedPassword());

        user = results.get().get(1);
        assertEquals(MOCK_ID_TWO, user.getId());
        assertEquals(MOCK_EMAIL_TWO, user.getEmail());
        assertEquals(MOCK_SALT, user.getSalt());
        assertEquals(MOCK_HASHED_PASSWORD, user.getHashedPassword());
    }

    @Test
    public void test_Read_WhereClause_AND_Handling() {
        // Arrange...
        insertTwoMockUsers();
        final SelectStatement.Builder selectBuilder = userController.selectStatementBuilder()
                .where(ID, EQUALS, "2")
                .and(EMAIL, EQUALS, "jane.doe@gmail.com");

        // Act...
        Optional<List<MockUser>> results = userController.read(selectBuilder);

        // Assert...
        assert (results.isPresent());
        assertEquals(1, results.get().size());
        MockUser user = results.get().get(0);
        assertEquals(MOCK_ID_TWO, user.getId());
        assertEquals(MOCK_EMAIL_TWO, user.getEmail());
        assertEquals(MOCK_SALT, user.getSalt());
        assertEquals(MOCK_HASHED_PASSWORD, user.getHashedPassword());
    }

    @Test
    public void test_Inner_Join() {
        // Arrange...
        insertTwoMockUsers();
        insertTwoMockMessages();
        Join messagesUsersJoin = Join.innerJoin(
            MockMessageColumn.ID.getReferenceInTable("Users"),
            MockMessageColumn.ID.getReferenceInTable("Messages")
        );

        JoinStatement.Builder builder = messagesController.joinStatementBuilder().select(
                MockMessageColumn.ID.getReferenceInTable("Messages"),
                MockMessageColumn.RECIPIENT.getReferenceInTable("Messages"),
                MockMessageColumn.SENDER_ID.getReferenceInTable("Messages"),
                MockMessageColumn.TEXT.getReferenceInTable("Messages"),
                MockUsersColumn.ID.getReferenceInTable("Users"),
                MockUsersColumn.EMAIL.getReferenceInTable("Users")
        ).join(messagesUsersJoin);

        // Act...
        Optional<List<MockMessage>> result = messagesController.join(builder);

        // Assert...
        assert(result.isPresent());
        assert(result.get().size() == 1);
    }

    private void insertTwoMockMessages() {
        // Arrange...
        dropAndRecreateTableIfNeeded(messagesController);
        final InsertStatement.Builder builder = messagesController.insertStatementBuilder()
                .insert(MOCK_ID_ONE_STRING, MockMessageColumn.SENDER_ID)
                .insert(MOCK_ID_TWO_STRING, MockMessageColumn.RECIPIENT)
                .insert("hey!", MockMessageColumn.TEXT);

        // Act...
        Optional<MockMessage> result = messagesController.insert(builder);

         //Assert...
        assert (result.isPresent());
    }

    private void insertTwoMockUsers() {
        // Arrange...
        dropAndRecreateTableIfNeeded(userController);

        // Act...
        final InsertStatement.Builder firstInsert = VALID_INSERT_STATEMENT_BUILDER;
        final InsertStatement.Builder secondInsert = userController.insertStatementBuilder()
                .insert(MOCK_EMAIL_TWO, EMAIL)
                .insert(MOCK_SALT, MockUsersColumn.SALT)
                .insert(MOCK_HASHED_PASSWORD, MockUsersColumn.HASHED_PASSWORD);

        // Assert...
        assert (userController.insert(firstInsert).isPresent());
        assert (userController.insert(secondInsert).isPresent());
    }

    private void dropAndRecreateTableIfNeeded(SqlTableController controller) {
        if (controller.tableExists()) {
            assert (controller.dropTable());
        }
        assert (controller.createTable());
    }

    private void assertLoggerMessageWasRecorded(final String expectedMessage) {
        System.out.println(expectedMessage);
    }
}