package databases.sql;

import com.google.inject.Inject;
import databases.core.*;
import databases.sql.postgresql.deserializers.TableExistsDeserializer;
import databases.sql.postgresql.statements.*;
import databases.sql.postgresql.statements.builders.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base class for concrete SQL CRUD implementations to share logic
 */
public class SqlTableController<T> implements Database<T> {
    @Inject
    private SqlExecutor executor;

    @Inject
    private DatabaseTableSchema schema;

    private ResultSetDeserializer<T> deserializer;

    public boolean createTable() {
        // TODO: add schema-defined permission to selectively allow this functionality
        final Optional<String> statement = CreateTableStatement.create(schema);

        if (statement.isEmpty()) {
            // TODO: Add logging here
            return false;
        } else {
            return executeUpdateWithBooleanReturnValue(statement.get(), new SQLUpdateDeserializer());
        }
    }

    public boolean dropTable() {
        // TODO: add schema-defined permission to selectively allow this functionality
        final String statement = DropTableStatement.create(schema.getTableName());
        return executeUpdateWithBooleanReturnValue(statement, new SQLUpdateDeserializer());
    }

    public boolean tableExists() {
        final String query = TableExistsStatement.create(schema.getPostgresqlSchemaName(), schema.getTableName());
        try {
            final DatabaseResponse response = executor.executeQuery(query, new TableExistsDeserializer());
            return response.getCastedObject(Boolean.class).orElse(false);
        } catch (Exception e) {
            // TODO: add error logging here
            e.printStackTrace();
            return false;
        }
    }

    public InsertStatement.Builder insertStatementBuilder() {
        return InsertStatement.newBuilder(schema);
    }

    public SelectStatement.Builder selectStatementBuilder() {
        return SelectStatement.newBuilder(schema);
    }

    public DeleteStatement.Builder deleteStatementBuilder() {
        return DeleteStatement.newBuilder(schema);
    }

    public UpdateStatement.Builder updateStatementBuilder() {
        return UpdateStatement.newBuilder(schema);
    }

    public JoinStatement.Builder joinStatementBuilder() { return JoinStatement.newBuilder(schema); }

    @Override
    public Optional<T> insert(InsertStatement.Builder builder) {
        if(insertBuilderIsMissingRequiredFields(builder)) {
            return Optional.empty();
        }

        try {
            final String statement = builder.build();
            final DatabaseResponse response = executor.executeQuery(statement, deserializer);
            return response.getCastedObjectOrDefault(List.class, List.of()).stream()
                    .filter(deserializer.getGenericClassReference()::isInstance)
                    .map(deserializer.getGenericClassReference()::cast)
                    .findFirst();
        } catch (InsertStatement.Builder.Exception e) {
            // TODO: Add logging here
            e.printStackTrace();
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<T>> read(SelectStatement.Builder builder) {
        try {
            final String query = builder.build();
            return executeQueryWithListReturnValue(query, deserializer);
        } catch (Exception e) {
            // TODO: Add logging here
            return Optional.empty();
        }
    }

    @Override
    public Boolean update(UpdateStatement.Builder builder) {
        try {
            final String statement = builder.build();
            return executeUpdateWithBooleanReturnValue(statement, new SQLUpdateDeserializer());
        } catch (Exception e) {
            // TODO: Add logging here
            return false;
        }
    }

    @Override
    public Boolean delete(DeleteStatement.Builder builder) {
        try {
            final String statement = builder.build();
            return executeUpdateWithBooleanReturnValue(statement, new SQLUpdateDeserializer());
        } catch (Exception e) {
            // TODO: Add logging here
            return false;
        }
    }

    @Override
    public Optional<List<T>> join(JoinStatement.Builder builder) {
        return Optional.empty();
    }

    private Boolean executeUpdateWithBooleanReturnValue(final String statement, final Deserializer deserializer) {
        try {
            final DatabaseResponse response = executor.executeUpdate(statement, deserializer);
            return response.getCastedObject(Boolean.class).orElse(false);
        } catch (Exception e) {
            // TODO: Add logging here
            return false;
        }
    }

    private Optional<List<T>> executeQueryWithListReturnValue(final String query, final ResultSetDeserializer<T> deserializer) {
        try {
            Optional<List> response = executor.executeQuery(query, deserializer).getCastedObject(List.class);

            if (response.isEmpty()) {
                return Optional.empty();
            }

            final Class<T> tClass = deserializer.getGenericClassReference();
            final List<T> castedObjectList = (List<T>) response.get().stream()
                    .filter(tClass::isInstance)
                    .map(tClass::cast)
                    .collect(Collectors.toList());
            return Optional.of(castedObjectList);
        } catch (Exception e) {
            // TODO: add logging here
            return Optional.empty();
        }
    }

    private Boolean insertBuilderIsMissingRequiredFields(InsertStatement.Builder builder) {
        final Set<Column> insertBuilderRequestsColumns = builder.getColumnValuePairs().stream()
                .map(ColumnValuePair::getColumn)
                .collect(Collectors.toSet());
        return schema.getColumns().stream()
                .filter(Column::isRequired)
                .anyMatch(Predicate.not(insertBuilderRequestsColumns::contains));
    }

    public void setDeserializer(ResultSetDeserializer<T> deserializer) {
        this.deserializer = deserializer;
    }

}