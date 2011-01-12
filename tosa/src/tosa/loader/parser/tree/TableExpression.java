package tosa.loader.parser.tree;

import tosa.loader.parser.Token;

public class TableExpression extends SQLParsedElement {

  private TableFromClause _from;
  private SQLParsedElement _where;

  public TableExpression(Token first, Token last, TableFromClause fromClause, SQLParsedElement whereClause) {
    super(first, last, fromClause, whereClause);
    _from = fromClause;
    _where = whereClause;
  }

  @Override
  protected void toSQL(boolean prettyPrint, int indent, StringBuilder sb) {
    _from.toSQL(prettyPrint, indent, sb);
    if (_where != null) _where.toSQL(prettyPrint, indent, sb);
  }

  public TableFromClause getFrom() {
    return _from;
  }

  public SQLParsedElement getWhere() {
    return _where;
  }
}
