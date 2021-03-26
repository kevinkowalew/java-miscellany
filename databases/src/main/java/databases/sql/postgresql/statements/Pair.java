package databases.sql.postgresql.statements;

public class Pair<KeyType, ValueType> {
    private final KeyType key;
    private final ValueType value;

    Pair(KeyType key, ValueType value) {
        this.key = key;
        this.value = value;
    }

    public KeyType getKey() {
        return this.key;
    }

    public ValueType getValue() {
        return this.value;
    }
}
