package databases.sql.postgresql.statements.builders;

import databases.sql.Column;
import databases.sql.postgresql.statements.WhereClause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Join {
    private final Type type;
    private final Column fromColumn;
    private final Column toColumn;
    private final List<Column> selectedColumns;
    private final WhereClause whereClause;

    private Join(Builder builder) {
        this.type = builder.type;
        this.fromColumn = builder.from;
        this.toColumn = builder.to;
        this.selectedColumns = builder.selectedColumns;
        this.whereClause = builder.whereClause;
    }

    public String getTypeDescription() {
        switch (this.type) {
            case INNER_JOIN:
                return "INNER JOIN";
            default:
                return "";
        }
    }

    public Column getToColumn() {
        return toColumn;
    }

    public Column getFromColumn() {
        return fromColumn;
    }

    public List<Column> getSelectedColumns() {
        return selectedColumns;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Type type;
        private Column from;
        private Column to;
        private List<Column> selectedColumns = new ArrayList<>();
        private WhereClause whereClause;

        public Builder innerJoin() {
            type = Type.INNER_JOIN;
            return this;
        }

        public Builder from(Column column) {
            this.from = column;
            return this;
        }

        public Builder to(Column column) {
            this.to = column;
            return this;
        }

        public Builder select(Column... columns) {
            this.selectedColumns.addAll(Arrays.asList(columns));
            return this;
        }

        public Join build() {
            return new Join(this);
        }

        public Builder where(WhereClause whereClause) {
            this.whereClause = whereClause;
            return this;
        }
    }

    private enum Type {
        INNER_JOIN;
    }
}
